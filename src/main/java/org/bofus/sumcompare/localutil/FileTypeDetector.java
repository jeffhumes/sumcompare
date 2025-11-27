// package org.bofus.sumcompare.localutil;

// import java.io.File;
// import java.io.IOException;

// import org.apache.tika.Tika;

// import lombok.extern.slf4j.Slf4j;

// /**
// * Utility class for detecting file types using Apache Tika.
// * Provides content-based file type detection rather than relying solely on
// file
// * extensions.
// */
// @Slf4j
// public class FileTypeDetector {

// // Thread-safe singleton Tika instance
// private static final Tika TIKA = new Tika();

// /**
// * File type enumeration
// */
// public enum FileType {
// VIDEO,
// IMAGE,
// OTHER
// }

// /**
// * Detects the type of a file using Apache Tika's content-based detection.
// * This method reads the actual file content to determine the type, rather
// than
// * relying solely on file extensions.
// *
// * @param file The file to detect
// * @return FileType enum indicating VIDEO, IMAGE, or OTHER
// */
// public static FileType detectFileType(File file) {
// if (file == null || !file.exists() || !file.isFile()) {
// return FileType.OTHER;
// }

// try {
// String mimeType = detectMimeType(file);
// if null != (mimeType) {
// if (mimeType.startsWith("video/")) {
// log.trace("File {} detected as VIDEO by Tika: {}", file.getName(), mimeType);
// return FileType.VIDEO;
// }
// if (mimeType.startsWith("image/")) {
// log.trace("File {} detected as IMAGE by Tika: {}", file.getName(), mimeType);
// return FileType.IMAGE;
// }
// }
// } catch (IOException e) {
// log.warn("Could not detect MIME type for {}: {}, falling back to OTHER",
// file.getName(), e.getMessage());
// }

// return FileType.OTHER;
// }

// /**
// * Checks if a file is a video file.
// *
// * @param file The file to check
// * @return true if the file is a video, false otherwise
// */
// public static boolean isVideo(File file) {
// return detectFileType(file) == FileType.VIDEO;
// }

// /**
// * Checks if a file is an image file.
// *
// * @param file The file to check
// * @return true if the file is an image, false otherwise
// */
// public static boolean isImage(File file) {
// return detectFileType(file) == FileType.IMAGE;
// }

// /**
// * Checks if a file is a media file (video or image).
// *
// * @param file The file to check
// * @return true if the file is a video or image, false otherwise
// */
// public static boolean isMediaFile(File file) {
// FileType type = detectFileType(file);
// return type == FileType.VIDEO || type == FileType.IMAGE;
// }

// /**
// * Gets a human-readable description of the file type.
// *
// * @param file The file to check
// * @return A string describing the file type (e.g., "Video", "Image", "Other")
// */
// public static String getFileTypeDescription(File file) {
// FileType type = detectFileType(file);
// switch (type) {
// case VIDEO:
// return "Video";
// case IMAGE:
// return "Image";
// default:
// return "Other";
// }
// }

// }
