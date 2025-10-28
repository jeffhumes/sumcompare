# sumcompare - Copilot Instructions

## Project Overview
`sumcompare` is a Java CLI tool for intelligent file copying that uses checksum-based deduplication. It compares source and target directories, copies only unique files, and generates Excel reports of duplicates and copy operations.

## Architecture

### Core Flow
1. **Initialization** (`Main.java`): Parse CLI options → populate `PropertiesObject` → optionally backup source
2. **Target Analysis**: Recursively scan target directory → compute checksums → store in `TargetFileHashMapSingleton` (checksum → filepath)
3. **Source Processing**: Scan source directory → for each file, compute checksum → check if exists in target map
4. **Copy Decision**: If checksum exists AND filenames match → skip (duplicate). If checksum exists but different filename → log to `MatchingFileHashMapSingleton`. If checksum doesn't exist → copy to `CopiedFileHashMapSingleton`
5. **Reporting** (`ReportUtils.java`): Generate Excel with 3 sheets: copied files, target duplicates, source duplicates

### Singleton Pattern Usage
All data is stored in singleton collections (not thread-safe, single-threaded design):
- `TargetFileHashMapSingleton`: Map<checksum, filepath> for all target files
- `SourceFileArraySingleton`: List of all source file paths
- `CopiedFileHashMapSingleton`: Map<source, target> of files copied
- `MatchingFileHashMapSingleton`: Map<source, target> of duplicate files not copied
- `ExistingTargetFileObjectArraySingleton`: List of duplicate files already in target

Access pattern: `SometonSingleton.getInstance().getMap()` or `.getArray()` then add/get directly.

## Key Command-Line Options
- `-s <path>` / `--source`: Source directory (required)
- `-t <path>` / `--target`: Target directory (required)
- `-z <type>` / `--chksumtype`: Checksum algorithm - `SHA1` or `MD5` (required)
- `-d` / `--dry-run`: Preview mode - logs operations without copying
- `-b` / `--backup-source-first`: Creates zip backup of source in temp directory before processing
- `-k` / `--keep-source-structure`: Preserve source directory structure in target (default: flatten)
- `-o` / `--create-output-file`: Generate Excel report (`Copy_Output.xlsx`)
- `-p` / `--preserve-file-date`: Maintain file timestamps on copy
- `-y` / `--i-agree`: Skip interactive acceptance prompt

## Build & Run
```bash
# Build executable JAR with dependencies
mvn clean package

# Run example
java -jar target/sumcompare.jar -s /path/to/source -t /path/to/target -z SHA1 -d -o -y
```

The `maven-shade-plugin` bundles all dependencies into a single executable JAR with `Main` as entrypoint.

## Code Conventions
- **Logging**: Use SLF4J with Logback. Debug for detailed flow, Info for user-visible operations, Warn/Error for issues
- **File Operations**: Use Apache Commons IO (`FileUtils`, `FilenameUtils`) for cross-platform path handling
- **Error Handling**: System.exit with specific codes (94: directory not found, 98: invalid digest type)
- **Checksum Logic** (`FileUtilsLocal.getFileChecksum`): Read 1KB chunks, update MessageDigest, convert to hex string
- **Properties Object**: `PropertiesObject` is the central configuration holder passed between methods
- **Recursive Directory Scanning**: Methods like `getSourceDirectoryContentsArray` recursively traverse and populate singletons

## Dependencies
- **Apache Commons CLI**: Command-line parsing
- **Apache Commons IO**: File operations
- **Apache POI**: Excel report generation (`.xlsx` format)
- **SLF4J + Logback**: Logging framework
- **JSch, JavaMail, BoneCP**: Present but unused (legacy dependencies)

## Known Patterns
- No test suite exists - manual testing required
- Java 8 source/target (see `pom.xml` compiler config)
- Reports written to current directory as `Copy_Output.xlsx`
- Source backups stored in system temp directory as `Source_Backup.zip`
