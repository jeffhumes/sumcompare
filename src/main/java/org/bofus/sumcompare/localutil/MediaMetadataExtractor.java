package org.bofus.sumcompare.localutil;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

/**
 * Utility class for extracting metadata dates from image and video files.
 * Supports EXIF data from images and creation dates from video files.
 */
public class MediaMetadataExtractor {
    private static final Logger log = LoggerFactory.getLogger(MediaMetadataExtractor.class);

    // Supported image extensions
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "heic", "heif", "webp", "raw", "cr2", "nef", "arw");

    // Supported video extensions
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mov", "avi", "mkv", "m4v", "3gp", "flv", "wmv", "mpg", "mpeg", "mts", "m2ts");

    /**
     * Checks if a file is a supported media file (image or video).
     *
     * @param file the file to check
     * @return true if the file is a supported media file
     */
    public static boolean isSupportedMediaFile(File file) {
        String extension = getFileExtension(file).toLowerCase();
        return IMAGE_EXTENSIONS.contains(extension) || VIDEO_EXTENSIONS.contains(extension);
    }

    /**
     * Extracts the creation date from a media file's metadata.
     * For images: reads EXIF DateTimeOriginal, DateTimeDigitized, or DateTime
     * For videos: reads creation time from QuickTime or MP4 metadata
     * Falls back to file system dates if metadata is unavailable.
     *
     * @param file the media file
     * @return the creation date as an Instant, or null if unavailable
     */
    public static Instant extractCreationDate(File file) {
        if (!file.exists() || !file.isFile()) {
            log.warn("File does not exist or is not a file: {}", file.getAbsolutePath());
            return null;
        }

        String extension = getFileExtension(file).toLowerCase();

        try {
            // Try to read metadata
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // For images, try EXIF data
            if (IMAGE_EXTENSIONS.contains(extension)) {
                Date exifDate = extractExifDate(metadata);
                if (exifDate != null) {
                    log.debug("Found EXIF date for {}: {}", file.getName(), exifDate);
                    return exifDate.toInstant();
                }
            }

            // For videos, try video metadata
            if (VIDEO_EXTENSIONS.contains(extension)) {
                Date videoDate = extractVideoDate(metadata);
                if (videoDate != null) {
                    log.debug("Found video metadata date for {}: {}", file.getName(), videoDate);
                    return videoDate.toInstant();
                }
            }

        } catch (ImageProcessingException e) {
            log.debug("Could not process metadata for {}: {}", file.getName(), e.getMessage());
        } catch (IOException e) {
            log.debug("IO error reading metadata for {}: {}", file.getName(), e.getMessage());
        }

        // Fallback: try file system creation time
        return getFileSystemCreationDate(file);
    }

    /**
     * Extracts EXIF date from image metadata.
     * Tries DateTimeOriginal, DateTimeDigitized, and DateTime in order.
     *
     * @param metadata the image metadata
     * @return the EXIF date, or null if not found
     */
    private static Date extractExifDate(Metadata metadata) {
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory == null) {
            return null;
        }

        // Try DateTimeOriginal (when photo was taken)
        Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        if (date != null) {
            return date;
        }

        // Try DateTimeDigitized (when photo was digitized)
        date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
        if (date != null) {
            return date;
        }

        // Try generic DateTime
        date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME);
        return date;
    }

    /**
     * Extracts creation date from video metadata.
     * Supports QuickTime (MOV) and MP4 formats.
     *
     * @param metadata the video metadata
     * @return the creation date, or null if not found
     */
    private static Date extractVideoDate(Metadata metadata) {
        // Try QuickTime metadata (for MOV files)
        QuickTimeDirectory qtDirectory = metadata.getFirstDirectoryOfType(QuickTimeDirectory.class);
        if (qtDirectory != null) {
            Date date = qtDirectory.getDate(QuickTimeDirectory.TAG_CREATION_TIME);
            if (date != null) {
                return date;
            }
        }

        // Try MP4 metadata
        Mp4Directory mp4Directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
        if (mp4Directory != null) {
            Date date = mp4Directory.getDate(Mp4Directory.TAG_CREATION_TIME);
            if (date != null) {
                return date;
            }
        }

        return null;
    }

    /**
     * Gets the file system creation date as a fallback.
     *
     * @param file the file
     * @return the creation date from file system attributes, or null if unavailable
     */
    private static Instant getFileSystemCreationDate(File file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attrs.creationTime().toInstant();
        } catch (IOException e) {
            log.debug("Could not read file system attributes for {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the file extension from a file.
     *
     * @param file the file
     * @return the extension (without dot), or empty string if none
     */
    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1);
        }
        return "";
    }
}
