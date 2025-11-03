package org.bofus.sumcompare.localutil;

import org.bofus.sumcompare.model.FileMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Utility class for advanced file metadata operations.
 * Provides extended metadata retrieval beyond basic FileMetadata.
 */
@Slf4j
public class FileMetadataUtils {

    /**
     * Logs detailed metadata information for a file.
     * 
     * @param file The file to log metadata for
     */
    public static void logDetailedMetadata(File file) {
        try {
            FileMetadata metadata = FileMetadata.fromFile(file);
            log.info("=== File Metadata ===");
            log.info(metadata.toString());

            // Additional attributes
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            log.info("Is Regular File: {}", attrs.isRegularFile());
            log.info("Is Symbolic Link: {}", attrs.isSymbolicLink());
            log.info("File Key: {}", attrs.fileKey());

            // Try to get POSIX permissions (Unix/Linux/Mac)
            try {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
                log.info("POSIX Permissions: {}", formatPosixPermissions(permissions));
            } catch (UnsupportedOperationException e) {
                log.error("POSIX permissions not supported on this file system");
            }

        } catch (IOException e) {
            log.error("Failed to retrieve metadata for: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Formats POSIX permissions as a readable string (e.g., "rwxr-xr--").
     */
    private static String formatPosixPermissions(Set<PosixFilePermission> permissions) {
        StringBuilder sb = new StringBuilder(9);

        sb.append(permissions.contains(PosixFilePermission.OWNER_READ) ? 'r' : '-');
        sb.append(permissions.contains(PosixFilePermission.OWNER_WRITE) ? 'w' : '-');
        sb.append(permissions.contains(PosixFilePermission.OWNER_EXECUTE) ? 'x' : '-');

        sb.append(permissions.contains(PosixFilePermission.GROUP_READ) ? 'r' : '-');
        sb.append(permissions.contains(PosixFilePermission.GROUP_WRITE) ? 'w' : '-');
        sb.append(permissions.contains(PosixFilePermission.GROUP_EXECUTE) ? 'x' : '-');

        sb.append(permissions.contains(PosixFilePermission.OTHERS_READ) ? 'r' : '-');
        sb.append(permissions.contains(PosixFilePermission.OTHERS_WRITE) ? 'w' : '-');
        sb.append(permissions.contains(PosixFilePermission.OTHERS_EXECUTE) ? 'x' : '-');

        return sb.toString();
    }

    /**
     * Compares two files' metadata to determine if they have the same
     * characteristics.
     * 
     * @param file1 First file
     * @param file2 Second file
     * @return true if files have same size and modification time
     */
    public static boolean haveSameMetadata(File file1, File file2) {
        try {
            FileMetadata meta1 = FileMetadata.fromFile(file1);
            FileMetadata meta2 = FileMetadata.fromFile(file2);

            return meta1.getSizeBytes() == meta2.getSizeBytes() &&
                    meta1.getLastModifiedTime().equals(meta2.getLastModifiedTime());
        } catch (IOException e) {
            log.error("Failed to compare metadata", e);
            return false;
        }
    }

    /**
     * Checks if a file was modified within the last N hours.
     * 
     * @param file  The file to check
     * @param hours Number of hours to check
     * @return true if file was modified within the specified time
     */
    public static boolean wasModifiedWithinHours(File file, int hours) {
        long currentTime = System.currentTimeMillis();
        long fileTime = file.lastModified();
        long hoursInMillis = hours * 60 * 60 * 1000L;

        return (currentTime - fileTime) <= hoursInMillis;
    }

    /**
     * Gets a compact metadata summary for logging.
     * 
     * @param file The file to get metadata for
     * @return Compact metadata string or error message
     */
    public static String getCompactMetadata(File file) {
        try {
            FileMetadata metadata = FileMetadata.fromFile(file);
            return metadata.getSummary();
        } catch (IOException e) {
            return "Unable to retrieve metadata: " + e.getMessage();
        }
    }
}
