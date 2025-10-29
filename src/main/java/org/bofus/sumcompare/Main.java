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
import org.bofus.sumcompare.model.PropertiesObject;
import org.bofus.sumcompare.singletons.CopiedFileHashMapSingleton;
import org.bofus.sumcompare.singletons.MatchingFileHashMapSingleton;
import org.bofus.sumcompare.singletons.SourceFileArraySingleton;
import org.bofus.sumcompare.singletons.TargetFileArraySingleton;
import org.bofus.sumcompare.singletons.TargetFileHashMapSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

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
        logger.debug(
            String.format(
                "Setting Source Location from Command Line Argument: %s",
                propertiesObject.getSourceLocation()));
        FileUtilsLocal.checkDirectoryExists(propertiesObject.getSourceLocation());
      } else {
        showHelp(cliOptions);
      }

      if (cmdLine.hasOption("t")) {
        propertiesObject.setTargetLocation(cmdLine.getOptionValue("t"));
        logger.debug(
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
        // postCopyRemove = true;
      }

      if (cmdLine.hasOption("z")) {
        // String typeFromArgs = cmdLine.getOptionValue("z");
        propertiesObject.setDigestType(FileUtilsLocal.SetDigestType(cmdLine.getOptionValue("z")));
        // digestType = FileUtilsLocal.SetDigestType(typeFromArgs);
      } else {
        showHelp(cliOptions);
      }

      if (cmdLine.hasOption("y")) {
        logger.info("User has accepted the terms via the command line option '-y'");
      } else {
        boolean userAccepts = UserUtilities.getUserAcceptance();
        if (userAccepts = true) {
          logger.info("User has accepted the agreement, beginning processing...");
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
      logger.debug(e.toString());
      throw e;
    }

    if (propertiesObject.isBackupFirst() == true) {
      // String backupFile = propertiesObject.getSourceLocation();
      logger.debug("Backup first selected");
      FileUtilsLocal.populateBackupFilesList(propertiesObject);
      FileUtilsLocal.zipDirectory(propertiesObject);
    } else {
      logger.warn(
          "Backup first not specified on the command line, we will not backup the source files first!!!");
    }

    // get the target file directory list
    logger.info("Getting the file list for the target location...");
    FileUtilsLocal.getTargetDirectoryContentsArray(propertiesObject.getTargetLocation());
    logger.debug(
        String.format(
            "Target File Singleton Size: %s",
            TargetFileArraySingleton.getInstance().getArray().size()));

    // get the checksums for the target files and put them in a map
    logger.info(
        "Getting the checksums for the target location files, depending on the number and size of files this may take a while...");
    FileUtilsLocal.createTargetFileChecksumMap(
        TargetFileArraySingleton.getInstance(), propertiesObject.getDigestType());

    // get the source file directory list
    logger.info("Getting the file list  for the source location...");
    FileUtilsLocal.getSourceDirectoryContentsArray(propertiesObject.getSourceLocation());
    logger.debug(
        String.format(
            "Source File Singleton Size: %s",
            SourceFileArraySingleton.getInstance().getArray().size()));

    logger.debug(
        "Iterating through the source array, and checking if there is already a matching checksum in the target array");

    // Use parallel stream for concurrent file processing (Java 21 optimized)
    SourceFileArraySingleton.getInstance().getArray().parallelStream().forEach(thisSourceFileName -> {
      try {
        File thisSourceFile = new File(thisSourceFileName);

        // Detect file type
        FileTypeDetector.FileType fileType = FileTypeDetector.detectFileType(thisSourceFile);
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
              logger.info(
                  String.format(
                      "%s [%s] seems to be a copy of file:\r\n%s",
                      thisSourceFileName, fileTypeDesc, existingfile));
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
              logger.info(
                  String.format("Would Copy File [%s]: %s to %s", fileTypeDesc, thisSourceFileName, targetFullPath));
            } else {
              logger.info(
                  String.format("Copying [%s]: %s", fileTypeDesc, thisSourceFile.getName()));
              FileUtils.copyFile(thisSourceFile, targetFile, propertiesObject.isPreserveFileDate());
            }
          }
        }
      } catch (Exception e) {
        logger.error("Error processing source file: " + thisSourceFileName, e);
      }
    });

    if (CopiedFileHashMapSingleton.getInstance().getMap().size() != 0
        || MatchingFileHashMapSingleton.getInstance().getMap().size() != 0) {
      logger.debug(
          String.format(
              "%s files copied ", CopiedFileHashMapSingleton.getInstance().getMap().size()));
      logger.debug(
          String.format(
              "%s files not copied as there were duplicates in the target location",
              MatchingFileHashMapSingleton.getInstance().getMap().size()));
      if (propertiesObject.isCreateOutputFile() == true) {
        logger.debug("Creating output file");
        ReportUtils.createOutputExcel();
      }
    } else {
      if (propertiesObject.isCreateOutputFile() == true) {
        logger.debug(
            "There were no files copied, and no duplicate files in the target with different names, no use creating a report");
      }
    }

    logger.info("================================================");
    logger.info("		COMPLETE		");
    logger.info("================================================");

    // // get the source file directory list
    // FileUtils.getSourceDirectoryContentsArray(sourceLocation);
    // logger.debug(String.format("Source File Singleton Size: %s",
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
