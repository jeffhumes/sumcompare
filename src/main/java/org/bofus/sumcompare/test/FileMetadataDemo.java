package org.bofus.sumcompare.test;

import org.bofus.sumcompare.localutil.FileMetadataUtils;
import org.bofus.sumcompare.model.FileMetadata;

import java.io.File;

/**
 * Demo class to showcase file metadata retrieval capabilities.
 */
public class FileMetadataDemo {

    public static void main(String[] args) {
        System.out.println("=== File Metadata Demonstration ===\n");

        // Example 1: Get metadata from current directory
        File currentDir = new File(".");
        demonstrateMetadata(currentDir);

        // Example 2: Get metadata for pom.xml if it exists
        File pomFile = new File("pom.xml");
        if (pomFile.exists()) {
            System.out.println("\n--- POM File Metadata ---");
            demonstrateMetadata(pomFile);
        }

        // Example 3: Get metadata for README if it exists
        File readmeFile = new File("README.md");
        if (readmeFile.exists()) {
            System.out.println("\n--- README File Metadata ---");
            demonstrateMetadata(readmeFile);
        }

        // Example 4: Demonstrate metadata comparison
        if (pomFile.exists() && readmeFile.exists()) {
            System.out.println("\n--- Metadata Comparison ---");
            boolean sameMetadata = FileMetadataUtils.haveSameMetadata(pomFile, readmeFile);
            System.out.println("pom.xml and README.md have same metadata: " + sameMetadata);
        }

        // Example 5: Check recent modifications
        if (pomFile.exists()) {
            System.out.println("\n--- Recent Modification Check ---");
            boolean modifiedRecently = FileMetadataUtils.wasModifiedWithinHours(pomFile, 24);
            System.out.println("pom.xml was modified in last 24 hours: " + modifiedRecently);
        }
    }

    private static void demonstrateMetadata(File file) {
        try {
            FileMetadata metadata = FileMetadata.fromFile(file);

            System.out.println("File: " + file.getName());
            System.out.println("  Path: " + metadata.getFilePath());
            System.out.println("  Size: " + metadata.getFormattedSize());
            System.out.println("  Created: " + metadata.getCreationTime());
            System.out.println("  Modified: " + metadata.getLastModifiedTime());
            System.out.println("  Accessed: " + metadata.getLastAccessTime());
            System.out.println("  Owner: " + metadata.getOwner());
            System.out.println("  Read-only: " + metadata.isReadOnly());
            System.out.println("  Hidden: " + metadata.isHidden());
            System.out.println("  Directory: " + metadata.isDirectory());
            System.out.println("  Summary: " + metadata.getSummary());

            // Log detailed metadata using utility
            System.out.println("\n  Detailed logging:");
            FileMetadataUtils.logDetailedMetadata(file);

        } catch (Exception e) {
            System.err.println("Error retrieving metadata for " + file.getName() + ": " + e.getMessage());
        }
    }
}
