package org.bofus.sumcompare.model;

import lombok.Data;

@Data
public class FileDispositionObject {
    private String fileChecksum;
    private String currentFile;
    private String existingFile;
    private String dispositionAction;
    private String newFileLocation;
}
