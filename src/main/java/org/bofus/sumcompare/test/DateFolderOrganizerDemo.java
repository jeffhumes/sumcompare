package org.bofus.sumcompare.test;

import org.bofus.sumcompare.localutil.DateFolderOrganizer;
import org.bofus.sumcompare.model.FileMetadata;

import java.io.File;
import java.io.IOException;

/**
 * Demo class to showcase date-based folder organization capabilities.
 */
public class DateFolderOrganizerDemo {

    public static void main(String[] args) {
        System.out.println("=== Date-Based Folder Organization Demonstration ===\n");

        // Get some test files
        File pomFile = new File("pom.xml");
        File readmeFile = new File("README.md");
        File currentDir = new File(".");

        File[] testFiles = { pomFile, readmeFile };
        File baseTargetDir = new File("/tmp/sumcompare-target");

        // Test all date sources
        DateFolderOrganizer.DateSource[] sources = DateFolderOrganizer.DateSource.values();

        // Test all patterns
        DateFolderOrganizer.DatePattern[] patterns = DateFolderOrganizer.DatePattern.values();

        System.out.println("--- Testing Different Date Sources ---\n");

        for (DateFolderOrganizer.DateSource source : sources) {
            System.out.println("Date Source: " + source);
            for (File file : testFiles) {
                if (file.exists()) {
                    demonstrateDateFolder(file, source, DateFolderOrganizer.DatePattern.YEAR_MONTH);
                }
            }
            System.out.println();
        }

        System.out.println("\n--- Testing Different Date Patterns ---\n");

        for (DateFolderOrganizer.DatePattern pattern : patterns) {
            System.out.println("Pattern: " + pattern + " (" + pattern.getPattern() + ")");
            if (pomFile.exists()) {
                demonstrateDateFolder(pomFile, DateFolderOrganizer.DateSource.MODIFIED, pattern);
            }
            System.out.println();
        }

        System.out.println("\n--- Testing Complete Path Generation ---\n");

        for (File file : testFiles) {
            if (file.exists()) {
                demonstrateFullPath(file, baseTargetDir, false);
                demonstrateFullPath(file, baseTargetDir, true);
            }
        }

        System.out.println("\n--- Organization Descriptions ---\n");

        for (DateFolderOrganizer.DatePattern pattern : patterns) {
            String desc = DateFolderOrganizer.getOrganizationDescription(
                    DateFolderOrganizer.DateSource.MODIFIED, pattern);
            System.out.println(desc);
        }
    }

    private static void demonstrateDateFolder(File file, DateFolderOrganizer.DateSource source,
            DateFolderOrganizer.DatePattern pattern) {
        try {
            FileMetadata metadata = FileMetadata.fromFile(file);
            String dateFolder = DateFolderOrganizer.getDateBasedFolder(file, source, pattern);

            String timestamp;
            switch (source) {
                case CREATED:
                    timestamp = metadata.getCreationTime();
                    break;
                case ACCESSED:
                    timestamp = metadata.getLastAccessTime();
                    break;
                case MODIFIED:
                default:
                    timestamp = metadata.getLastModifiedTime();
                    break;
            }

            System.out.println("  " + file.getName() + " (" + timestamp + ") -> " + dateFolder);
        } catch (IOException e) {
            System.err.println("  Error processing " + file.getName() + ": " + e.getMessage());
        }
    }

    private static void demonstrateFullPath(File file, File baseTargetDir, boolean keepStructure) {
        try {
            File targetPath = DateFolderOrganizer.generateDateBasedTargetPath(
                    file,
                    baseTargetDir,
                    DateFolderOrganizer.DateSource.MODIFIED,
                    DateFolderOrganizer.DatePattern.YEAR_MONTH,
                    keepStructure);

            System.out.println("Source: " + file.getAbsolutePath());
            System.out.println("Target: " + targetPath.getAbsolutePath());
            System.out.println("Keep Structure: " + keepStructure);
            System.out.println();
        } catch (IOException e) {
            System.err.println("Error generating path: " + e.getMessage());
        }
    }
}
