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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilsLocal {
  private static final Logger logger = LoggerFactory.getLogger(FileUtilsLocal.class);

  public static String getFileChecksum(MessageDigest digest, File file) throws IOException {
    // Get file input stream for reading the file content
    FileInputStream fis = new FileInputStream(file);

    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;

    // Read file data and update in message digest
    while ((bytesCount = fis.read(byteArray)) != -1) {
      digest.update(byteArray, 0, bytesCount);
    }

    // close the stream; We don't need it now.
    fis.close();

    // Get the hash's bytes
    byte[] bytes = digest.digest();

    // This bytes[] has bytes in decimal format;
    // Convert it to hexadecimal format
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }

    // return complete hash
    return sb.toString();
  }

  public static MessageDigest SetDigestType(String typeFromArgs) throws NoSuchAlgorithmException {
    MessageDigest returnData = null;

    if (typeFromArgs.equalsIgnoreCase("MD5")) {
      returnData = MessageDigest.getInstance("MD5");
    } else if (typeFromArgs.equalsIgnoreCase("SHA1")) {
      returnData = MessageDigest.getInstance("SHA1");
    } else {
      logger.error(
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
          //					logger.debug(String.format("Directory: %s", file.getCanonicalPath()));
          getSourceDirectoryContentsArray(file.toString());
        } else {
          //					logger.debug(String.format("File: %s", file.getCanonicalPath()));
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
    for (int sourceFileNumber = 0;
        sourceFileNumber < sourceFileArray.getInstance().getArray().size();
        sourceFileNumber++) {
      String fileString = SourceFileArraySingleton.getInstance().getArray().get(sourceFileNumber);
      File thisFile = new File(fileString);
      String thisFileChecksum = FileUtilsLocal.getFileChecksum(digestType, thisFile);
      //			logger.debug(String.format("File Checksum for file %s is %s", fileString,
      // thisFileChecksum));

      if (SourceFileHashMapSingleton.getInstance().getMap().containsKey(thisFileChecksum)) {
        logger.debug(
            String.format(
                "Hashmap already contains an entry for checksum: %s with filename of %s",
                thisFileChecksum,
                SourceFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum)));
        String existingfile =
            SourceFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum);
        //				logger.info(String.format("%s \r\n seems to be a copy of file:\r\n%s", thisFile,
        // existingfile));
      } else {
        SourceFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
      }
    }
    logger.debug(
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
          //					logger.debug(String.format("Directory: %s", file.getCanonicalPath()));
          getTargetDirectoryContentsArray(file.toString());
        } else {
          //					logger.debug(String.format("File: %s", file.getCanonicalPath()));
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
    for (int targetFileNumber = 0;
        targetFileNumber < targetFileArray.getInstance().getArray().size();
        targetFileNumber++) {
      String fileString = TargetFileArraySingleton.getInstance().getArray().get(targetFileNumber);
      File thisFile = new File(fileString);
      String thisFileChecksum = FileUtilsLocal.getFileChecksum(digestType, thisFile);
      //			logger.debug(String.format("File Checksum for file %s is %s", fileString,
      // thisFileChecksum));

      if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(thisFileChecksum)) {
        String existingFile =
            TargetFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum);
        logger.debug(
            String.format(
                "Hashmap already contains an entry for checksum: %s with filename of %s",
                thisFileChecksum, existingFile));
        //				logger.info(String.format("%s \r\n seems to be a copy of file:\r\n%s", thisFile,
        // existingfile));
        ExistingTargetFileObject thisObject = new ExistingTargetFileObject();
        thisObject.setCurrentFile(fileString);
        thisObject.setExistingFile(existingFile);
        thisObject.setFileChecksum(thisFileChecksum);
        ExistingTargetFileObjectArraySingleton.getInstance().addToArray(thisObject);
      } else {
        TargetFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
      }
    }
    logger.debug(
        String.format(
            "Hashmap size for target files: %s",
            TargetFileHashMapSingleton.getInstance().getMap().size()));
    logger.debug(
        String.format(
            "Hashmap size for duplicate/existing target files: %s",
            ExistingTargetFileObjectArraySingleton.getInstance().getArray().size()));
  }

  public static String getFilePath(String file) {
    String filePathString = null;
    File filePath = null;

    //		logger.debug(String.format("getting path for file: %s", file));
    try {
      filePathString = FilenameUtils.getFullPathNoEndSeparator(file);
      //			logger.debug(String.format("Determined path: %s", filePathString));
      filePath = new File(filePathString);
    } catch (Exception e) {
      logger.error(e.getMessage());
    }

    return filePath.toString();
  }

  public static String getFileName(String file) {
    String fileNameString = null;
    File fileName = null;

    //		logger.debug(String.format("getting path for file: %s", file));
    try {
      fileNameString = FilenameUtils.getName(file);
      fileName = new File(fileNameString);
    } catch (Exception e) {
      logger.error(e.getMessage());
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
      else populateBackupFilesList(propertiesObject);
    }
    logger.debug(
        String.format(
            "Number of files in backup array: %s for directory: %s",
            SourceFileBackupArraySingleton.getInstance().getArray().size(),
            propertiesObject.getSourceLocation()));
  }

  //	private void zipDirectory(File dir, String zipDirName)
  public static void zipDirectory(PropertiesObject propertiesObject)
      throws SQLException, PropertyVetoException {
    String tempDir = System.getProperty("java.io.tmpdir");
    //		File backupFileName = new File(tempDir + File.separator +  "Source_Backup.zip");
    String backupFileName = tempDir + File.separator + "Source_Backup.zip";
    logger.info(String.format("Backing up to: %s", backupFileName));
    try {
      populateBackupFilesList(propertiesObject);

      // now zip files one by one
      // create ZipOutputStream to write to the zip file
      FileOutputStream fos = new FileOutputStream(backupFileName);
      ZipOutputStream zos = new ZipOutputStream(fos);
      ArrayList<File> sourceFilesToBackup = SourceFileBackupArraySingleton.getInstance().getArray();

      for (File filePath : sourceFilesToBackup) {
        try {
          logger.debug(String.format("Adding to zip file: %s", filePath));
          // for ZipEntry we need to keep only relative file path, so we used substring on absolute
          // path
          //					ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1,
          // filePath.length()));
          ZipEntry ze = new ZipEntry(filePath.toString());
          zos.putNextEntry(ze);
          // read the file and write to ZipOutputStream
          FileInputStream fis = new FileInputStream(filePath);
          byte[] buffer = new byte[1024];
          int len;
          while ((len = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
          }
          zos.closeEntry();
          fis.close();
        } catch (ZipException zex) {
          logger.error(String.format("Caught Zip exception %s", zex.getMessage()));
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
      logger.debug(String.format("Directory Exists: %s", thisDirectory));
    } else {
      logger.error(
          String.format("Directory provided (%s) does not exist, exiting now...", directory));
      System.exit(94);
    }
  }
}
