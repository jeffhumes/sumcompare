# sumcompare

Intelligent file copying tool with checksum-based deduplication for Java.

## Features

- **Checksum-based deduplication**: Compares files using cryptographic hashes to avoid copying duplicates
- **Multiple hash algorithms**: Supports MD5, SHA1, XXHash32, and XXHash64
- **File type detection**: Automatically identifies video and image files for enhanced logging
- **File metadata retrieval**: Captures and displays file size, timestamps, owner, and permissions
- **Dry-run mode**: Preview operations without making changes
- **Excel reports**: Generate detailed reports of copied files and duplicates
- **Structure preservation**: Option to maintain source directory structure
- **Parallel processing**: Multi-threaded checksum computation for faster operation

## Usage

```bash
java -jar target/sumcompare.jar [OPTIONS]
```

### Required Options

- `-s <path>` or `--source`: Source directory to copy from
- `-t <path>` or `--target`: Target directory to copy to
- `-z <type>` or `--chksumtype`: Checksum algorithm (MD5, SHA1, XXHASH32, XXHASH64)

### Optional Flags

- `-b` or `--backup-source-first`: Create zip backup of source before processing
- `-d` or `--dry-run`: Preview mode - log operations without copying files
- `-k` or `--keep-source-structure`: Preserve source directory structure in target
- `-o` or `--create-output-file`: Generate Excel report (`Copy_Output.xlsx`)
- `-p` or `--preserve-file-date`: Maintain file timestamps when copying
- `-df` or `--date-folders`: Organize files into date-based folders
- `-ds` or `--date-source`: Date source (CREATED, MODIFIED, ACCESSED) - default: MODIFIED
- `-dp` or `--date-pattern`: Folder pattern (see Date-Based Organization below)
- `-y` or `--i-agree`: Skip interactive acceptance prompt
- `-h` or `--help`: Show help screen

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

## Examples

### Basic dry-run with XXHash64

```bash
java -jar target/sumcompare.jar -d -y -z XXHASH64 \
  -s /path/to/source -t /path/to/target
```

### Copy files with report and structure preservation

```bash
java -jar target/sumcompare.jar -y -z XXHASH64 -k -o \
  -s /path/to/source -t /path/to/target
```

### Full backup and copy with timestamp preservation

```bash
java -jar target/sumcompare.jar -b -p -o -z SHA1 \
  -s /path/to/source -t /path/to/target
```

## Build

```bash
mvn clean package
```

This creates `target/sumcompare.jar` with all dependencies bundled.

## Requirements

- Java 21 (LTS)
- Maven 3.x (for building)

## How It Works

1. **Target Analysis**: Scans target directory and computes checksums for all files
2. **Source Scanning**: Scans source directory files
3. **Deduplication**: For each source file:
   - Computes checksum
   - Checks if checksum exists in target
   - If exists with same filename → skip (duplicate)
   - If exists with different filename → log as duplicate
   - If doesn't exist → copy to target
4. **Reporting**: Generates Excel report with three sheets:
   - Copied files
   - Target duplicates
   - Source duplicates

## File Type Detection

The tool automatically detects whether files are videos or images:

### Supported Video Formats

MP4, AVI, MOV, MKV, WMV, FLV, WebM, M4V, MPG, MPEG, 3GP, M2TS, MTS, TS, VOB, OGV, MXF, RM, RMVB, ASF, DivX, F4V, M2V

### Supported Image Formats

JPG, JPEG, PNG, GIF, BMP, TIFF, WebP, SVG, ICO, HEIC, HEIF, RAW, CR2, NEF, ORF, ARW, DNG, PSD, AI, EPS, XCF, EXR, HDR

### Detection Methods

1. **Extension-based**: Fast detection using file extensions
2. **MIME type**: Fallback to content-based detection when extension is unclear

### Log Output Enhancement

File type information is included in log messages:

```
Would Copy File [Video]: /path/to/video.mp4 to /target/video.mp4
Would Copy File [Image]: /path/to/photo.jpg to /target/photo.jpg
Duplicate [Video]: clip.mov -> existing_clip.mov
```

## Date-Based Organization

The tool can automatically organize copied files into folders based on file dates:

### Date Sources

- **MODIFIED** (default): Use file modification date
- **CREATED**: Use file creation date
- **ACCESSED**: Use file last access date

### Folder Patterns

- **YEAR_MONTH** (default): `2024-10/` format
- **YEAR_MONTH_SLASH**: `2024/10/` format
- **YEAR_MONTH_DAY**: `2024-10-31/` format
- **YEAR_MONTH_DAY_SLASH**: `2024/10/31/` format
- **YEAR_ONLY**: `2024/` format
- **YEAR_QUARTER**: `2024-Q4/` format

### Examples

Organize files by year and month (modification date):

```bash
java -jar target/sumcompare.jar -df -y -z XXHASH64 \
  -s /path/to/source -t /path/to/target
```

Organize by full date with creation date:

```bash
java -jar target/sumcompare.jar -df -ds CREATED -dp YEAR_MONTH_DAY -y -z XXHASH64 \
  -s /path/to/source -t /path/to/target
```

Result structure:

```
target/
├── 2024-01/
│   ├── file1.jpg
│   └── file2.mp4
├── 2024-02/
│   └── file3.jpg
└── 2024-03/
    └── file4.mp4
```

Combine with `-k` to preserve source structure within date folders:

```bash
java -jar target/sumcompare.jar -df -k -y -z XXHASH64 \
  -s /path/to/source -t /path/to/target
```

Result structure:

```
target/
├── 2024-01/
│   └── vacation/
│       └── beach.jpg
└── 2024-02/
    └── work/
        └── presentation.pdf
```

## File Metadata

The tool automatically captures and displays metadata for all files being checked:

### Captured Metadata

- **File size**: Displayed in human-readable format (B, KB, MB, GB)
- **Creation time**: When the file was created (if available)
- **Last modified time**: When the file was last changed
- **Last access time**: When the file was last accessed
- **Owner**: File owner username
- **Permissions**: Read-only status and hidden flag
- **File attributes**: Regular file, directory, or symbolic link

### Metadata in Logs

Metadata is displayed inline with file operations:

```
Copying [Video]: vacation.mp4 (Size: 156.32 MB | Modified: 2024-08-15 14:23:10)
Would copy [Image]: photo.jpg (Size: 2.45 MB | Modified: 2024-09-20 09:15:33)
Duplicate [Video]: clip.mov -> existing_clip.mov (Size: 89.12 MB | Modified: 2024-07-10 18:42:05)
```

### Metadata API

The `FileMetadata` class provides programmatic access to file attributes:

```java
FileMetadata metadata = FileMetadata.fromFile(file);
String size = metadata.getFormattedSize();        // "156.32 MB"
String modified = metadata.getLastModifiedTime(); // "2024-08-15 14:23:10"
String summary = metadata.getSummary();           // Complete summary string
```

Additional utilities are available in `FileMetadataUtils`:

```java
// Log detailed metadata
FileMetadataUtils.logDetailedMetadata(file);

// Compare metadata between files
boolean same = FileMetadataUtils.haveSameMetadata(file1, file2);

// Check recent modifications
boolean recent = FileMetadataUtils.wasModifiedWithinHours(file, 24);
```

## License

See project documentation for license information.
