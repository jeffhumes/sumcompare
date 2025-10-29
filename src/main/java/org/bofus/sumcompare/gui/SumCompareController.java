package org.bofus.sumcompare.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.bofus.sumcompare.localutil.FileUtilsLocal;
import org.bofus.sumcompare.localutil.ReportUtils;
import org.bofus.sumcompare.model.PropertiesObject;
import org.bofus.sumcompare.singletons.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

/**
 * Controller for the SumCompare GUI.
 * Handles user interactions and coordinates the file comparison process.
 */
public class SumCompareController {
    private static final Logger logger = LoggerFactory.getLogger(SumCompareController.class);

    @FXML
    private TextField sourceTextField;
    @FXML
    private TextField targetTextField;
    @FXML
    private ComboBox<String> algorithmComboBox;
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

    private Task<Void> currentTask;
    private Instant startTime;

    @FXML
    public void initialize() {
        // Populate algorithm choices
        algorithmComboBox.getItems().addAll("XXHASH64", "XXHASH32", "SHA1", "MD5");

        // Set default algorithm
        algorithmComboBox.getSelectionModel().select("XXHASH64");

        // Clear statistics
        resetStatistics();

        logger.info("SumCompareController initialized");
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

        if (targetTextField.getText() == null || targetTextField.getText().trim().isEmpty()) {
            showError("Please select a target directory");
            return;
        }

        File sourceDir = new File(sourceTextField.getText());
        File targetDir = new File(targetTextField.getText());

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
                        "   - Create Report: Generate Excel output\n\n" +
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

                    // Step 2: Scan target directory
                    updateMessage("Scanning target directory...");
                    FileUtilsLocal.getTargetDirectoryContentsArray(props.getTargetLocation());
                    int targetCount = TargetFileArraySingleton.getInstance().getArray().size();
                    updateMessage("Found " + targetCount + " files in target");

                    // Step 3: Compute target checksums
                    updateMessage("Computing target checksums...");
                    FileUtilsLocal.createTargetFileChecksumMap(
                            TargetFileArraySingleton.getInstance(),
                            props.getDigestType());
                    updateMessage("Target checksums completed");

                    // Step 4: Scan source directory
                    updateMessage("Scanning source directory...");
                    FileUtilsLocal.getSourceDirectoryContentsArray(props.getSourceLocation());
                    int sourceCount = SourceFileArraySingleton.getInstance().getArray().size();
                    updateMessage("Found " + sourceCount + " files in source");
                    updateScannedCount(sourceCount);

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
                    logger.error("Error during comparison", e);
                    updateMessage("ERROR: " + e.getMessage());
                    throw e;
                }

                return null;
            }

            @Override
            protected void succeeded() {
                enableControls(true);
            }

            @Override
            protected void failed() {
                enableControls(true);
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    statusLabel.setText("Failed");
                    showError("Comparison failed: " + getException().getMessage());
                });
            }

            @Override
            protected void cancelled() {
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
        for (String sourceFile : SourceFileArraySingleton.getInstance().getArray()) {
            if (currentTask.isCancelled()) {
                break;
            }

            File thisSourceFile = new File(sourceFile);
            MessageDigest threadDigest = (MessageDigest) props.getDigestType().clone();
            String checksum = FileUtilsLocal.getFileChecksum(threadDigest, thisSourceFile);

            if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(checksum)) {
                String existingFile = TargetFileHashMapSingleton.getInstance().getMap().get(checksum);
                String sourceFileName = FileUtilsLocal.getFileName(sourceFile);
                String targetFileName = FileUtilsLocal.getFileName(existingFile);

                if (sourceFileName.equals(targetFileName)) {
                    MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                } else {
                    Platform.runLater(() -> appendLog("Duplicate: " + sourceFile + " -> " + existingFile));
                    MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                }
            } else {
                // File needs to be copied
                String targetPath = calculateTargetPath(sourceFile, props);
                CopiedFileHashMapSingleton.getInstance().addToMap(sourceFile, targetPath);

                if (props.isDryRun()) {
                    String fileName = thisSourceFile.getName();
                    Platform.runLater(() -> appendLog("Would copy: " + fileName));
                } else {
                    File targetFile = new File(targetPath);
                    org.apache.commons.io.FileUtils.copyFile(thisSourceFile, targetFile, props.isPreserveFileDate());
                    String fileName = thisSourceFile.getName();
                    Platform.runLater(() -> appendLog("Copied: " + fileName));
                }

                updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
            }

            updateDuplicatesCount(MatchingFileHashMapSingleton.getInstance().getMap().size());
        }
    }

    private String calculateTargetPath(String sourceFile, PropertiesObject props) {
        String targetFileName = FileUtilsLocal.getFileName(sourceFile);

        if (props.isKeepSourceStructure()) {
            String sourceBasePath = sourceFile.replace(props.getSourceLocation(), "");
            String tempPath = org.apache.commons.io.FilenameUtils.getPath(sourceBasePath);
            return props.getTargetLocation() + File.separator + tempPath + File.separator + targetFileName;
        } else {
            return props.getTargetLocation() + File.separator + targetFileName;
        }
    }

    private void startElapsedTimeUpdater() {
        Task<Void> timeTask = new Task<Void>() {
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

        Thread thread = new Thread(timeTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void enableControls(boolean enable) {
        Platform.runLater(() -> {
            sourceTextField.setDisable(!enable);
            targetTextField.setDisable(!enable);
            algorithmComboBox.setDisable(!enable);
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
            logger.error("Error clearing singletons", e);
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
