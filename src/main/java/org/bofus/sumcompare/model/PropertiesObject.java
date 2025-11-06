package org.bofus.sumcompare.model;

import org.bofus.sumcompare.localutil.DateFolderOrganizer;

import lombok.Data;

import java.security.MessageDigest;

@Data
public class PropertiesObject {
	private String sourceLocation;
	private String targetLocation;
	private MessageDigest digestType;
	private boolean postCopyRemove;
	private boolean preserveFileDate;
	private boolean createOutputFile;
	private boolean dryRun;
	private boolean backupFirst;
	private boolean keepSourceStructure;
	private boolean organizeDateFolders;
	private DateFolderOrganizer.DateSource dateSource;
	private DateFolderOrganizer.DatePattern datePattern;
	private boolean sourceDuplicateCheckOnly;
	private String dateTargetDirectory;
	private boolean useMetadata;
	private boolean renameDuplicates;
	private String duplicatePrefix;
	private boolean deleteEmptyFolders;
	private boolean moveInsteadOfCopy;
	private boolean permanentlyDelete;
	private int threadCount;
}
