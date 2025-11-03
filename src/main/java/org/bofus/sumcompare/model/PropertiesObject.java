package org.bofus.sumcompare.model;

import org.bofus.sumcompare.localutil.DateFolderOrganizer;

import java.security.MessageDigest;

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

	public String getSourceLocation() {
		return sourceLocation;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public String getTargetLocation() {
		return targetLocation;
	}

	public void setTargetLocation(String targetLocation) {
		this.targetLocation = targetLocation;
	}

	public MessageDigest getDigestType() {
		return digestType;
	}

	public void setDigestType(MessageDigest digestType) {
		this.digestType = digestType;
	}

	public boolean isPostCopyRemove() {
		return postCopyRemove;
	}

	public void setPostCopyRemove(boolean postCopyRemove) {
		this.postCopyRemove = postCopyRemove;
	}

	public boolean isPreserveFileDate() {
		return preserveFileDate;
	}

	public void setPreserveFileDate(boolean preserveFileDate) {
		this.preserveFileDate = preserveFileDate;
	}

	public boolean isCreateOutputFile() {
		return createOutputFile;
	}

	public void setCreateOutputFile(boolean createOutputFile) {
		this.createOutputFile = createOutputFile;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public boolean isBackupFirst() {
		return backupFirst;
	}

	public void setBackupFirst(boolean backupFirst) {
		this.backupFirst = backupFirst;
	}

	public boolean isKeepSourceStructure() {
		return keepSourceStructure;
	}

	public void setKeepSourceStructure(boolean keepSourceStructure) {
		this.keepSourceStructure = keepSourceStructure;
	}

	public boolean isOrganizeDateFolders() {
		return organizeDateFolders;
	}

	public void setOrganizeDateFolders(boolean organizeDateFolders) {
		this.organizeDateFolders = organizeDateFolders;
	}

	public DateFolderOrganizer.DateSource getDateSource() {
		return dateSource;
	}

	public void setDateSource(DateFolderOrganizer.DateSource dateSource) {
		this.dateSource = dateSource;
	}

	public DateFolderOrganizer.DatePattern getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(DateFolderOrganizer.DatePattern datePattern) {
		this.datePattern = datePattern;
	}

	public boolean isSourceDuplicateCheckOnly() {
		return sourceDuplicateCheckOnly;
	}

	public void setSourceDuplicateCheckOnly(boolean sourceDuplicateCheckOnly) {
		this.sourceDuplicateCheckOnly = sourceDuplicateCheckOnly;
	}

	public String getDateTargetDirectory() {
		return dateTargetDirectory;
	}

	public void setDateTargetDirectory(String dateTargetDirectory) {
		this.dateTargetDirectory = dateTargetDirectory;
	}

	public boolean isUseMetadata() {
		return useMetadata;
	}

	public void setUseMetadata(boolean useMetadata) {
		this.useMetadata = useMetadata;
	}

}
