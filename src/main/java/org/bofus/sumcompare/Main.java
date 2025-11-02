package org.bofus.sumcompare;

import java.io.File;
import java.security.MessageDigest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bofus.sumcompare.localutil.FileTypeDetector;
import org.bofus.sumcompare.localutil.FileUtilsLocal;
import org.bofus.sumcompare.localutil.ReportUtils;
import org.bofus.sumcompare.localutil.UserUtilities;
import org.bofus.sumcompare.model.FileMetadata;
import org.bofus.sumcompare.model.PropertiesObject;
import org.bofus.sumcompare.singletons.CopiedFileHashMapSingleton;
import org.bofus.sumcompare.singletons.MatchingFileHashMapSingleton;
import org.bofus.sumcompare.singletons.SourceFileArraySingleton;
import org.bofus.sumcompare.singletons.TargetFileArraySingleton;
import org.bofus.sumcompare.singletons.TargetFileHashMapSingleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

  public static void main(String[] args) throws Exception {
    PropertiesObject propertiesObject = new PropertiesObject();

    // -------------------------------------------------------------
    // Set command line options
    // -------------------------------------------------------------
    CommandLineParser parser = new DefaultParser();
    Options cliOptions = new Options();
    cliOptions.addOption(
        "b",
        "backup-source-first",
        false,
        "Backup (zip file) the source directory before taking any other action (default: false)");
    cliOptions.addOption("d", "dry-run", false, "Run the process (default: false)");
    cliOptions.addOption(
        "k",
        "keep-source-structure",
        false,
        "Keep the source directory structure when copying to target (default: false)");
    cliOptions.addOption("o", "create-output-file", false, "Create an output (excel) file");
    cliOptions.addOption(
        "p", "preserve-file-date", false, "Preserve the file date on copy (Default: false)");
    cliOptions.addOption(
        "r", "post-remove", false, "Remove source file after copy to destination (Default: false)");
    cliOptions.addOption("s", "source", true, "Specifies the source location	<REQUIRED>");
    cliOptions.addOption("t", "target", true, "Specifies the target location		<REQUIRED>");
    cliOptions.addOption(
        "y",
        "i-agree",
        false,
        "Agree to the fact that there is not warranty, or guarantee, and hold noone responsible for the results of this application");
    cliOptions.addOption(
        "z",
        "chksumtype",
        true,
        "The type of checksum data to use for comparison (SHA1, MD5, XXHASH32, XXHASH64)	<REQUIRED>");
    cliOptions.addOption("h", "help", false, "Shows this help screen");

    // -------------------------------------------------------------
    // Check command line options
    // -------------------------------------------------------------
    try {
      CommandLine cmdLine;
      cmdLine = parser.parse(cliOptions, args);

      if (cmdLine.hasOption("b")) {
        propertiesObject.setBackupFirst(true);
      } else {
        propertiesObject.setBackupFirst(false);
      }

      if (cmdLine.hasOption("d")) {
        propertiesObject.setDryRun(true);
      } else {
        propertiesObject.setDryRun(false);
      }

      if (cmdLine.hasOption("s")) {
        // sourceLocation = cmdLine.getOptionValue("s");
        propertiesObject.setSourceLocation(cmdLine.getOptionValue("s"));
        log.debug(
            String.format(
                "Setting Source Location from Command Line Argument: %s",
                propertiesObject.getSourceLocation()));
        FileUtilsLocal.checkDirectoryExists(propertiesObject.getSourceLocation());
      } else {
        showHelp(cliOptions);
      }

      if (cmdLine.hasOption("t")) {
        propertiesObject.setTargetLocation(cmdLine.getOptionValue("t"));
        log.debug(
            String.format(
                "Setting Target Location from Command Line Argument: %s",
                cmdLine.getOptionValue("t")));
        FileUtilsLocal.checkDirectoryExists(cmdLine.getOptionValue("t"));
      } else {
        showHelp(cliOptions);
      }

      if (cmdLine.hasOption("o")) {
        propertiesObject.setCreateOutputFile(true);
      }

      if (cmdLine.hasOption("p")) {
        propertiesObject.setPreserveFileDate(true);
      }

      if (cmdLine.hasOption("r")) {
        propertiesObject.setPostCopyRemove(true);
      }

      if (cmdLine.hasOption("z")) {
        propertiesObject.setDigestType(FileUtilsLocal.SetDigestType(cmdLine.getOptionValue("z")));
      } else {
        showHelp(cliOptions);
      }

      if (cmdLine.hasOption("y")) {
        log.info("User has accepted the terms via the command line option '-y'");
      } else {
        boolean userAccepts = UserUtilities.getUserAcceptance();
        if (userAccepts = true) {
          log.info("User has accepted the agreement, beginning processing...");
        }
      }

      if (cmdLine.hasOption("k")) {
        propertiesObject.setKeepSourceStructure(true);
      } else {
        propertiesObject.setKeepSourceStructure(false);
      }

      if (cmdLine.hasOption("h")) {
        showHelp(cliOptions);
      }

    } catch (Exception e) {
      log.debug(e.toString());
      throw e;
    }

    // Step 1: Backup if requested
    if (propertiesObject.isBackupFirst() == true) {
      log.info("Creating backup of source directory...");
      FileUtilsLocal.zipDirectory(propertiesObject);
      log.info("Backup completed");
    } else {
      log.warn(
          "Backup first not specified on the command line, we will not backup the source files first!!!");
    }

    // Step 2 & 4: Scan target and source directories in parallel
    log.info("Scanning directories in parallel...");

    Thread targetScanThread = new Thread(() -> {
      try {
        FileUtilsLocal.getTargetDirectoryContentsArray(propertiesObject.getTargetLocation());
        int targetCount = TargetFileArraySingleton.getInstance().getArray().size();
        log.info("Found " + targetCount + " files in target");

        // Step 3: Compute target checksums
        log.info("Computing target checksums...");
        FileUtilsLocal.createTargetFileChecksumMap(
            TargetFileArraySingleton.getInstance(),
            propertiesObject.getDigestType());
        log.info("Target checksums completed");
      } catch (Exception e) {
        log.error("Error scanning target directory", e);
      }
    });

    Thread sourceScanThread = new Thread(() -> {
      try {
        FileUtilsLocal.getSourceDirectoryContentsArray(propertiesObject.getSourceLocation());
        int sourceCount = SourceFileArraySingleton.getInstance().getArray().size();
        log.info("Found " + sourceCount + " files in source");
      } catch (Exception e) {
        log.error("Error scanning source directory", e);
      }
    });

    // Start both threads
    targetScanThread.start();
    sourceScanThread.start();

    // Wait for both to complete
    targetScanThread.join();
    sourceScanThread.join();

    log.info("Directory scanning completed");

    log.info("Processing source files...");
    log.debug(
        "Iterating through the source array, and checking if there is already a matching checksum in the target array");

    // Use parallel stream for concurrent file processing (Java 21 optimized)
    SourceFileArraySingleton.getInstance().getArray().parallelStream().forEach(thisSourceFileName -> {
      try {
        File thisSourceFile = new File(thisSourceFileName);

        // Capture file metadata
        FileMetadata metadata = FileMetadata.fromFile(thisSourceFile);

        // Detect file type
        String fileTypeDesc = FileTypeDetector.getFileTypeDescription(thisSourceFile);

        // Clone digest for thread-safety
        MessageDigest threadDigest = (MessageDigest) propertiesObject.getDigestType().clone();
        String thisSourceChecksum = FileUtilsLocal.getFileChecksum(threadDigest, thisSourceFile);

        // Synchronized access to shared collections
        synchronized (TargetFileHashMapSingleton.getInstance().getMap()) {
          if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(thisSourceChecksum)) {
            String existingfile = TargetFileHashMapSingleton.getInstance().getMap().get(thisSourceChecksum);
            String thisSourceFileNameOnly = FileUtilsLocal.getFileName(thisSourceFileName);
            String thisTargetFileNameOnly = FileUtilsLocal.getFileName(existingfile);

            if (thisSourceFileNameOnly.trim().equals(thisTargetFileNameOnly.trim())) {
              // if this is a dryrun, add to the map, so that we can create an output file of
              // all files
              if (propertiesObject.isDryRun() == true) {
                MatchingFileHashMapSingleton.getInstance().addToMap(thisSourceFileName, existingfile);
              }
            } else {
              log.info(
                  String.format(
                      "%s [%s] seems to be a copy of file:\r\n%s\r\nMetadata: %s",
                      thisSourceFileName, fileTypeDesc, existingfile, metadata.getSummary()));
              MatchingFileHashMapSingleton.getInstance().addToMap(thisSourceFileName, existingfile);
            }

          } else {
            String targetFileName = FileUtilsLocal.getFileName(thisSourceFileName);
            String targetFullPath = null;
            String sourceBasePath = null;

            if (propertiesObject.isKeepSourceStructure() == true) {
              sourceBasePath = thisSourceFileName.replace(propertiesObject.getSourceLocation(), "");
              String tempPath = FilenameUtils.getPath(sourceBasePath);
              targetFullPath = propertiesObject.getTargetLocation()
                  + File.separatorChar
                  + tempPath
                  + File.separatorChar
                  + targetFileName;
            } else {
              targetFullPath = propertiesObject.getTargetLocation() + File.separatorChar + targetFileName;
            }

            File targetFile = new File(targetFullPath);

            CopiedFileHashMapSingleton.getInstance().getMap().put(thisSourceFileName, targetFullPath);
            if (propertiesObject.isDryRun() == true) {
              log.info(
                  String.format("Would Copy File [%s]: %s to %s (%s)",
                      fileTypeDesc, thisSourceFileName, targetFullPath, metadata.getSummary()));
            } else {
              log.info(
                  String.format("Copying [%s]: %s (%s)", fileTypeDesc, thisSourceFile.getName(),
                      metadata.getSummary()));
              FileUtils.copyFile(thisSourceFile, targetFile, propertiesObject.isPreserveFileDate());
            }
          }
        }
      } catch (Exception e) {
        log.error("Error processing source file: " + thisSourceFileName, e);
      }
    });

    // Generate report if requested
    if (propertiesObject.isCreateOutputFile() == true) {
      log.info("Generating Excel report...");
      ReportUtils.createOutputExcel();
      log.info("Report created: Copy_Output.xlsx");
    }

    // Update final statistics
    int copied = CopiedFileHashMapSingleton.getInstance().getMap().size();
    int duplicates = MatchingFileHashMapSingleton.getInstance().getMap().size();

    log.info("================================================");
    log.info("           COMPLETED SUCCESSFULLY               ");
    log.info("================================================");
    log.info(String.format("Files copied: %d", copied));
    log.info(String.format("Duplicates found: %d", duplicates));
    log.info("================================================");

    // // get the source file directory list
    // FileUtils.getSourceDirectoryContentsArray(sourceLocation);
    // log.debug(String.format("Source File Singleton Size: %s",
    // SourceFileArraySingleton.getInstance().getArray().size()));
    //
    // // get the checksums for the source files and put them in a map
    // FileUtils.createSourceFileChecksumMap(SourceFileArraySingleton.getInstance(),
    // digestType);

  }

  /*************************************************
   * @param cliOptions
   *************************************************/
  private static void showHelp(Options cliOptions) {
    // automatically generate the help statement
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Main.jar", cliOptions);
    System.exit(0);
  }
}
