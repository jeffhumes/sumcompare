package org.bofus.sumcompare.model;

import lombok.Data;

@Data
public class ExistingTargetFileObject {
	private String fileChecksum;
	private String currentFile;
	private String existingFile;
}
