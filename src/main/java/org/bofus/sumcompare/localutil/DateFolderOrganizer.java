package org.bofus.sumcompare.localutil;

import lombok.extern.slf4j.Slf4j;
import org.bofus.sumcompare.model.FileMetadata;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for organizing files into date-based folder structures.
 * Supports multiple organization patterns based on file metadata timestamps.
 */
@Slf4j
public class DateFolderOrganizer {

    /**
     * Enum defining different date-based folder organization strategies.
     */
    public enum DatePattern {
        /** Year-Month format: 2024-10 */
        YEAR_MONTH("yyyy-MM"),

        /** Year/Month format: 2024/10 */
        YEAR_MONTH_SLASH("yyyy/MM"),

        /** Year-Month-Day format: 2024-10-31 */
        YEAR_MONTH_DAY("yyyy-MM-dd"),

        /** Year/Month/Day format: 2024/10/31 */
        YEAR_MONTH_DAY_SLASH("yyyy/MM/dd"),

        /** Year only: 2024 */
        YEAR_ONLY("yyyy"),

        /** Year-Quarter format: 2024-Q4 */
        YEAR_QUARTER("yyyy-'Q'Q");

        private final String pattern;

        DatePattern(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }
    }

    /**
     * Enum defining which timestamp to use for date-based organization.
     */
    public enum DateSource {
        /** Use file creation time */
        CREATED,

        /** Use file last modified time (most common) */
        MODIFIED,

        /** Use file last access time */
        ACCESSED
    }

    // FIXME: when a file does not have EXIF data, the file modified date should be
    // used
    // FIXME: currently, it seems that the current date (today) is being used
    // instead

    /**
     * Generates a date-based folder path for the given file.
     * 
     * @param file       The file to organize
     * @param dateSource Which timestamp to use (created, modified, or accessed)
     * @param pattern    The folder structure pattern
     * @return The date-based folder path (e.g., "2024-10" or "2024/10/31")
     * @throws IOException If metadata cannot be retrieved
     */
    public static String getDateBasedFolder(File file, DateSource dateSource, DatePattern pattern) throws IOException {
        return getDateBasedFolder(file, dateSource, pattern, false);
    }

    /**
     * Generates a date-based folder path for the given file, optionally using
     * image/video metadata.
     * 
     * @param file        The file to organize
     * @param dateSource  Which timestamp to use (created, modified, or accessed)
     * @param pattern     The folder structure pattern
     * @param useMetadata Whether to try extracting date from image/video metadata
     *                    (EXIF, etc.)
     * @return The date-based folder path (e.g., "2024-10" or "2024/10/31")
     * @throws IOException If metadata cannot be retrieved
     */
    public static String getDateBasedFolder(File file, DateSource dateSource, DatePattern pattern, boolean useMetadata)
            throws IOException {
        LocalDateTime dateTime = null;

        // Try to use media metadata if enabled and file is a supported media type
        if (useMetadata && MediaMetadataExtractor.isSupportedMediaFile(file)) {
            try {
                java.time.Instant metadataDate = MediaMetadataExtractor.extractCreationDate(file);
                if (metadataDate != null) {
                    dateTime = LocalDateTime.ofInstant(metadataDate, java.time.ZoneId.systemDefault());
                    log.trace("Using metadata date for {}: {}", file.getName(), dateTime);
                }
            } catch (Exception e) {
                log.error("Could not extract metadata date for {}, falling back to file system: {}",
                        file.getName(), e.getMessage());
            }
        }

        // Fallback to file system metadata if no media metadata was found
        if (dateTime == null) {
            FileMetadata metadata = FileMetadata.fromFile(file);

            // Get the appropriate timestamp based on dateSource
            String timestamp;
            switch (dateSource) {
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

            // Parse the timestamp
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dateTime = LocalDateTime.parse(timestamp, inputFormatter);
        }

        // Format according to pattern
        // Handle quarter pattern specially
        if (pattern == DatePattern.YEAR_QUARTER) {
            int month = dateTime.getMonthValue();
            int quarter = (month - 1) / 3 + 1;
            return dateTime.getYear() + "-Q" + quarter;
        }

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(pattern.getPattern());
        return dateTime.format(outputFormatter);
    }

    /**
     * Generates the complete target path including date-based folder structure.
     * 
     * @param sourceFile          The source file being copied
     * @param baseTargetDir       The base target directory
     * @param dateSource          Which timestamp to use
     * @param pattern             The folder structure pattern
     * @param keepSourceStructure Whether to preserve source directory structure
     *                            after date folder
     * @return The complete target file path with date-based folders
     * @throws IOException If metadata cannot be retrieved
     */
    public static File generateDateBasedTargetPath(File sourceFile, File baseTargetDir,
            DateSource dateSource, DatePattern pattern,
            boolean keepSourceStructure) throws IOException {
        return generateDateBasedTargetPath(sourceFile, baseTargetDir, dateSource, pattern,
                keepSourceStructure, false);
    }

    /**
     * Generates the complete target path including date-based folder structure.
     * 
     * @param sourceFile          The source file being copied
     * @param baseTargetDir       The base target directory
     * @param dateSource          Which timestamp to use
     * @param pattern             The folder structure pattern
     * @param keepSourceStructure Whether to preserve source directory structure
     *                            after date folder
     * @param useMetadata         Whether to try extracting date from image/video
     *                            metadata
     * @return The complete target file path with date-based folders
     * @throws IOException If metadata cannot be retrieved
     */
    public static File generateDateBasedTargetPath(File sourceFile, File baseTargetDir,
            DateSource dateSource, DatePattern pattern,
            boolean keepSourceStructure, boolean useMetadata) throws IOException {
        String dateFolder = getDateBasedFolder(sourceFile, dateSource, pattern, useMetadata);

        if (keepSourceStructure) {
            // Preserve source structure within date folder
            String relativePath = sourceFile.getPath();
            File targetWithDate = new File(baseTargetDir, dateFolder);
            return new File(targetWithDate, relativePath);
        } else {
            // Flatten to just date folder + filename
            File targetDateDir = new File(baseTargetDir, dateFolder);
            return new File(targetDateDir, sourceFile.getName());
        }
    }

    /**
     * Creates the date-based folder structure if it doesn't exist.
     * 
     * @param targetFile The target file whose parent directories should be created
     * @return true if directories were created or already exist, false on failure
     */
    public static boolean ensureDateFolderExists(File targetFile) {
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.trace("Created date-based folder structure: {}", parentDir.getAbsolutePath());
            }
            return created;
        }
        return true;
    }

    /**
     * Gets a human-readable description of the date organization strategy.
     * 
     * @param dateSource The date source being used
     * @param pattern    The pattern being used
     * @return A descriptive string
     */
    public static String getOrganizationDescription(DateSource dateSource, DatePattern pattern) {
        String sourceDesc;
        switch (dateSource) {
            case CREATED:
                sourceDesc = "creation date";
                break;
            case ACCESSED:
                sourceDesc = "last access date";
                break;
            case MODIFIED:
            default:
                sourceDesc = "modification date";
                break;
        }

        String patternDesc;
        switch (pattern) {
            case YEAR_MONTH:
                patternDesc = "YYYY-MM folders";
                break;
            case YEAR_MONTH_SLASH:
                patternDesc = "YYYY/MM folders";
                break;
            case YEAR_MONTH_DAY:
                patternDesc = "YYYY-MM-DD folders";
                break;
            case YEAR_MONTH_DAY_SLASH:
                patternDesc = "YYYY/MM/DD folders";
                break;
            case YEAR_ONLY:
                patternDesc = "YYYY folders";
                break;
            case YEAR_QUARTER:
                patternDesc = "YYYY-QN folders";
                break;
            default:
                patternDesc = "date-based folders";
                break;
        }

        return String.format("Organizing by %s into %s", sourceDesc, patternDesc);
    }
}
