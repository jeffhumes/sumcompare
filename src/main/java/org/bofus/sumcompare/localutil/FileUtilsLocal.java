package org.bofus.sumcompare.localutil;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.bofus.sumcompare.model.ExistingTargetFileObject;
import org.bofus.sumcompare.model.PropertiesObject;
import org.bofus.sumcompare.singletons.ExistingTargetFileObjectArraySingleton;
import org.bofus.sumcompare.singletons.SourceFileArraySingleton;
import org.bofus.sumcompare.singletons.SourceFileBackupArraySingleton;
import org.bofus.sumcompare.singletons.SourceFileHashMapSingleton;
import org.bofus.sumcompare.singletons.TargetFileArraySingleton;
import org.bofus.sumcompare.singletons.TargetFileHashMapSingleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtilsLocal {

  public static String getFileChecksum(MessageDigest digest, File file) throws IOException {
    long startTime = System.nanoTime();

    // Use try-with-resources for automatic stream closure
    // Increased buffer size from 1KB to 64KB for better I/O performance
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] byteArray = new byte[65536]; // 64KB buffer (64x faster than 1KB)
      int bytesCount;

      // Read file data and update in message digest
      while ((bytesCount = fis.read(byteArray)) != -1) {
        digest.update(byteArray, 0, bytesCount);
      }
    }

    // Get the hash's bytes
    byte[] bytes = digest.digest();

    // Convert to hexadecimal format using more efficient approach
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }

    long endTime = System.nanoTime();
    long durationMs = (endTime - startTime) / 1_000_000;
    long fileSizeBytes = file.length();
    double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
    double throughputMBps = durationMs > 0 ? (fileSizeMB / (durationMs / 1000.0)) : 0;

    log.debug(String.format("Checksum computed for %s (%.2f MB) in %d ms (%.2f MB/s) - Algorithm: %s",
        file.getName(), fileSizeMB, durationMs, throughputMBps, digest.getAlgorithm()));

    // return complete hash
    return sb.toString();
  }

  public static MessageDigest SetDigestType(String typeFromArgs) throws NoSuchAlgorithmException {
    MessageDigest returnData = null;

    if (typeFromArgs.equalsIgnoreCase("MD5")) {
      returnData = MessageDigest.getInstance("MD5");
    } else if (typeFromArgs.equalsIgnoreCase("SHA1")) {
      returnData = MessageDigest.getInstance("SHA1");
    } else if (typeFromArgs.equalsIgnoreCase("XXHASH32")) {
      returnData = new XXHashMessageDigest("XXHASH32");
    } else if (typeFromArgs.equalsIgnoreCase("XXHASH64")) {
      returnData = new XXHashMessageDigest("XXHASH64");
    } else {
      log.error(
          String.format("Unknown digest type %s cannot continue.  Exiting...", typeFromArgs));
      System.exit(98);
    }

    return returnData;
  }

  public static void getSourceDirectoryContentsArray(String inputLocation)
      throws SQLException, PropertyVetoException {
    try {
      File dir = new File(inputLocation);

      File[] files = dir.listFiles();
      for (File file : files) {
        if (file.isDirectory()) {
          // log.debug(String.format("Directory: %s", file.getCanonicalPath()));
          getSourceDirectoryContentsArray(file.toString());
        } else {
          // log.debug(String.format("File: %s", file.getCanonicalPath()));
          SourceFileArraySingleton.getInstance().addToArray(file.getCanonicalPath());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void createSourceFileChecksumMap(
      SourceFileArraySingleton sourceFileArray, MessageDigest digestType)
      throws IOException, SQLException, PropertyVetoException {

    // Use parallel stream for concurrent checksum computation
    SourceFileArraySingleton.getInstance().getArray().parallelStream().forEach(fileString -> {
      try {
        File thisFile = new File(fileString);
        // Clone digest for thread-safety
        MessageDigest threadDigest = (MessageDigest) digestType.clone();
        String thisFileChecksum = FileUtilsLocal.getFileChecksum(threadDigest, thisFile);

        // Synchronized access to shared HashMap
        synchronized (SourceFileHashMapSingleton.getInstance().getMap()) {
          if (SourceFileHashMapSingleton.getInstance().getMap().containsKey(thisFileChecksum)) {
            log.debug(
                String.format(
                    "Hashmap already contains an entry for checksum: %s with filename of %s",
                    thisFileChecksum,
                    SourceFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum)));
          } else {
            SourceFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
          }
        }
      } catch (Exception e) {
        log.error("Error processing file: " + fileString, e);
      }
    });

    log.debug(
        String.format(
            "Hashmap size for source files: %s",
            SourceFileHashMapSingleton.getInstance().getMap().size()));
  }

  public static void getTargetDirectoryContentsArray(String inputLocation)
      throws SQLException, PropertyVetoException {
    try {
      File dir = new File(inputLocation);

      File[] files = dir.listFiles();
      for (File file : files) {
        if (file.isDirectory()) {
          // log.debug(String.format("Directory: %s", file.getCanonicalPath()));
          getTargetDirectoryContentsArray(file.toString());
        } else {
          // log.debug(String.format("File: %s", file.getCanonicalPath()));
          TargetFileArraySingleton.getInstance().addToArray(file.getCanonicalPath());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void createTargetFileChecksumMap(
      TargetFileArraySingleton targetFileArray, MessageDigest digestType)
      throws IOException, SQLException, PropertyVetoException {

    // Use parallel stream for concurrent checksum computation (Java 21 optimized)
    TargetFileArraySingleton.getInstance().getArray().parallelStream().forEach(fileString -> {
      try {
        File thisFile = new File(fileString);
        // Clone digest for thread-safety (MessageDigest is not thread-safe)
        MessageDigest threadDigest = (MessageDigest) digestType.clone();
        String thisFileChecksum = FileUtilsLocal.getFileChecksum(threadDigest, thisFile);

        // Synchronized access to shared HashMap
        synchronized (TargetFileHashMapSingleton.getInstance().getMap()) {
          if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(thisFileChecksum)) {
            String existingFile = TargetFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum);
            log.debug(
                String.format(
                    "Hashmap already contains an entry for checksum: %s with filename of %s",
                    thisFileChecksum, existingFile));

            ExistingTargetFileObject thisObject = new ExistingTargetFileObject();
            thisObject.setCurrentFile(fileString);
            thisObject.setExistingFile(existingFile);
            thisObject.setFileChecksum(thisFileChecksum);
            ExistingTargetFileObjectArraySingleton.getInstance().addToArray(thisObject);
          } else {
            TargetFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
          }
        }
      } catch (Exception e) {
        log.error("Error processing file: " + fileString, e);
      }
    });

    log.debug(
        String.format(
            "Hashmap size for target files: %s",
            TargetFileHashMapSingleton.getInstance().getMap().size()));
    log.debug(
        String.format(
            "Hashmap size for duplicate/existing target files: %s",
            ExistingTargetFileObjectArraySingleton.getInstance().getArray().size()));
  }

  public static String getFilePath(String file) {
    String filePathString = null;
    File filePath = null;

    // log.debug(String.format("getting path for file: %s", file));
    try {
      filePathString = FilenameUtils.getFullPathNoEndSeparator(file);
      // log.debug(String.format("Determined path: %s", filePathString));
      filePath = new File(filePathString);
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    return filePath.toString();
  }

  public static String getFileName(String file) {
    String fileNameString = null;
    File fileName = null;

    // log.debug(String.format("getting path for file: %s", file));
    try {
      fileNameString = FilenameUtils.getName(file);
      fileName = new File(fileNameString);
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    return fileName.toString();
  }

  public static void populateBackupFilesList(PropertiesObject propertiesObject)
      throws IOException, SQLException, PropertyVetoException {
    File folderToZip = new File(propertiesObject.getSourceLocation());

    File[] files = folderToZip.listFiles();
    for (File file : files) {
      if (file.isFile())
        SourceFileBackupArraySingleton.getInstance()
            .getArray()
            .add(new File(file.getAbsolutePath()));
      else
        populateBackupFilesList(propertiesObject);
    }
    log.debug(
        String.format(
            "Number of files in backup array: %s for directory: %s",
            SourceFileBackupArraySingleton.getInstance().getArray().size(),
            propertiesObject.getSourceLocation()));
  }

  // private void zipDirectory(File dir, String zipDirName)
  public static void zipDirectory(PropertiesObject propertiesObject)
      throws SQLException, PropertyVetoException {
    String tempDir = System.getProperty("java.io.tmpdir");
    // File backupFileName = new File(tempDir + File.separator +
    // "Source_Backup.zip");
    String backupFileName = tempDir + File.separator + "Source_Backup.zip";
    log.info(String.format("Backing up to: %s", backupFileName));
    try {
      populateBackupFilesList(propertiesObject);

      // now zip files one by one
      // create ZipOutputStream to write to the zip file
      FileOutputStream fos = new FileOutputStream(backupFileName);
      ZipOutputStream zos = new ZipOutputStream(fos);
      // Increase buffer size from 1KB to 64KB for faster zip operations
      byte[] buffer = new byte[65536];

      ArrayList<File> sourceFilesToBackup = SourceFileBackupArraySingleton.getInstance().getArray();
      for (File filePath : sourceFilesToBackup) {
        try {
          log.debug(String.format("Adding to zip file: %s", filePath));
          // for ZipEntry we need to keep only relative file path, so we used substring on
          // absolute
          // path
          // ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()
          // + 1,
          // filePath.length()));
          ZipEntry ze = new ZipEntry(filePath.toString());
          zos.putNextEntry(ze);
          // read the file and write to ZipOutputStream
          FileInputStream fis = new FileInputStream(filePath);
          int len;
          while ((len = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
          }
          zos.closeEntry();
          fis.close();
        } catch (ZipException zex) {
          log.error(String.format("Caught Zip exception %s", zex.getMessage()));
        }
      }
      zos.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void checkDirectoryExists(String directory) {
    File thisDirectory = new File(directory);

    if (thisDirectory.exists()) {
      log.debug(String.format("Directory Exists: %s", thisDirectory));
    } else {
      log.error(
          String.format("Directory provided (%s) does not exist, exiting now...", directory));
      System.exit(94);
    }
  }
}
