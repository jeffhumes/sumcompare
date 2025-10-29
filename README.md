# sumcompare

Intelligent file copying tool with checksum-based deduplication for Java.

## Features

- **Checksum-based deduplication**: Compares files using cryptographic hashes to avoid copying duplicates
- **Multiple hash algorithms**: Supports MD5, SHA1, XXHash32, and XXHash64
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

## License

See project documentation for license information.
