package org.bofus.sumcompare.localutil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for detecting file types, specifically identifying video and
 * image files.
 */
public class FileTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(FileTypeDetector.class);

    // Common video file extensions
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v",
            "mpg", "mpeg", "3gp", "3g2", "m2ts", "mts", "ts", "vob",
            "ogv", "mxf", "rm", "rmvb", "asf", "divx", "f4v", "m2v"));

    // Common image file extensions
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp",
            "svg", "ico", "heic", "heif", "raw", "cr2", "nef", "orf",
            "arw", "dng", "psd", "ai", "eps", "xcf", "exr", "hdr"));

    /**
     * File type enumeration
     */
    public enum FileType {
        VIDEO,
        IMAGE,
        OTHER
    }

    /**
     * Detects the type of a file based on its extension and optionally MIME type.
     * 
     * @param file The file to detect
     * @return FileType enum indicating VIDEO, IMAGE, or OTHER
     */
    public static FileType detectFileType(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return FileType.OTHER;
        }

        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);

        // Check by extension first (fastest)
        if (VIDEO_EXTENSIONS.contains(extension)) {
            logger.debug("File {} detected as VIDEO by extension", file.getName());
            return FileType.VIDEO;
        }

        if (IMAGE_EXTENSIONS.contains(extension)) {
            logger.debug("File {} detected as IMAGE by extension", file.getName());
            return FileType.IMAGE;
        }

        // Optionally check MIME type for more accurate detection
        try {
            String mimeType = detectMimeType(file);
            if (mimeType != null) {
                if (mimeType.startsWith("video/")) {
                    logger.debug("File {} detected as VIDEO by MIME type: {}", file.getName(), mimeType);
                    return FileType.VIDEO;
                }
                if (mimeType.startsWith("image/")) {
                    logger.debug("File {} detected as IMAGE by MIME type: {}", file.getName(), mimeType);
                    return FileType.IMAGE;
                }
            }
        } catch (IOException e) {
            logger.debug("Could not detect MIME type for {}: {}", file.getName(), e.getMessage());
        }

        return FileType.OTHER;
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

    /**
     * Detects the MIME type of a file using Java NIO.
     * 
     * @param file The file to check
     * @return The MIME type string, or null if it cannot be determined
     * @throws IOException if an I/O error occurs
     */
    private static String detectMimeType(File file) throws IOException {
        Path path = file.toPath();
        return Files.probeContentType(path);
    }

    /**
     * Checks if a file is a video file.
     * 
     * @param file The file to check
     * @return true if the file is a video, false otherwise
     */
    public static boolean isVideo(File file) {
        return detectFileType(file) == FileType.VIDEO;
    }

    /**
     * Checks if a file is an image file.
     * 
     * @param file The file to check
     * @return true if the file is an image, false otherwise
     */
    public static boolean isImage(File file) {
        return detectFileType(file) == FileType.IMAGE;
    }

    /**
     * Checks if a file is a media file (video or image).
     * 
     * @param file The file to check
     * @return true if the file is a video or image, false otherwise
     */
    public static boolean isMediaFile(File file) {
        FileType type = detectFileType(file);
        return type == FileType.VIDEO || type == FileType.IMAGE;
    }

    /**
     * Gets a human-readable description of the file type.
     * 
     * @param file The file to check
     * @return A string describing the file type (e.g., "Video", "Image", "Other")
     */
    public static String getFileTypeDescription(File file) {
        FileType type = detectFileType(file);
        switch (type) {
            case VIDEO:
                return "Video";
            case IMAGE:
                return "Image";
            default:
                return "Other";
        }
    }

    /**
     * Gets detailed information about a file including its type and extension.
     * 
     * @param file The file to analyze
     * @return A formatted string with file type information
     */
    public static String getFileTypeInfo(File file) {
        if (file == null || !file.exists()) {
            return "File does not exist";
        }

        FileType type = detectFileType(file);
        String extension = getFileExtension(file.getName().toLowerCase());

        StringBuilder info = new StringBuilder();
        info.append("Type: ").append(getFileTypeDescription(file));
        if (!extension.isEmpty()) {
            info.append(", Extension: .").append(extension);
        }

        try {
            String mimeType = detectMimeType(file);
            if (mimeType != null) {
                info.append(", MIME: ").append(mimeType);
            }
        } catch (IOException e) {
            // MIME type detection is optional
        }

        return info.toString();
    }
}
