package org.bofus.sumcompare;

import java.io.File;
import java.security.MessageDigest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.*;
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

public class Main
{
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception
	{
		PropertiesObject propertiesObject = new PropertiesObject();
//		String sourceLocation = null;
//		String targetLocation = null;
//		MessageDigest digestType = null;
//		boolean postCopyRemove = false;
//		boolean preserveFileDate = false;
//		boolean createOutputFile = false;
//		boolean dryRun = false;

		// -------------------------------------------------------------
		// Set command line options
		// -------------------------------------------------------------
		CommandLineParser parser = new DefaultParser();
		Options cliOptions = new Options();
		cliOptions.addOption("d", "dry-run", false, "Run the process (default: false)");
		cliOptions.addOption("o", "create-output-file", false, "Create an output (excel) file");
		cliOptions.addOption("p", "preserve-file-date", false, "Preserve the file date on copy (Default: false)");
		cliOptions.addOption("r", "post-remove", false, "Remove source file after copy to destination (Default: false)");
		cliOptions.addOption("s", "source", true, "Specifies the source location	<REQUIRED>");
		cliOptions.addOption("t", "target", true, "Specifies the target location		<REQUIRED>");
		cliOptions.addOption("z", "chksumtype", true, "The type of checksum data to use for comparison (SHA1, MD5)	<REQUIRED>");
		cliOptions.addOption("h", "help", false, "Shows this help screen");

		// -------------------------------------------------------------
		// Check command line options
		// -------------------------------------------------------------
		try
		{
			CommandLine cmdLine;
			cmdLine = parser.parse(cliOptions, args);

			if (cmdLine.hasOption("d"))
			{
				propertiesObject.setDryRun(true);
//				dryRun = true;
			}

			if (cmdLine.hasOption("s"))
			{
//				sourceLocation = cmdLine.getOptionValue("s");
				propertiesObject.setSourceLocation(cmdLine.getOptionValue("s"));
				logger.debug(String.format("Setting Source Location from Command Line Argument: %s", propertiesObject.getSourceLocation()));
				FileUtilsLocal.checkDirectoryExists(propertiesObject.getSourceLocation());
			}
			else
			{
				showHelp(cliOptions);
			}

			if (cmdLine.hasOption("t"))
			{
//				targetLocation = cmdLine.getOptionValue("t");
				propertiesObject.setTargetLocation(cmdLine.getOptionValue("t"));
				logger.debug(String.format("Setting Target Location from Command Line Argument: %s", cmdLine.getOptionValue("t")));
				FileUtilsLocal.checkDirectoryExists(cmdLine.getOptionValue("t"));
			}
			else
			{
				showHelp(cliOptions);
			}

			if (cmdLine.hasOption("o"))
			{
				propertiesObject.setCreateOutputFile(true);
//				createOutputFile = true;
			}

			if (cmdLine.hasOption("p"))
			{
				propertiesObject.setPreserveFileDate(true);
//				preserveFileDate = true;
			}

			if (cmdLine.hasOption("r"))
			{
				propertiesObject.setPostCopyRemove(true);
//				postCopyRemove = true;
			}

			if (cmdLine.hasOption("z"))
			{
//				String typeFromArgs = cmdLine.getOptionValue("z");
				propertiesObject.setDigestType(FileUtilsLocal.SetDigestType(cmdLine.getOptionValue("z")));
//				digestType = FileUtilsLocal.SetDigestType(typeFromArgs);
			}
			else
			{
				showHelp(cliOptions);
			}

			if (cmdLine.hasOption("h"))
			{
				showHelp(cliOptions);
			}

		}
		catch (Exception e)
		{
			logger.debug(e.toString());
			throw e;
		}

		boolean userAccepts = UserUtilities.getUserAcceptance();
		if (userAccepts = true)
		{
			logger.info("User has accepted the agreement, beginning processing...");
		}

		// get the target file directory list
		FileUtilsLocal.getTargetDirectoryContentsArray(propertiesObject.getTargetLocation());
		logger.debug(String.format("Target File Singleton Size: %s", TargetFileArraySingleton.getInstance().getArray().size()));

		// get the checksums for the target files and put them in a map
		FileUtilsLocal.createTargetFileChecksumMap(TargetFileArraySingleton.getInstance(), propertiesObject.getDigestType());

		// get the source file directory list
		FileUtilsLocal.getSourceDirectoryContentsArray(propertiesObject.getSourceLocation());
		logger.debug(String.format("Source File Singleton Size: %s", SourceFileArraySingleton.getInstance().getArray().size()));

		for (int filecount = 0; filecount < SourceFileArraySingleton.getInstance().getArray().size(); filecount++)
		{
			String thisSourceFileName = SourceFileArraySingleton.getInstance().getArray().get(filecount);
			File thisSourceFile = new File(thisSourceFileName);
			String thisSourceChecksum = FileUtilsLocal.getFileChecksum(propertiesObject.getDigestType(), thisSourceFile);
//			logger.debug(String.format("This source file checksum: %s - %s: %s", thisSourceChecksum, digestType, thisSourceFileName));
			if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(thisSourceChecksum))
			{
//				logger.debug(String.format("Hashmap already contains an entry for checksum: %s with filename of %s", thisSourceChecksum, TargetFileHashMapSingleton.getInstance().getMap().get(thisSourceChecksum)));
				String existingfile = TargetFileHashMapSingleton.getInstance().getMap().get(thisSourceChecksum);
				String thisSourceFileNameOnly = FileUtilsLocal.getFileName(thisSourceFileName);
				String thisTargetFileNameOnly = FileUtilsLocal.getFileName(existingfile);

				if (thisSourceFileNameOnly.trim().equals(thisTargetFileNameOnly.trim()))
				{
					if (propertiesObject.isDryRun() == true)
					{
						logger.info(String.format("%s seems to be a copy of file:\r\n%s", thisSourceFileName, existingfile));
						MatchingFileHashMapSingleton.getInstance().addToMap(thisSourceFileName, existingfile);
					}
				}
				else
				{
					logger.info(String.format("%s seems to be a copy of file:\r\n%s", thisSourceFileName, existingfile));
					MatchingFileHashMapSingleton.getInstance().addToMap(thisSourceFileName, existingfile);
				}

			}
			else
			{
				String targetFileName = FileUtilsLocal.getFileName(thisSourceFileName);
				String targetFullPath = propertiesObject.getTargetLocation() + File.separatorChar + targetFileName;
				File targetFile = new File(targetFullPath);

//				String currentSourceBaseLocation = FileUtilsLocal.getFilePath(thisSourceFileName);
//				String destinationFileName = "";

				CopiedFileHashMapSingleton.getInstance().getMap().put(thisSourceFileName, targetFullPath);
				if (propertiesObject.isDryRun() == true)
				{
					logger.info(String.format("Would Copy File: %s to %s", thisSourceFileName, targetFullPath));
				}
				else
				{
					FileUtils.copyFile(thisSourceFile, targetFile, propertiesObject.isPreserveFileDate());
				}
			}
//			else
//			{
//				TargetFileHashMapSingleton.getInstance().addToMap(thisFileChecksum, fileString);
//			}
		}

		logger.debug(String.format("%s files not copied as there were duplicates with different names in the target location", MatchingFileHashMapSingleton.getInstance().getMap().size()));
//		for (int matchNumber = 0; matchNumber < MatchingFileHashMapSingleton.getInstance().getMap().size(); matchNumber++)
//		{
//			String sourceFileMatch = MatchingFileHashMapSingleton.getInstance().getMap().get(matchNumber);
////			logger.debug("===================================================================================");
////			logger.debug(msg);
////			logger.debug(msg);
////			logger.debug(msg);
////			logger.debug("===================================================================================");
//		}

//		for (String key : MatchingFileHashMapSingleton.getInstance().getMap().keySet())
//		{
//			logger.debug("===================================================================================");
//			logger.debug("Source File: " + key);
//			logger.debug("Target File: " + MatchingFileHashMapSingleton.getInstance().getMap().get(key));
////			logger.debug(msg);
////			logger.debug(msg);
//			logger.debug("===================================================================================");
//		}

		if (propertiesObject.isCreateOutputFile() == true)
		{
			logger.debug("Creating output file");
			ReportUtils.createOutputExcel();
		}

//		// get the source file directory list
//		FileUtils.getSourceDirectoryContentsArray(sourceLocation);
//		logger.debug(String.format("Source File Singleton Size: %s", SourceFileArraySingleton.getInstance().getArray().size()));
//		
//		// get the checksums for the source files and put them in a map
//		FileUtils.createSourceFileChecksumMap(SourceFileArraySingleton.getInstance(), digestType);

	}

	/*************************************************
	 * @param cliOptions
	 *************************************************/
	private static void showHelp(Options cliOptions)
	{
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Main.jar", cliOptions);
		System.exit(0);
	}

}
