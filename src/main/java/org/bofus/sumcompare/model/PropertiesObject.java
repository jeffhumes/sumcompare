package org.bofus.sumcompare.model;

import java.security.MessageDigest;

public class PropertiesObject
{
	private String			sourceLocation;
	private String			targetLocation;
	private MessageDigest	digestType;
	private boolean			postCopyRemove;
	private boolean			preserveFileDate;
	private boolean			createOutputFile;
	private boolean			dryRun;
	private boolean			backupFirst;

	public String getSourceLocation()
	{
		return sourceLocation;
	}

	public void setSourceLocation(String sourceLocation)
	{
		this.sourceLocation = sourceLocation;
	}

	public String getTargetLocation()
	{
		return targetLocation;
	}

	public void setTargetLocation(String targetLocation)
	{
		this.targetLocation = targetLocation;
	}

	public MessageDigest getDigestType()
	{
		return digestType;
	}

	public void setDigestType(MessageDigest digestType)
	{
		this.digestType = digestType;
	}

	public boolean isPostCopyRemove()
	{
		return postCopyRemove;
	}

	public void setPostCopyRemove(boolean postCopyRemove)
	{
		this.postCopyRemove = postCopyRemove;
	}

	public boolean isPreserveFileDate()
	{
		return preserveFileDate;
	}

	public void setPreserveFileDate(boolean preserveFileDate)
	{
		this.preserveFileDate = preserveFileDate;
	}

	public boolean isCreateOutputFile()
	{
		return createOutputFile;
	}

	public void setCreateOutputFile(boolean createOutputFile)
	{
		this.createOutputFile = createOutputFile;
	}

	public boolean isDryRun()
	{
		return dryRun;
	}

	public void setDryRun(boolean dryRun)
	{
		this.dryRun = dryRun;
	}

	public boolean isBackupFirst()
	{
		return backupFirst;
	}

	public void setBackupFirst(boolean backupFirst)
	{
		this.backupFirst = backupFirst;
	}

}
