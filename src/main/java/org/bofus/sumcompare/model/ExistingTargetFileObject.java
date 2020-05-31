package org.bofus.sumcompare.model;

public class ExistingTargetFileObject
{
	private String	fileChecksum;
	private String	currentFile;
	private String	existingFile;

	public String getFileChecksum()
	{
		return fileChecksum;
	}

	public void setFileChecksum(String fileChecksum)
	{
		this.fileChecksum = fileChecksum;
	}

	public String getCurrentFile()
	{
		return currentFile;
	}

	public void setCurrentFile(String currentFile)
	{
		this.currentFile = currentFile;
	}

	public String getExistingFile()
	{
		return existingFile;
	}

	public void setExistingFile(String existingFile)
	{
		this.existingFile = existingFile;
	}

}
