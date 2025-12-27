package org.bofus.sumcompare.localutil;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;

import lombok.extern.slf4j.Slf4j;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.bofus.sumcompare.model.FileMetadata;
import org.bofus.sumcompare.model.PropertiesObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
/**
 * Utility class for extracting metadata dates from image and video files.
 * Supports EXIF data from images and creation dates from video files.
 * Uses Apache Tika for content-based file type detection.
 */
public class FileMetadataExtractor {

    // // Thread-safe singleton Tika instance for content-based detection
    // private static final Tika TIKA = new Tika();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static FileMetadata getFileMetadata(File file, PropertiesObject props)
            throws IOException, ImageProcessingException {

        log.debug("Beginning getFileMetadata for file: {}", file.getAbsolutePath());
        FileMetadata fileMetadata = new FileMetadata();

        fileMetadata.setAbsolutePath(file.getAbsolutePath());
        fileMetadata.setFileName(FileUtilsLocal.getFileName(file.getAbsolutePath()));
        fileMetadata.setFilePath(file.getAbsolutePath());
        fileMetadata.setSizeBytes(file.length());
        fileMetadata.setDirectory(file.isDirectory());
        fileMetadata.setReadOnly(!file.canWrite());
        fileMetadata.setHidden(file.isHidden());

        fileMetadata.setDateTargetLocation(null != props.getDateTargetDirectory()
                ? props.getDateTargetDirectory()
                : props.getSourceLocation());

        fileMetadata.setFileExtension(getFileExtension(file.getName()));

        // Sets mime type and media file flag - wrapped to not throw exceptions
        try {
            getMediaFileInfo(fileMetadata);
        } catch (Exception e) {
            // If TIKA fails completely, log and continue with non-media file defaults
            log.warn("Failed to detect MIME type for {}: {}", file.getName(), e.getMessage());
            fileMetadata.setMediaFile(false);
            fileMetadata.setMimeType("application/octet-stream");
        }

        extractFileOwner(file, fileMetadata);

        extractFileDates(fileMetadata);

        log.debug("Successfully completed getFileMetadata for file: {}", file.getName());
        return fileMetadata;
    }

    /**
     * Detects the MIME type and sets the media file flag.
     * Uses Apache Tika for content-based file type detection.
     * 
     * @param fileMetadata the file metadata object to populate
     */
    private static void getMediaFileInfo(FileMetadata fileMetadata) {
        File file = new File(fileMetadata.getFilePath());

        if (null != file && file.exists() && file.isFile()) {
            try {

                Magic magic = new Magic();
                MagicMatch match = magic.getMagicMatch(file, false);
                String mimeType = match.getMimeType();

                if (null != mimeType && !mimeType.isEmpty()) {
                    if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                        fileMetadata.setMediaFile(true);
                        fileMetadata.setMimeType(mimeType);
                        log.debug("Detected media file: {} with MIME type: {}", file.getName(), mimeType);
                    } else {
                        fileMetadata.setMediaFile(false);
                        log.debug("Non-media file: {} with MIME type: {}", file.getName(), mimeType);
                    }
                } else {
                    // Default to false if MIME type couldn't be determined
                    fileMetadata.setMediaFile(false);
                    log.debug("Could not determine MIME type for: {}, treating as non-media file", file.getName());
                }
            } catch (Exception e) {
                // Catch any other unexpected exceptions from TIKA
                log.error("Unexpected error detecting MIME type for {}: {}", file.getName(), e.getMessage(), e);
                fileMetadata.setMediaFile(false);
                fileMetadata.setMimeType("application/octet-stream");
            }
        } else {
            // File doesn't exist or isn't a file
            fileMetadata.setMediaFile(false);
            log.debug("File does not exist or is not a regular file: {}", fileMetadata.getFilePath());
        }
    }

    /**
     * Extracts the creation date from a media file's metadata.
     * For images: reads EXIF DateTimeOriginal, DateTimeDigitized, or DateTime
     * For videos: reads creation time from QuickTime or MP4 metadata
     * Falls back to file system dates if metadata is unavailable.
     *
     * @param file the media file
     * @return the creation date as an Instant, or null if unavailable
     * @throws IOException
     * @throws ImageProcessingException
     */
    private static void extractFileDates(FileMetadata fileMetadata) throws ImageProcessingException, IOException {
        File file = new File(fileMetadata.getFilePath());

        if (!file.exists() || !file.isFile()) {
            log.warn("File does not exist or is not a file: {}", file.getAbsolutePath());
            return; // Exit early if file doesn't exist
        }

        // NOTE: get filesystem dates
        Path path = file.toPath();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        // NOTE: Get simple file attributes
        fileMetadata.setCreationTime(formatFileTime(attrs.creationTime()));
        fileMetadata.setLastModifiedTime(formatFileTime(attrs.lastModifiedTime()));
        fileMetadata.setLastAccessTime(formatFileTime(attrs.lastAccessTime()));

        // For images, try to extract EXIF data
        if (fileMetadata.isMediaFile()) {
            // Try to read metadata for use in media files
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            extractExifDates(metadata, fileMetadata);
            extractVideoDates(metadata, fileMetadata);
        }

    }

    /**
     * Extracts EXIF date from image metadata.
     * Tries DateTimeOriginal, DateTimeDigitized, and DateTime in order.
     *
     * @param metadata the image metadata
     * @return the EXIF date, or null if not found
     */
    private static void extractExifDates(Metadata metadata, FileMetadata fileMetadata) {
        ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIFDDirectory == null) {
            log.debug("No EXIF SubIFD directory found in metadata.");
            return; // Exit early if no EXIF data
        }

        // Try DateTimeOriginal (when photo was taken)
        Date date = subIFDDirectory.getDateOriginal();
        if (null != date) {
            fileMetadata.setExifOriginalDate(formatTimestamp(date.getTime()));
        }

        // Try DateTimeDigitized (when photo was digitized)
        date = subIFDDirectory.getDateDigitized();
        if (null != date) {
            fileMetadata.setExifDigitizedDate(formatTimestamp(date.getTime()));
        }

        // Try generic DateTime
        date = subIFDDirectory.getDateModified();
        if (null != date) {
            fileMetadata.setExifModifiedDate(formatTimestamp(date.getTime()));
        }
    }

    /**
     * Extracts creation date from video metadata.
     * Supports QuickTime (MOV) and MP4 formats.
     *
     * @param metadata the video metadata
     * @return the creation date, or null if not found
     */
    private static void extractVideoDates(Metadata metadata, FileMetadata fileMetadata) {
        // Try QuickTime metadata (for MOV files)
        QuickTimeDirectory qtDirectory = metadata.getFirstDirectoryOfType(QuickTimeDirectory.class);
        if (null != qtDirectory) {
            Date date = qtDirectory.getDate(QuickTimeDirectory.TAG_CREATION_TIME);
            if (null != date) {
                fileMetadata.setVideoCreationDate(formatTimestamp(date.getTime()));
            }
        }

        // Try MP4 metadata
        Mp4Directory mp4Directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
        if (null != mp4Directory) {
            Date date = mp4Directory.getDate(Mp4Directory.TAG_CREATION_TIME);
            if (null != date) {
                fileMetadata.setVideoCreationDate(formatTimestamp(date.getTime()));
            }
        }
    }

    public static void extractFileOwner(File file, FileMetadata fileMetadata) {
        try {
            fileMetadata.setOwner(Files.getOwner(file.toPath()).getName());
        } catch (Exception e) {
            fileMetadata.setOwner("Unknown");
        }
    }

    /**
     * Gets the file extension from a filename.
     * 
     * @param fileName The filename
     * @return The extension (without dot) in lowercase, or empty string if no
     *         extension
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private static String formatFileTime(FileTime fileTime) {
        Instant instant = fileTime.toInstant();
        return FORMATTER.format(instant);
    }

    private static String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return FORMATTER.format(instant);
    }

    // public String getFormattedSize() {
    // if (sizeBytes < 1024) {
    // return sizeBytes + " B";
    // } else if (sizeBytes < 1024 * 1024) {
    // return String.format("%.2f KB", sizeBytes / 1024.0);
    // } else if (sizeBytes < 1024 * 1024 * 1024) {
    // return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
    // } else {
    // return String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
    // }
    // }

    // /**
    // * Returns a compact summary of the metadata.
    // */
    // public String getSummary() {
    // StringBuilder sb = new StringBuilder();
    // sb.append("Size: ").append(getFormattedSize());
    // sb.append(" | Modified: ").append(lastModifiedTime);
    // if (isReadOnly) {
    // sb.append(" | Read-only");
    // }
    // return sb.toString();
    // }

    // @Override
    // public String toString() {
    // return String.format(
    // "FileMetadata{path='%s', size=%s, created='%s', modified='%s', accessed='%s',
    // owner='%s', readOnly=%s, hidden=%s}",
    // filePath, getFormattedSize(), creationTime, lastModifiedTime,
    // lastAccessTime, owner, isReadOnly, isHidden);
    // }

}
