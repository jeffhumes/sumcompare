package org.bofus.sumcompare.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Model class for storing file metadata information.
 * Captures creation time, modification time, size, and other file attributes.
 */
public class FileMetadata {

    private String filePath;
    private long sizeBytes;
    private String creationTime;
    private String lastModifiedTime;
    private String lastAccessTime;
    private boolean isReadOnly;
    private boolean isHidden;
    private boolean isDirectory;
    private String owner;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Creates a FileMetadata object from a File.
     * 
     * @param file The file to extract metadata from
     * @return FileMetadata object with populated attributes
     * @throws IOException if metadata cannot be read
     */
    public static FileMetadata fromFile(File file) throws IOException {
        FileMetadata metadata = new FileMetadata();
        metadata.setFilePath(file.getAbsolutePath());
        metadata.setSizeBytes(file.length());
        metadata.setDirectory(file.isDirectory());
        metadata.setReadOnly(!file.canWrite());
        metadata.setHidden(file.isHidden());

        // Use NIO for more detailed attributes
        try {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            metadata.setCreationTime(formatFileTime(attrs.creationTime()));
            metadata.setLastModifiedTime(formatFileTime(attrs.lastModifiedTime()));
            metadata.setLastAccessTime(formatFileTime(attrs.lastAccessTime()));

            // Try to get owner (may not be available on all systems)
            try {
                metadata.setOwner(Files.getOwner(path).getName());
            } catch (Exception e) {
                metadata.setOwner("Unknown");
            }
        } catch (IOException e) {
            // Fallback to basic file methods
            metadata.setCreationTime("Unknown");
            metadata.setLastModifiedTime(formatTimestamp(file.lastModified()));
            metadata.setLastAccessTime("Unknown");
            metadata.setOwner("Unknown");
        }

        return metadata;
    }

    private static String formatFileTime(FileTime fileTime) {
        Instant instant = fileTime.toInstant();
        return FORMATTER.format(instant);
    }

    private static String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return FORMATTER.format(instant);
    }

    /**
     * Returns a human-readable size string (e.g., "1.5 MB", "432 KB").
     */
    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Returns a compact summary of the metadata.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Size: ").append(getFormattedSize());
        sb.append(" | Modified: ").append(lastModifiedTime);
        if (isReadOnly) {
            sb.append(" | Read-only");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "FileMetadata{path='%s', size=%s, created='%s', modified='%s', accessed='%s', owner='%s', readOnly=%s, hidden=%s}",
                filePath, getFormattedSize(), creationTime, lastModifiedTime,
                lastAccessTime, owner, isReadOnly, isHidden);
    }

    // Getters and Setters

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(String lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
