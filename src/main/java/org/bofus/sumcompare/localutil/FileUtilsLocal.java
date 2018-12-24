package org.bofus.sumcompare.localutil;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;
import org.bofus.sumcompare.Main;
import org.bofus.sumcompare.singletons.SourceFileArraySingleton;
import org.bofus.sumcompare.singletons.SourceFileHashMapSingleton;
import org.bofus.sumcompare.singletons.TargetFileArraySingleton;
import org.bofus.sumcompare.singletons.TargetFileHashMapSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilsLocal
{
	private static final Logger logger = LoggerFactory.getLogger(FileUtilsLocal.class);

	public static String getFileChecksum(MessageDigest digest, File file) throws IOException
	{
		//Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		//Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		//Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1)
		{
			digest.update(byteArray, 0, bytesCount);
		}

		//close the stream; We don't need it now.
		fis.close();

		//Get the hash's bytes
		byte[] bytes = digest.digest();

		//This bytes[] has bytes in decimal format;
		//Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++)
		{
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		//return complete hash
		return sb.toString();
	}

	public static MessageDigest SetDigestType(String typeFromArgs) throws NoSuchAlgorithmException
	{
		MessageDigest returnData = null;

		if (typeFromArgs.equalsIgnoreCase("MD5"))
		{
			returnData = MessageDigest.getInstance("MD5");
		}
		else if (typeFromArgs.equalsIgnoreCase("SHA1"))
		{
			returnData = MessageDigest.getInstance("SHA1");
		}
		else
		{
			logger.error(String.format("Unknown digest type %s cannot continue.  Exiting...", typeFromArgs));
			System.exit(98);
		}

		return returnData;
	}

	public static void getSourceDirectoryContentsArray(String inputLocation) throws SQLException, PropertyVetoException
	{
		try
		{
			File dir = new File(inputLocation);

			File[] files = dir.listFiles();
			for (File file : files)
			{
				if (file.isDirectory())
				{
					logger.debug(String.format("Directory: %s", file.getCanonicalPath()));
					getSourceDirectoryContentsArray(file.toString());
				}
				else
				{
					logger.debug(String.format("File: %s", file.getCanonicalPath()));
					SourceFileArraySingleton.getInstance().addToArray(file.getCanonicalPath());
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void createSourceFileChecksumMap(SourceFileArraySingleton sourceFileArray, MessageDigest digestType) throws IOException, SQLException, PropertyVetoException
	{
		for (int sourceFileNumber = 0; sourceFileNumber < sourceFileArray.getInstance().getArray().size(); sourceFileNumber++)
		{
			String fileString = SourceFileArraySingleton.getInstance().getArray().get(sourceFileNumber);
			File thisFile = new File(fileString);
			String thisFileChecksum = FileUtilsLocal.getFileChecksum(digestType, thisFile);
//			logger.debug(String.format("File Checksum for file %s is %s", fileString, thisFileChecksum));
			
			if (SourceFileHashMapSingleton.getInstance().getMap().containsKey(thisFileChecksum))
			{
				logger.debug(String.format("Hashmap already contains an entry for checksum: %s with filename of %s", thisFileChecksum, SourceFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum)));
				String existingfile = SourceFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum);
				logger.info(String.format("%s \r\n seems to be a copy of file:\r\n%s",thisFile, existingfile));
			}
			else
			{
				SourceFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
			}
		}
		logger.debug(String.format("Hashmap size for source files: %s", SourceFileHashMapSingleton.getInstance().getMap().size()));
	}
	

	public static void getTargetDirectoryContentsArray(String inputLocation) throws SQLException, PropertyVetoException
	{
		try
		{
			File dir = new File(inputLocation);

			File[] files = dir.listFiles();
			for (File file : files)
			{
				if (file.isDirectory())
				{
					logger.debug(String.format("Directory: %s", file.getCanonicalPath()));
					getTargetDirectoryContentsArray(file.toString());
				}
				else
				{
					logger.debug(String.format("File: %s", file.getCanonicalPath()));
					TargetFileArraySingleton.getInstance().addToArray(file.getCanonicalPath());
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	
	public static void createTargetFileChecksumMap(TargetFileArraySingleton targetFileArray, MessageDigest digestType) throws IOException, SQLException, PropertyVetoException
	{
		for (int targetFileNumber = 0; targetFileNumber < targetFileArray.getInstance().getArray().size(); targetFileNumber++)
		{
			String fileString = TargetFileArraySingleton.getInstance().getArray().get(targetFileNumber);
			File thisFile = new File(fileString);
			String thisFileChecksum = FileUtilsLocal.getFileChecksum(digestType, thisFile);
//			logger.debug(String.format("File Checksum for file %s is %s", fileString, thisFileChecksum));
			
			if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(thisFileChecksum))
			{
				logger.debug(String.format("Hashmap already contains an entry for checksum: %s with filename of %s", thisFileChecksum, TargetFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum)));
				String existingfile = TargetFileHashMapSingleton.getInstance().getMap().get(thisFileChecksum);
				logger.info(String.format("%s \r\n seems to be a copy of file:\r\n%s",thisFile, existingfile));
			}
			else
			{
				TargetFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
			}
		}
		logger.debug(String.format("Hashmap size for target files: %s", TargetFileHashMapSingleton.getInstance().getMap().size()));
	}

	public static String getFilePath(String file)
	{
		String filePathString = null;
		File filePath = null;

//		logger.debug(String.format("getting path for file: %s", file));
		try
		{
			filePathString = FilenameUtils.getFullPathNoEndSeparator(file);
//			logger.debug(String.format("Determined path: %s", filePathString));
			filePath = new File(filePathString);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
		}

		return filePath.toString();
	}

	public static String getFileName(String file)
	{
		String fileNameString = null;
		File fileName = null;

//		logger.debug(String.format("getting path for file: %s", file));
		try
		{
			fileNameString = FilenameUtils.getName(file);
			fileName = new File(fileNameString);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
		}

		return fileName.toString();
	}

	
	public static void checkDirectoryExists(String directory)
	{
		File thisDirectory = new File(directory);

		if (thisDirectory.exists())
		{
			logger.debug(String.format("Directory Exists: %s", thisDirectory));
		}
		else
		{
			logger.error(String.format("Directory provided (%s) does not exist, exiting now...", directory));
			System.exit(94);
		}		
	}

}
