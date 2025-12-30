# SumCompare

Intelligent file deduplication and organization tool with a modern JavaFX GUI and powerful checksum-based comparison.

## Features

### Core Functionality

- **Checksum-based deduplication**: Compares files using cryptographic hashes to avoid copying duplicates
- **Multiple hash algorithms**: Supports MD5, SHA1, XXHash32, and XXHash64 (recommended)
- **JavaFX GUI**: Modern, user-friendly graphical interface with real-time progress tracking
- **Parallel processing**: Multi-threaded checksum computation for maximum performance
- **Dry-run mode**: Preview all operations without making any changes

### Advanced Features

- **Date-based organization**: Automatically organize files into date folders (YYYY-MM, YYYY/MM/DD, etc.)
- **EXIF metadata extraction**: Extract creation dates from photos and videos for accurate date organization
- **Source duplicate checking**: Find duplicates within the source directory itself
- **Move or copy**: Choose to move files (with trash/permanent delete options) or copy them
- **Structure preservation**: Optionally maintain source directory structure in target
- **Duplicate file renaming**: Automatically rename duplicates with custom prefix instead of skipping
- **Empty folder cleanup**: Automatically remove empty folders after moving files
- **Excel reports**: Generate detailed reports of all operations with three sheets (copied, duplicates, existing)
- **File type detection**: Content-based detection using Apache Tika (not extension-based)
- **Real-time logging**: Live log window showing all operations as they happen

## Quick Start

### Running the GUI

```bash
java -jar target/sumcompare.jar
```

The application launches a graphical interface where you can:

1. Select source and target directories using browse buttons
2. Choose your checksum algorithm (XXHASH64 recommended for speed)
3. Configure options via checkboxes
4. See real-time progress and logs
5. Review detailed operation reports

### CLI Mode

> **Note**: CLI mode is currently disabled. Please use the GUI interface

## Hash Algorithms

### MD5

- **Speed**: Fast
- **Size**: 128 bits (16 bytes)
- **Use case**: Legacy compatibility, not cryptographically secure

### SHA1

- **Speed**: Moderate
- **Size**: 160 bits (20 bytes)
- **Use case**: Better security than MD5, widely supported

### XXHash32

- **Speed**: Extremely fast (multiple GB/s)
- **Size**: 32 bits (4 bytes)
- **Use case**: High-speed deduplication, non-cryptographic

### XXHash64

- **Speed**: Extremely fast (multiple GB/s)
- **Size**: 64 bits (8 bytes)
- **Use case**: High-speed deduplication with better collision resistance than XXHash32

**Recommendation**: Use XXHash64 for best performance on large file sets. Use SHA1 if you need cryptographic properties.

### CLI Mode

> **Note**: CLI mode is currently disabled. Please use the GUI interface.

## GUI Features

### Main Window

- **Directory Selection**: Browse buttons for source, target, and date-organization directories
- **Algorithm Selection**: Dropdown menu with XXHASH64, XXHASH32, SHA1, and MD5
- **Thread Count**: Adjustable worker threads for parallel processing
- **Real-time Statistics**: Live counters for scanned, copied, and duplicate files
- **Elapsed Time Tracker**: Shows operation duration in real-time
- **Progress Bar**: Visual feedback during operations

### Operation Modes

#### Standard Copy Mode

Copy files from source to target, skipping duplicates that already exist.

#### Source Duplicate Check Mode

Analyze source directory for internal duplicates without copying to target. Useful for cleaning up photo libraries or finding duplicate downloads.

#### Date Organization Mode

Organize files into date-based folders using file metadata or EXIF data. Can be combined with duplicate checking or used standalone for reorganization.

### Options

**Basic Options:**

- **Dry Run**: Preview all operations without making changes - see exactly what would happen
- **Keep Structure**: Preserve source directory hierarchy in target
- **Backup Source First**: Create zip backup before processing
- **Preserve File Date**: Maintain original file timestamps
- **Create Report**: Generate Excel file with operation details

**Date Organization:**

- **Date Folders**: Enable date-based folder organization
- **Date Source**: Choose MODIFIED, CREATED, or ACCESSED timestamp
- **Date Pattern**: Select folder structure (YYYY-MM, YYYY/MM/DD, YYYY-Q1, etc.)
- **Date Target**: Specify separate target directory for organized files
- **Use Metadata**: Extract EXIF dates from photos/videos for accurate organization

**Duplicate Handling:**

- **Rename Duplicates**: Add prefix to duplicate files instead of skipping
- **Custom Prefix**: Customize the duplicate file prefix (default: "DUPLICATE*FILE*")

**File Operations:**

- **Move Files**: Move instead of copy (removes from source)
- **Permanently Delete**: When moving, delete source files permanently vs. trash
- **Delete Empty Folders**: Remove empty directories after moving files

**Logging:**

- **Write Log to File**: Save detailed logs to disk
- **Log Directory**: Choose where to save log files
- **View Log Button**: Open logs in new window or current output area

## Common Use Cases

### 1. Deduplicate Photo Library

**Goal**: Copy photos from camera/phone to library, skip duplicates

- Source: Camera SD card or phone export
- Target: Your main photo library
- Enable: Create Report
- Algorithm: XXHASH64 (fastest)

### 2. Organize Photos by Date

**Goal**: Reorganize existing photos into YYYY-MM folders by date taken

- Source: Unorganized photo folder
- Target: Same or different location
- Enable: Date Folders, Use Metadata, Date Pattern = YEAR_MONTH
- Source Duplicate Check: Yes (if source = target)

### 3. Clean Up Downloads Folder

**Goal**: Find and handle duplicate files in Downloads

- Source: Downloads folder
- Target: Downloads folder (same)
- Enable: Source Duplicate Check, Rename Duplicates
- Review duplicates, then manually delete if desired

### 4. Archive Old Files

**Goal**: Move old files to archive, remove from source

- Source: Current working directory
- Target: Archive location
- Enable: Move Files, Delete Empty Folders, Preserve File Date
- Choose: Move to trash (safe) or Permanently Delete (careful!)

### 5. Backup with Deduplication

**Goal**: Incremental backup - only copy new/changed files

- Source: Important documents
- Target: Backup drive
- Enable: Backup Source First, Preserve File Date, Create Report
- Algorithm: SHA1 (cryptographically secure)

### 6. Preview Before Copying

**Goal**: See what would happen without making changes

- Enable: Dry Run
- Review log output and statistics
- Disable Dry Run and run again when satisfied

## Build

### Standard JAR (requires Java 21)

```bash
mvn clean package
```

This creates `target/sumcompare.jar` with all dependencies bundled.

**Run the GUI:**

```bash
java -jar target/sumcompare.jar
```

### Native Executable/Installer (no Java required)

Create platform-specific installers with bundled Java runtime:

```bash
./build-native.sh
```

This creates:

- **Linux**: `.deb` package
- **macOS**: `.dmg` installer
- **Windows**: `.msi` installer

Users can install and run without having Java installed.

See [NATIVE_BUILD.md](NATIVE_BUILD.md) for detailed instructions and options.

## Requirements

### For Running

- **JAR version**: Java 21 (LTS)
- **Native version**: No requirements (Java bundled)

### For Building

- Java 21 (LTS)
- Maven 3.x

## How It Works

### Processing Pipeline

1. **Initialization**

   - User selects directories and options in GUI
   - Settings are validated and populated into singleton `PropertiesObject`
   - Optional: Source backup created as zip file

2. **Target Directory Scan** (parallel thread)

   - Recursively scans all files in target directory
   - Computes checksums using selected algorithm
   - Stores in `TargetFileHashMapSingleton` (checksum → filepath mapping)
   - Detects duplicate files already in target

3. **Source Directory Scan** (parallel thread)

   - Recursively scans all files in source directory
   - Stores file paths in `SourceFileArraySingleton`
   - Both scans run simultaneously for efficiency

4. **Source File Processing** (multi-threaded worker pool)
   - For each source file:
     - Extract file metadata (size, dates, EXIF if media file)
     - Compute checksum
     - Check if checksum exists in target map
5. **Deduplication Logic**

   - **If checksum exists in target:**
     - If filename matches → skip (already exists)
     - If filename differs → mark as duplicate
     - If "Rename Duplicates" enabled → rename source file with prefix
   - **If checksum doesn't exist:**
     - Calculate target path (with date folders if enabled)
     - Copy or move file to target
     - Update statistics and logs

6. **Post-Processing**

   - Delete empty folders if enabled
   - Generate Excel report with three sheets:
     - **Copied Files**: All successfully copied/moved files
     - **Target Duplicates**: Files that already existed in target
     - **Source Duplicates**: Multiple files in source with same checksum

7. **Results Display**
   - Final statistics shown in GUI
   - Detailed log available in log window
   - Excel report saved to current directory

### Thread Architecture

- **Main Thread**: JavaFX UI and event handling
- **Task Thread**: Coordinates overall operation
- **Source Scan Thread**: Recursively lists source files
- **Target Scan Thread**: Recursively scans and checksums target files
- **Worker Thread Pool**: Configurable thread count (default: CPU cores) for parallel file processing

### Data Flow

```
Source Dir ──┐
             ├──> PropertiesObject (singleton)
Target Dir ──┘

Target Scan ──> TargetFileHashMapSingleton ──┐
Source Scan ──> SourceFileArraySingleton ────┼──> Worker Pool ──> Process Each File
EXIF/Metadata ───────────────────────────────┘         │
                                                        ├──> CopiedFileHashMapSingleton
                                                        ├──> MatchingFileHashMapSingleton
                                                        └──> ExistingTargetFileObjectArraySingleton
                                                                    │
                                                                    └──> Excel Report
```

## File Type Detection

The tool uses **Apache Tika** for intelligent content-based file type detection, particularly important for accurate date organization of photos and videos.

### Why Content-Based Detection?

- **Accurate identification**: Works even with wrong, missing, or changed file extensions
- **EXIF metadata extraction**: Detects image/video files to extract creation dates from EXIF data
- **Thousands of formats**: Supports video, image, audio, documents, archives, and more
- **Industry standard**: Apache Tika is used by major search engines and content management systems

### Media File Detection for Date Organization

When **"Use Metadata"** is enabled for date organization:

1. **Image Files** (JPEG, PNG, TIFF, RAW formats, etc.)

   - Extracts EXIF DateTimeOriginal, DateTimeDigitized, or DateTime
   - Falls back to file system dates if no EXIF data
   - Supports camera RAW formats: CR2, NEF, ORF, ARW, DNG

2. **Video Files** (MP4, MOV, AVI, MKV, etc.)

   - Extracts QuickTime creation date metadata
   - Handles various video container formats
   - Falls back to file modified date if no metadata

3. **Other Files**
   - Uses file system timestamps (created, modified, accessed)
   - Selected based on "Date Source" option

### Supported Formats

**Video**: MP4, AVI, MOV, MKV, WMV, FLV, WebM, MPEG, 3GP, and many more

**Image**: JPEG, PNG, GIF, BMP, TIFF, WebP, HEIC, camera RAW (CR2, NEF, ORF, ARW, DNG), PSD

**Documents**: PDF, Word, Excel, PowerPoint (for file type detection)

### How It Works

1. **Magic byte analysis**: Reads file header to identify actual content type
2. **MIME type detection**: Returns standard MIME types (e.g., `video/mp4`, `image/jpeg`)
3. **Metadata extraction**: For media files, extracts EXIF/QuickTime metadata
4. **Smart fallback**: If metadata unavailable, uses file system dates

### Benefits for Date Organization

Without metadata: A photo taken in 2020 but downloaded in 2024 would be organized into 2024 folders.

With metadata: The same photo is correctly organized into 2020 folders based on actual capture date.

## Date-Based Organization

Automatically organize files into date-based folder structures. Especially powerful when combined with EXIF metadata extraction from photos and videos.

### Date Sources

Choose which timestamp to use for organizing files:

- **MODIFIED** (default): File modification date - when file was last changed
- **CREATED**: File creation date - when file was created on current filesystem
- **ACCESSED**: File last access date - when file was last opened

For media files with **"Use Metadata"** enabled, EXIF dates take precedence over filesystem dates.

### Folder Patterns

- **YEAR_MONTH** (default): `2024-10/` format
- **YEAR_MONTH_SLASH**: `2024/10/` format
- **YEAR_MONTH_DAY**: `2024-10-31/` format
- **YEAR_MONTH_DAY_SLASH**: `2024/10/31/` format
- **YEAR_ONLY**: `2024/` format
- **YEAR_QUARTER**: `2024-Q4/` format

### GUI Configuration

1. Check **"Date Folders"** to enable
2. Select **Date Source** from dropdown (MODIFIED, CREATED, ACCESSED)
3. Choose **Date Pattern** for folder structure
4. Optional: Check **"Use Metadata"** to extract EXIF dates from photos/videos
5. Optional: Specify separate **Date Target Directory** (defaults to target directory)

### Examples

**Scenario 1: Organize vacation photos by date taken**

```
Settings:
  - Date Folders: ✓
  - Date Source: MODIFIED
  - Date Pattern: YEAR_MONTH
  - Use Metadata: ✓

Result:
target/
├── 2024-07/
│   ├── beach.jpg (taken July 2024)
│   └── sunset.jpg
├── 2024-08/
│   ├── mountain.jpg (taken August 2024)
│   └── hiking.mp4
└── 2024-09/
    └── city.jpg (taken September 2024)
```

**Scenario 2: Organize with full date and preserve structure**

```
Settings:
  - Date Folders: ✓
  - Date Pattern: YEAR_MONTH_DAY_SLASH
  - Keep Structure: ✓

Result:
target/
├── 2024/08/15/
│   └── vacation/
│       └── beach.jpg
└── 2024/09/20/
    └── work/
        └── presentation.pdf
```

**Scenario 3: Organize existing files in-place (source = target)**

```
Settings:
  - Source: /photos/unsorted
  - Target: /photos/unsorted (same directory)
  - Source Duplicate Check: ✓
  - Date Folders: ✓
  - Move Files: ✓

Result: Files reorganized into date folders, duplicates handled, originals removed
```

### EXIF Metadata Priority

When **"Use Metadata"** is checked:

| File Type         | EXIF Date Used                                  | Fallback                              |
| ----------------- | ----------------------------------------------- | ------------------------------------- |
| JPEG/PNG/TIFF     | DateTimeOriginal → DateTimeDigitized → DateTime | File modified date                    |
| RAW (CR2/NEF/etc) | DateTimeOriginal → DateTime                     | File modified date                    |
| Video (MP4/MOV)   | QuickTime creation date                         | File modified date                    |
| Other files       | N/A                                             | File system date based on Date Source |

This ensures photos are organized by when they were **taken**, not when they were **transferred** to your computer.

## File Metadata

The application automatically extracts comprehensive metadata from all files during processing.

### Metadata Captured

- **File size**: Human-readable format (B, KB, MB, GB, TB)
- **Timestamps**: Creation, modification, and access times
- **EXIF data** (photos): DateTimeOriginal, DateTimeDigitized, DateTime
- **Video metadata**: QuickTime creation dates
- **File attributes**: Owner, permissions, hidden flag, read-only status
- **File type**: MIME type via Apache Tika content detection

### Metadata in Processing

Metadata is used throughout the application:

1. **Date Organization**: EXIF dates determine which folder files go into
2. **Logging**: File operations show size and dates for context
3. **Reports**: Excel output includes complete metadata for all files
4. **UI Display**: Statistics show total data processed

### Implementation Details

- `FileMetadata` class: Central metadata container
- `FileMetadataExtractor`: Extracts EXIF, video, and filesystem metadata
- `FileMetadataUtils`: Helper utilities for metadata operations
- Thread-safe extraction during parallel processing
- Graceful fallback when metadata unavailable

### Example Log Output

```
Copied [Image]: vacation_2024.jpg (Size: 3.45 MB | Created: 2024-07-15 14:23:10)
Moved [Video]: birthday.mp4 (Size: 156.32 MB | Created: 2024-08-20 16:45:33)
Duplicate [Image]: sunset.jpg (Size: 2.12 MB | Created: 2024-07-15 18:30:22)
```

## Architecture

### Design Patterns

**Singleton Pattern**: Core data structures use thread-safe singletons for application-wide access:

- `PropertiesObject`: Configuration settings
- `TargetFileHashMapSingleton`: Checksum → filepath mapping
- `SourceFileArraySingleton`: List of source files
- `CopiedFileHashMapSingleton`: Track copied files
- `MatchingFileHashMapSingleton`: Track duplicates

**JavaFX MVC**: Clean separation of UI (FXML) and logic (Controller)

**Multi-threading**:

- Parallel checksum computation using thread pool
- Separate threads for source/target directory scanning
- Thread-safe singleton access with ConcurrentHashMap

### Key Classes

- `SumCompareGUI`: JavaFX application entry point
- `SumCompareController`: Main UI controller with processing logic
- `PropertiesObject`: Configuration singleton with UI population method
- `FileMetadataExtractor`: EXIF and metadata extraction
- `DateFolderOrganizer`: Date-based folder path generation
- `FileUtilsLocal`: Core file operations and checksum computation
- `ReportUtils`: Excel report generation
- `LogAppenderUI`: Real-time log window updates

### Dependencies

- **JavaFX**: Modern GUI framework
- **Apache Commons CLI**: Command-line parsing (legacy)
- **Apache Commons IO**: File operations
- **Apache POI**: Excel report generation
- **Apache Tika**: Content-based file type detection
- **Drew Imaging**: EXIF metadata extraction
- **Logback**: Logging framework
- **Lombok**: Boilerplate reduction
- **XXHash**: High-performance hashing

## Performance

### Benchmarks

On a typical modern system (8-core CPU, SSD):

| File Count            | Algorithm | Time   | Throughput |
| --------------------- | --------- | ------ | ---------- |
| 10,000 files (50 GB)  | XXHASH64  | ~2 min | ~400 MB/s  |
| 10,000 files (50 GB)  | SHA1      | ~8 min | ~100 MB/s  |
| 1,000 videos (100 GB) | XXHASH64  | ~4 min | ~400 MB/s  |

_Results vary based on hardware, file sizes, and I/O subsystem_

### Performance Tips

1. **Use XXHASH64**: 4-8x faster than SHA1, excellent collision resistance
2. **Increase thread count**: Default uses CPU cores, but can increase for I/O-bound workloads
3. **SSD storage**: Dramatically faster than HDD for checksum operations
4. **Disable "Use Metadata"**: Skip EXIF extraction if not needed for date organization
5. **Larger files**: Better parallelization efficiency with fewer large files vs. many small files

## Troubleshooting

### Application Won't Start

- Ensure Java 21 is installed: `java -version`
- Check JavaFX is available (included in Oracle JDK, may need OpenJFX on OpenJDK)

### Slow Performance

- Check available disk I/O (HDD much slower than SSD)
- Reduce thread count if system is overloaded
- Use XXHASH64 instead of SHA1 for speed

### Date Organization Not Working

- Verify "Date Folders" checkbox is enabled
- For photos: Enable "Use Metadata" to use EXIF dates
- Check log output for "No date found for file" warnings

### Duplicates Not Detected

- Ensure same checksum algorithm used for both scans
- Verify files are actually identical (checksums match)
- Check target was fully scanned (watch progress counter)

### Files Not Copying

- Verify "Dry Run" is disabled
- Check file permissions (read source, write target)
- Ensure sufficient disk space in target
- Review log window for error messages

## License

See project documentation for license information.
