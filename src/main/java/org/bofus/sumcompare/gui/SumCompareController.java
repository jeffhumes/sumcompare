package org.bofus.sumcompare.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import org.bofus.sumcompare.localutil.FileTypeDetector;
import org.bofus.sumcompare.localutil.FileUtilsLocal;
import org.bofus.sumcompare.localutil.ReportUtils;
import org.bofus.sumcompare.model.FileMetadata;
import org.bofus.sumcompare.model.PropertiesObject;
import org.bofus.sumcompare.singletons.*;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for the SumCompare GUI.
 * Handles user interactions and coordinates the file comparison process.
 */
@Slf4j
public class SumCompareController {

    @FXML
    private TextField sourceTextField;
    @FXML
    private TextField targetTextField;
    @FXML
    private Button targetBrowseButton;
    @FXML
    private ComboBox<String> algorithmComboBox;
    @FXML
    private Spinner<Integer> threadCountSpinner;
    @FXML
    private CheckBox dryRunCheckBox;
    @FXML
    private CheckBox keepStructureCheckBox;
    @FXML
    private CheckBox backupCheckBox;
    @FXML
    private CheckBox preserveDateCheckBox;
    @FXML
    private CheckBox createReportCheckBox;
    @FXML
    private Button startButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;
    @FXML
    private TextArea logTextArea;
    @FXML
    private Label scannedCountLabel;
    @FXML
    private Label copiedCountLabel;
    @FXML
    private Label duplicatesCountLabel;
    @FXML
    private Label elapsedTimeLabel;
    @FXML
    private CheckBox sourceDuplicateCheckBox;
    @FXML
    private CheckBox dateFoldersCheckBox;
    @FXML
    private ComboBox<String> dateSourceComboBox;
    @FXML
    private ComboBox<String> datePatternComboBox;

    private Task<Void> currentTask;
    private Task<Void> timerTask;
    private Instant startTime;

    @FXML
    public void initialize() {
        // Populate algorithm choices
        algorithmComboBox.getItems().addAll("XXHASH64", "XXHASH32", "SHA1", "MD5");

        // Set default algorithm
        algorithmComboBox.getSelectionModel().select("XXHASH64");

        // Configure thread count spinner
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, availableProcessors * 2, availableProcessors);
        threadCountSpinner.setValueFactory(valueFactory);

        // Populate date source choices with descriptions
        if (dateSourceComboBox != null) {
            dateSourceComboBox.getItems().addAll(
                    "MODIFIED (last changed)",
                    "CREATED (when created)",
                    "ACCESSED (last opened)");
            dateSourceComboBox.getSelectionModel().select("MODIFIED (last changed)");
            dateSourceComboBox.setDisable(true); // Disabled by default
        }

        // Populate date pattern choices with examples
        if (datePatternComboBox != null) {
            datePatternComboBox.getItems().addAll(
                    "YEAR_MONTH (2025-11)",
                    "YEAR_MONTH_SLASH (2025/11)",
                    "YEAR_MONTH_DAY (2025-11-03)",
                    "YEAR_MONTH_DAY_SLASH (2025/11/03)",
                    "YEAR_ONLY (2025)",
                    "YEAR_QUARTER (2025-Q4)");
            datePatternComboBox.getSelectionModel().select("YEAR_MONTH (2025-11)");
            datePatternComboBox.setDisable(true); // Disabled by default
        }

        // Add listener to enable/disable date options when checkbox is toggled
        if (dateFoldersCheckBox != null) {
            dateFoldersCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (dateSourceComboBox != null) {
                    dateSourceComboBox.setDisable(!newValue);
                }
                if (datePatternComboBox != null) {
                    datePatternComboBox.setDisable(!newValue);
                }
            });
        }

        // Initialize progress bar to 0
        progressBar.setProgress(0.0);

        // Clear statistics
        resetStatistics();

        sourceDuplicateCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            targetTextField.setDisable(newValue);
            targetBrowseButton.setDisable(newValue);

            if (newValue) {
                targetTextField.clear();
                appendLog("Source duplicate check mode: Target directory disabled");
            }
        });

        log.info("SumCompareController initialized");

    }

    @FXML
    private void onSourceBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Source Directory");

        if (sourceTextField.getText() != null && !sourceTextField.getText().isEmpty()) {
            File current = new File(sourceTextField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (selected != null) {
            sourceTextField.setText(selected.getAbsolutePath());
            appendLog("Source directory selected: " + selected.getAbsolutePath());
        }
    }

    @FXML
    private void onTargetBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Target Directory");

        if (targetTextField.getText() != null && !targetTextField.getText().isEmpty()) {
            File current = new File(targetTextField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (selected != null) {
            targetTextField.setText(selected.getAbsolutePath());
            appendLog("Target directory selected: " + selected.getAbsolutePath());
        }
    }

    @FXML
    private void onStart() {
        // Validate inputs
        if (sourceTextField.getText() == null || sourceTextField.getText().trim().isEmpty()) {
            showError("Please select a source directory");
            return;
        }

        if (!sourceDuplicateCheckBox.isSelected()) {
            if (targetTextField.getText() == null || targetTextField.getText().trim().isEmpty()) {
                showError("Please select a target directory");
                return;
            }
        }

        File sourceDir = new File(sourceTextField.getText());
        File targetDir = null;

        // NOTE: if the source duplicate check is enabled, the target directory is the
        // same as the source
        if (!sourceDuplicateCheckBox.isSelected()) {
            targetDir = new File(targetTextField.getText());
        } else {
            targetDir = new File(sourceTextField.getText());
        }

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            showError("Source directory does not exist or is not a directory");
            return;
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            showError("Target directory does not exist or is not a directory");
            return;
        }

        // Clear previous run data
        clearSingletons();
        resetStatistics();
        logTextArea.clear();

        // Start the comparison task
        startComparisonTask();
    }

    @FXML
    private void onCancel() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
            appendLog("\n=== OPERATION CANCELLED BY USER ===");
            statusLabel.setText("Cancelled");
            enableControls(true);
        }
    }

    @FXML
    private void onViewLog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logviewer.fxml"));
            Parent root = loader.load();

            Stage logStage = new Stage();
            logStage.setTitle("SumCompare - Application Log");

            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            logStage.setScene(scene);

            // Handle window close to stop the tail thread
            LogViewerController controller = loader.getController();
            logStage.setOnCloseRequest(event -> controller.shutdown());

            logStage.show();
            log.info("Log viewer window opened");
        } catch (IOException e) {
            log.error("Failed to open log viewer", e);
            showError("Failed to open log viewer: " + e.getMessage());
        }
    }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help - SumCompare");
        alert.setHeaderText("How to use SumCompare");
        alert.setContentText(
                "1. Select SOURCE directory (files to copy from)\n" +
                        "2. Select TARGET directory (destination)\n" +
                        "3. Choose checksum algorithm:\n" +
                        "   - XXHASH64: Fastest (recommended)\n" +
                        "   - XXHASH32: Fast, smaller hash\n" +
                        "   - SHA1: Cryptographically secure\n" +
                        "   - MD5: Legacy support\n\n" +
                        "4. Configure options:\n" +
                        "   - Dry Run: Preview without copying\n" +
                        "   - Keep Structure: Preserve directories\n" +
                        "   - Backup Source: Create zip backup\n" +
                        "   - Preserve Dates: Keep file timestamps\n" +
                        "   - Create Report: Generate Excel output\n" +
                        "   - Date Folders: Organize by file dates\n" +
                        "     * Date Source: MODIFIED/CREATED/ACCESSED\n" +
                        "     * Pattern: YEAR_MONTH, YEAR_MONTH_DAY, etc.\n\n" +
                        "5. Click START to begin comparison\n\n" +
                        "The tool will skip files that already exist in target.");
        alert.showAndWait();
    }

    private void startComparisonTask() {
        startTime = Instant.now();
        enableControls(false);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Starting comparison...");

        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Create properties object
                    PropertiesObject props = new PropertiesObject();
                    props.setSourceLocation(sourceTextField.getText());
                    props.setTargetLocation(targetTextField.getText());
                    props.setDryRun(dryRunCheckBox.isSelected());
                    props.setKeepSourceStructure(keepStructureCheckBox.isSelected());
                    props.setBackupFirst(backupCheckBox.isSelected());
                    props.setPreserveFileDate(preserveDateCheckBox.isSelected());
                    props.setCreateOutputFile(createReportCheckBox.isSelected());

                    // Set date-based folder organization
                    if (dateFoldersCheckBox != null && dateFoldersCheckBox.isSelected()) {
                        props.setOrganizeDateFolders(true);

                        // Set date source
                        if (dateSourceComboBox != null) {
                            String dateSourceStr = dateSourceComboBox.getValue();
                            // Extract enum name before the space (e.g., "MODIFIED (last changed)" ->
                            // "MODIFIED")
                            String enumName = dateSourceStr.contains(" ")
                                    ? dateSourceStr.substring(0, dateSourceStr.indexOf(" "))
                                    : dateSourceStr;
                            props.setDateSource(org.bofus.sumcompare.localutil.DateFolderOrganizer.DateSource
                                    .valueOf(enumName));
                        } else {
                            props.setDateSource(org.bofus.sumcompare.localutil.DateFolderOrganizer.DateSource.MODIFIED);
                        }

                        // Set date pattern
                        if (datePatternComboBox != null) {
                            String datePatternStr = datePatternComboBox.getValue();
                            // Extract enum name before the space (e.g., "YEAR_MONTH (2025-11)" ->
                            // "YEAR_MONTH")
                            String enumName = datePatternStr.contains(" ")
                                    ? datePatternStr.substring(0, datePatternStr.indexOf(" "))
                                    : datePatternStr;
                            props.setDatePattern(org.bofus.sumcompare.localutil.DateFolderOrganizer.DatePattern
                                    .valueOf(enumName));
                        } else {
                            props.setDatePattern(
                                    org.bofus.sumcompare.localutil.DateFolderOrganizer.DatePattern.YEAR_MONTH);
                        }

                        String orgDescription = org.bofus.sumcompare.localutil.DateFolderOrganizer
                                .getOrganizationDescription(
                                        props.getDateSource(), props.getDatePattern());
                        updateMessage("Date-based organization: " + orgDescription);
                    } else {
                        props.setOrganizeDateFolders(false);
                    }

                    // Set digest type
                    String algorithm = algorithmComboBox.getValue();
                    MessageDigest digest = FileUtilsLocal.SetDigestType(algorithm);
                    props.setDigestType(digest);

                    updateMessage("Using algorithm: " + algorithm);

                    // Step 1: Backup if requested
                    if (props.isBackupFirst()) {
                        updateMessage("Creating backup of source directory...");
                        FileUtilsLocal.zipDirectory(props);
                        updateMessage("Backup completed");
                    }

                    // Step 2 & 4: Scan target and source directories in parallel
                    updateMessage("Scanning directories in parallel...");

                    Thread targetScanThread = new Thread(() -> {
                        try {
                            FileUtilsLocal.getTargetDirectoryContentsArray(props.getTargetLocation());
                            int targetCount = TargetFileArraySingleton.getInstance().getArray().size();
                            updateMessage("Found " + targetCount + " files in target");

                            // Step 3: Compute target checksums
                            updateMessage("Computing target checksums...");
                            FileUtilsLocal.createTargetFileChecksumMap(
                                    TargetFileArraySingleton.getInstance(),
                                    props.getDigestType());
                            updateMessage("Target checksums completed");
                        } catch (Exception e) {
                            log.error("Error scanning target directory", e);
                            updateMessage("ERROR scanning target: " + e.getMessage());
                        }
                    });

                    Thread sourceScanThread = new Thread(() -> {
                        try {
                            FileUtilsLocal.getSourceDirectoryContentsArray(props.getSourceLocation());
                            int sourceCount = SourceFileArraySingleton.getInstance().getArray().size();
                            updateMessage("Found " + sourceCount + " files in source");
                            Platform.runLater(() -> updateScannedCount(sourceCount));
                        } catch (Exception e) {
                            log.error("Error scanning source directory", e);
                            updateMessage("ERROR scanning source: " + e.getMessage());
                        }
                    });

                    // Start both threads
                    targetScanThread.start();
                    sourceScanThread.start();

                    // Wait for both to complete
                    targetScanThread.join();
                    sourceScanThread.join();

                    updateMessage("Directory scanning completed");

                    // Step 5: Process source files
                    updateMessage("Processing source files...");
                    processSourceFiles(props);

                    // Step 6: Generate report if requested
                    if (props.isCreateOutputFile()) {
                        updateMessage("Generating Excel report...");
                        ReportUtils.createOutputExcel();
                        updateMessage("Report created: Copy_Output.xlsx");
                    }

                    // Update final statistics
                    int copied = CopiedFileHashMapSingleton.getInstance().getMap().size();
                    int duplicates = MatchingFileHashMapSingleton.getInstance().getMap().size();
                    updateCopiedCount(copied);
                    updateDuplicatesCount(duplicates);

                    updateMessage("\n=== COMPLETED SUCCESSFULLY ===");
                    updateMessage("Files copied: " + copied);
                    updateMessage("Duplicates found: " + duplicates);

                    Platform.runLater(() -> {
                        progressBar.setProgress(1.0);
                        statusLabel.setText("Completed");
                    });

                } catch (Exception e) {
                    log.error("Error during comparison", e);
                    updateMessage("ERROR: " + e.getMessage());
                    throw e;
                }

                return null;
            }

            @Override
            protected void succeeded() {
                stopElapsedTimeUpdater();
                enableControls(true);
            }

            @Override
            protected void failed() {
                stopElapsedTimeUpdater();
                enableControls(true);
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    statusLabel.setText("Failed");
                    showError("Comparison failed: " + getException().getMessage());
                });
            }

            @Override
            protected void cancelled() {
                stopElapsedTimeUpdater();
                enableControls(true);
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    statusLabel.setText("Cancelled");
                });
            }
        };

        // Bind message property to status and log
        currentTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> {
                if (newMsg != null && !newMsg.isEmpty()) {
                    statusLabel.setText(newMsg);
                    appendLog(newMsg);
                }
            });
        });

        // Start elapsed time updater
        startElapsedTimeUpdater();

        // Run task in background thread
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void processSourceFiles(PropertiesObject props) throws Exception {
        int threadCount = threadCountSpinner.getValue();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<String> sourceFiles = SourceFileArraySingleton.getInstance().getArray();
        CountDownLatch latch = new CountDownLatch(sourceFiles.size());

        for (String sourceFile : sourceFiles) {
            executor.submit(() -> {
                try {
                    if (currentTask.isCancelled()) {
                        return;
                    }

                    File thisSourceFile = new File(sourceFile);

                    // Capture file metadata
                    FileMetadata metadata = FileMetadata.fromFile(thisSourceFile);

                    // Detect file type
                    String fileTypeDesc = FileTypeDetector.getFileTypeDescription(thisSourceFile);

                    MessageDigest threadDigest = (MessageDigest) props.getDigestType().clone();
                    String checksum = FileUtilsLocal.getFileChecksum(threadDigest, thisSourceFile);

                    if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(checksum)) {
                        String existingFile = TargetFileHashMapSingleton.getInstance().getMap().get(checksum);
                        String sourceFileName = FileUtilsLocal.getFileName(sourceFile);
                        String targetFileName = FileUtilsLocal.getFileName(existingFile);

                        if (sourceFileName.equals(targetFileName)) {
                            MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                        } else {
                            String logMsg = String.format("Duplicate [%s]: %s -> %s (%s)",
                                    fileTypeDesc, sourceFileName, existingFile, metadata.getSummary());
                            Platform.runLater(() -> appendLog(logMsg));
                            MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                        }
                    } else {
                        // File needs to be copied
                        String targetPath = calculateTargetPath(sourceFile, props);
                        CopiedFileHashMapSingleton.getInstance().addToMap(sourceFile, targetPath);

                        if (props.isDryRun()) {
                            String fileName = thisSourceFile.getName();
                            String logMsg = String.format("Would copy [%s]: %s (%s)",
                                    fileTypeDesc, fileName, metadata.getSummary());
                            Platform.runLater(() -> appendLog(logMsg));
                        } else {
                            File targetFile = new File(targetPath);

                            // Ensure date-based folder exists before copying
                            if (props.isOrganizeDateFolders()) {
                                org.bofus.sumcompare.localutil.DateFolderOrganizer.ensureDateFolderExists(targetFile);
                            }

                            org.apache.commons.io.FileUtils.copyFile(thisSourceFile, targetFile,
                                    props.isPreserveFileDate());
                            String fileName = thisSourceFile.getName();
                            String logMsg = String.format("Copied [%s]: %s (%s)",
                                    fileTypeDesc, fileName, metadata.getSummary());
                            Platform.runLater(() -> appendLog(logMsg));
                        }

                        updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                    }

                    updateDuplicatesCount(MatchingFileHashMapSingleton.getInstance().getMap().size());
                } catch (Exception e) {
                    log.error("Error processing file: " + sourceFile, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        executor.shutdown();
        latch.await();
    }

    private String calculateTargetPath(String sourceFile, PropertiesObject props) {
        String targetFileName = FileUtilsLocal.getFileName(sourceFile);

        // Use date-based folder organization if enabled
        if (props.isOrganizeDateFolders()) {
            try {
                File thisSourceFile = new File(sourceFile);
                File baseTargetDir = new File(props.getTargetLocation());
                File targetFile = org.bofus.sumcompare.localutil.DateFolderOrganizer.generateDateBasedTargetPath(
                        thisSourceFile,
                        baseTargetDir,
                        props.getDateSource(),
                        props.getDatePattern(),
                        props.isKeepSourceStructure());
                return targetFile.getAbsolutePath();
            } catch (Exception e) {
                log.error("Error generating date-based path for {}, falling back to standard path", sourceFile, e);
                // Fallback to standard logic
                return props.getTargetLocation() + File.separator + targetFileName;
            }
        } else if (props.isKeepSourceStructure()) {
            String sourceBasePath = sourceFile.replace(props.getSourceLocation(), "");
            String tempPath = org.apache.commons.io.FilenameUtils.getPath(sourceBasePath);
            return props.getTargetLocation() + File.separator + tempPath + File.separator + targetFileName;
        } else {
            return props.getTargetLocation() + File.separator + targetFileName;
        }
    }

    private void startElapsedTimeUpdater() {
        timerTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (!isCancelled()) {
                    Thread.sleep(1000);
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    String timeStr = String.format("%02d:%02d:%02d",
                            elapsed.toHours(),
                            elapsed.toMinutesPart(),
                            elapsed.toSecondsPart());
                    Platform.runLater(() -> elapsedTimeLabel.setText(timeStr));
                }
                return null;
            }
        };

        Thread thread = new Thread(timerTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void stopElapsedTimeUpdater() {
        if (timerTask != null && timerTask.isRunning()) {
            timerTask.cancel();
        }
    }

    private void enableControls(boolean enable) {
        Platform.runLater(() -> {
            sourceTextField.setDisable(!enable);
            targetTextField.setDisable(!enable);
            algorithmComboBox.setDisable(!enable);
            threadCountSpinner.setDisable(!enable);
            dryRunCheckBox.setDisable(!enable);
            keepStructureCheckBox.setDisable(!enable);
            backupCheckBox.setDisable(!enable);
            preserveDateCheckBox.setDisable(!enable);
            createReportCheckBox.setDisable(!enable);
            startButton.setDisable(!enable);
            cancelButton.setDisable(enable);
        });
    }

    private void appendLog(String message) {
        Platform.runLater(() -> {
            logTextArea.appendText(message + "\n");
        });
    }

    private void updateScannedCount(int count) {
        Platform.runLater(() -> scannedCountLabel.setText(String.valueOf(count)));
    }

    private void updateCopiedCount(int count) {
        Platform.runLater(() -> copiedCountLabel.setText(String.valueOf(count)));
    }

    private void updateDuplicatesCount(int count) {
        Platform.runLater(() -> duplicatesCountLabel.setText(String.valueOf(count)));
    }

    private void resetStatistics() {
        scannedCountLabel.setText("0");
        copiedCountLabel.setText("0");
        duplicatesCountLabel.setText("0");
        elapsedTimeLabel.setText("00:00:00");
    }

    private void clearSingletons() {
        try {
            SourceFileArraySingleton.getInstance().getArray().clear();
            TargetFileArraySingleton.getInstance().getArray().clear();
            SourceFileHashMapSingleton.getInstance().getMap().clear();
            TargetFileHashMapSingleton.getInstance().getMap().clear();
            CopiedFileHashMapSingleton.getInstance().getMap().clear();
            MatchingFileHashMapSingleton.getInstance().getMap().clear();
            ExistingTargetFileObjectArraySingleton.getInstance().getArray().clear();
        } catch (Exception e) {
            log.error("Error clearing singletons", e);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) sourceTextField.getScene().getWindow();
    }
}
