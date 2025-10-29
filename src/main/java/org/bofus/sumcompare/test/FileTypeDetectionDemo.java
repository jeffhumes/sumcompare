package org.bofus.sumcompare.test;

import org.bofus.sumcompare.localutil.FileTypeDetector;
import java.io.File;

/**
 * Simple test class to demonstrate file type detection capabilities.
 */
public class FileTypeDetectionDemo {
    public static void main(String[] args) {
        System.out.println("=== File Type Detection Demo ===\n");

        // Test various file types
        String[] testFiles = {
                // Videos
                "sample.mp4", "movie.avi", "clip.mov", "video.mkv", "presentation.wmv",
                "animation.webm", "recording.flv", "mobile.3gp",

                // Images
                "photo.jpg", "picture.png", "graphic.gif", "scan.bmp",
                "logo.svg", "icon.ico", "raw_photo.nef", "design.psd",

                // Other
                "document.pdf", "script.sh", "data.txt", "archive.zip",
                "music.mp3", "audio.wav"
        };

        for (String filename : testFiles) {
            File testFile = new File("/tmp/" + filename);
            FileTypeDetector.FileType type = FileTypeDetector.detectFileType(testFile);
            String description = FileTypeDetector.getFileTypeDescription(testFile);
            boolean isVideo = FileTypeDetector.isVideo(testFile);
            boolean isImage = FileTypeDetector.isImage(testFile);
            boolean isMedia = FileTypeDetector.isMediaFile(testFile);

            System.out.printf("%-25s -> Type: %-8s | Video: %-5s | Image: %-5s | Media: %-5s%n",
                    filename, description, isVideo, isImage, isMedia);
        }

        System.out.println("\n=== Testing with actual files ===\n");

        // Test with actual files if they exist
        String[] actualPaths = {
                "/tmp/file_type_test/videos/sample.mp4",
                "/tmp/file_type_test/images/photo.jpg",
                "/tmp/file_type_test/other/document.pdf"
        };

        for (String path : actualPaths) {
            File file = new File(path);
            if (file.exists()) {
                String info = FileTypeDetector.getFileTypeInfo(file);
                System.out.printf("%-50s: %s%n", file.getName(), info);
            }
        }

        System.out.println("\n=== Supported Extensions ===");
        System.out.println("\nVideo formats: mp4, avi, mov, mkv, wmv, flv, webm, m4v, mpg, mpeg, 3gp, and more...");
        System.out.println(
                "Image formats: jpg, jpeg, png, gif, bmp, tiff, webp, svg, heic, raw, cr2, nef, psd, and more...");
    }
}
