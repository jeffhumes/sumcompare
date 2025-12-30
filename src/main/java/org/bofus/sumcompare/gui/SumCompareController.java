package org.bofus.sumcompare.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import org.bofus.sumcompare.localutil.FileMetadataExtractor;
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

import static org.bofus.sumcompare.gui.LogAppenderUI.appendtoUiLog;

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
    private CheckBox writeLogToFileCheckBox;
    @FXML
    private TextField logDirectoryField;
    @FXML
    private Button logDirectoryBrowseButton;
    @FXML
    private Button startButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;

    // Log window components (not in FXML - created dynamically)
    private Stage logWindowStage;
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
    @FXML
    private TextField dateTargetField;
    @FXML
    private Button dateTargetBrowseButton;
    @FXML
    private CheckBox useMetadataCheckBox;
    @FXML
    private CheckBox renameDuplicatesCheckBox;
    @FXML
    private TextField duplicatePrefixField;
    @FXML
    private CheckBox deleteEmptyFoldersCheckBox;
    @FXML
    private CheckBox moveFilesCheckBox;
    @FXML
    private CheckBox permanentlyDeleteCheckBox;

    private Task<Void> currentTask;
    private Task<Void> timerTask;
    private Instant startTime;

    @FXML
    public void initialize() {
        progressBar.setProgress(0.001);

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
        if (null != dateSourceComboBox) {
            dateSourceComboBox.getItems().addAll(
                    "MODIFIED (last changed)",
                    "CREATED (when created)",
                    "ACCESSED (last opened)");
            dateSourceComboBox.getSelectionModel().select("MODIFIED (last changed)");
            dateSourceComboBox.setDisable(true); // Disabled by default
        }

        // Populate date pattern choices with examples
        if (null != datePatternComboBox) {
            datePatternComboBox.getItems().addAll(
                    "YEAR_MONTH (2025-11)",
                    "YEAR_MONTH_SLASH (2025/11)",
                    "YEAR_MONTH_DAY (2025-11-03)",
                    "YEAR_MONTH_DAY_SLASH (2025/11/03)",
                    "YEAR_ONLY (2025)",
                    "YEAR_QUARTER (2025-Q4)");
            datePatternComboBox.getSelectionModel().select("YEAR_MONTH_DAY (2025-11-03)");
            datePatternComboBox.setDisable(true); // Disabled by default
        }

        // Add listener to enable/disable date options when checkbox is toggled
        if (null != dateFoldersCheckBox) {
            dateFoldersCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (null != dateSourceComboBox) {
                    dateSourceComboBox.setDisable(!newValue);
                }
                if (null != datePatternComboBox) {
                    datePatternComboBox.setDisable(!newValue);
                }
                if (null != dateTargetField) {
                    dateTargetField.setDisable(!newValue);
                }
                if (null != dateTargetBrowseButton) {
                    dateTargetBrowseButton.setDisable(!newValue);
                }
                if (null != useMetadataCheckBox) {
                    useMetadataCheckBox.setDisable(!newValue);
                }
                updateModeStatusLabel();
            });
        }

        // Disable date target controls by default
        if (null != dateTargetField) {
            dateTargetField.setDisable(true);
        }
        if (null != dateTargetBrowseButton) {
            dateTargetBrowseButton.setDisable(true);
        }
        if (null != useMetadataCheckBox) {
            useMetadataCheckBox.setDisable(true);
        }

        // Initialize rename duplicates controls
        if (null != duplicatePrefixField) {
            duplicatePrefixField.setDisable(true); // Disabled by default
        }
        if (null != renameDuplicatesCheckBox) {
            renameDuplicatesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (null != duplicatePrefixField) {
                    duplicatePrefixField.setDisable(!newValue);
                }
            });
        }

        // Initialize move/delete controls
        if (null != permanentlyDeleteCheckBox) {
            permanentlyDeleteCheckBox.setDisable(true); // Disabled by default
        }
        if (null != moveFilesCheckBox) {
            moveFilesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (null != permanentlyDeleteCheckBox) {
                    permanentlyDeleteCheckBox.setDisable(!newValue);
                    if (!newValue) {
                        permanentlyDeleteCheckBox.setSelected(false);
                    }
                }
            });
        }

        // Initialize file logging control
        if (null != writeLogToFileCheckBox) {
            // Set default log directory
            String defaultLogDir = System.getProperty("user.home") + "/.sumcompare/logs";
            if (null != logDirectoryField) {
                logDirectoryField.setText(defaultLogDir);
                logDirectoryField.setDisable(true); // Disabled by default
            }
            if (null != logDirectoryBrowseButton) {
                logDirectoryBrowseButton.setDisable(true); // Disabled by default
            }

            // Set initial state - file logging disabled by default
            writeLogToFileCheckBox.setSelected(false);

            // Add listener to enable/disable file logging and controls
            writeLogToFileCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (null != logDirectoryField) {
                    logDirectoryField.setDisable(!newValue);
                }
                if (null != logDirectoryBrowseButton) {
                    logDirectoryBrowseButton.setDisable(!newValue);
                }
                setFileLoggingEnabled(newValue);
            });
        }

        // Clear statistics
        resetStatistics();

        sourceDuplicateCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            targetTextField.setDisable(newValue);
            targetBrowseButton.setDisable(newValue);

            if (newValue) {
                targetTextField.clear();
                updateModeStatusLabel();
                appendtoUiLog("Source duplicate check mode: Target directory disabled");
            } else {
                updateModeStatusLabel();
            }
            updateStartButtonState();
        });

        // Add listeners to required fields for start button validation
        sourceTextField.textProperty().addListener((observable, oldValue, newValue) -> updateStartButtonState());
        targetTextField.textProperty().addListener((observable, oldValue, newValue) -> updateStartButtonState());
        algorithmComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateStartButtonState());

        // Initial validation
        updateStartButtonState();

        log.debug("SumCompareController initialized");

    }

    @FXML
    private void onSourceBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Source Directory");

        if (null != sourceTextField.getText() && !sourceTextField.getText().isEmpty()) {
            File current = new File(sourceTextField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (null != selected) {
            sourceTextField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onTargetBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Target Directory");

        if (null != targetTextField.getText() && !targetTextField.getText().isEmpty()) {
            File current = new File(targetTextField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (null != selected) {
            targetTextField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseLogDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Log Directory");

        if (null != logDirectoryField.getText() && !logDirectoryField.getText().isEmpty()) {
            File current = new File(logDirectoryField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (null != selected) {
            logDirectoryField.setText(selected.getAbsolutePath());
            // appendLog("Log directory changed to: " + selected.getAbsolutePath());

            // Reconfigure the file appender with new location
            if (writeLogToFileCheckBox.isSelected()) {
                setFileLoggingEnabled(false);
                setFileLoggingEnabled(true);
            }
        }
    }

    @FXML
    private void onBrowseDateTarget() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Date Organization Target Directory");

        if (null != dateTargetField.getText() && !dateTargetField.getText().isEmpty()) {
            File current = new File(dateTargetField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        } else if (null != sourceTextField.getText() && !sourceTextField.getText().isEmpty()) {
            // Default to source directory if date target is empty
            File current = new File(sourceTextField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selected = chooser.showDialog(getStage());
        if (null != selected) {
            dateTargetField.setText(selected.getAbsolutePath());
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

        // NOTE: dont bother checking if tartget is valid if in source duplicate check
        // mode
        if (!targetDir.exists() || !targetDir.isDirectory() && !sourceDir.getName().equals(targetDir.getName())) {
            showError("Target directory does not exist or is not a directory");
            return;
        }

        // Clear previous run data
        clearSingletons();
        resetStatistics();

        // Open log window
        openLogWindow();

        // Start the comparison task
        startComparisonTask();
    }

    @FXML
    private void onCancel() {
        if (null != currentTask && currentTask.isRunning()) {
            currentTask.cancel();
            appendtoUiLog("\n=== OPERATION CANCELLED BY USER ===");
            statusLabel.setText("Cancelled");
            enableControls(true);
        }
    }

    @FXML
    private void onViewLog() {
        // Ask user where to display the log
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("View Additional Logging");
        alert.setHeaderText("Where would you like to view the log?");
        alert.setContentText("Choose your option:");

        ButtonType newWindowButton = new ButtonType("New Window");
        ButtonType currentWindowButton = new ButtonType("Current Output");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(newWindowButton, currentWindowButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == newWindowButton) {
                openLogInNewWindow();
            } else if (response == currentWindowButton) {
                showLogInCurrentOutput();
            }
        });
    }

    private void openLogInNewWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logviewer.fxml"));
            Parent root = loader.load();

            Stage logStage = new Stage();
            logStage.setTitle("SumCompare - Application Log");

            Scene scene = new Scene(root, 1024, 768);
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

    private void showLogInCurrentOutput() {
        try {
            // Get log directory from field or use default
            String logDir = null != logDirectoryField && !logDirectoryField.getText().isEmpty()
                    ? logDirectoryField.getText()
                    : System.getProperty("user.home") + "/.sumcompare/logs";

            File logDirFile = new File(logDir);

            if (!logDirFile.exists() || !logDirFile.isDirectory()) {
                appendtoUiLog("Log directory not found: " + logDir);
                appendtoUiLog("Note: Enable 'Write detailed log to file' checkbox to create log files.");
                return;
            }

            // Find the most recent log file
            File[] logFiles = logDirFile
                    .listFiles((dir, name) -> name.startsWith("sumcompare_") && name.endsWith(".log"));
            if (logFiles == null || logFiles.length == 0) {
                appendtoUiLog("No log files found in: " + logDir);
                appendtoUiLog("Note: Enable 'Write detailed log to file' checkbox to create log files.");
                return;
            }

            // Sort by last modified time to get the most recent
            java.util.Arrays.sort(logFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            File logFile = logFiles[0];

            // Open log window and show log content
            openLogWindow();
            logTextArea.clear();
            appendtoUiLog("=== Application Log ===\n");
            appendtoUiLog("Location: " + logFile.getAbsolutePath() + "\n");
            appendtoUiLog("Last Modified: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(logFile.lastModified())) + "\n\n");

            java.nio.file.Files.lines(logFile.toPath())
                    .forEach(line -> {
                        if (null != logTextArea) {
                            Platform.runLater(() -> logTextArea.appendText(line + "\n"));
                        }
                    });

            // Scroll to bottom
            if (null != logTextArea) {
                Platform.runLater(() -> logTextArea.setScrollTop(Double.MAX_VALUE));
            }

            log.info("Log displayed in current output window");
        } catch (IOException e) {
            log.error("Failed to read log file", e);
            showError("Failed to read log file: " + e.getMessage());
        }
    }

    private void openLogWindow() {
        // If window already exists, just clear it and bring to front
        if (null != logWindowStage && logWindowStage.isShowing()) {
            // logTextArea.clear();
            logWindowStage.toFront();
            return;
        }

        // Create log window
        logWindowStage = new Stage();
        logWindowStage.setTitle("SumCompare - Processing Log");

        // Create text area for log
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

        // Set the static reference for LogAppenderUI to use
        LogAppenderUI.logTextArea = logTextArea;

        // Create layout
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().add(logTextArea);
        VBox.setVgrow(logTextArea, Priority.ALWAYS);

        // Add close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> logWindowStage.close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(buttonBox);

        // Create scene and show
        Scene scene = new Scene(vbox, 800, 600);
        logWindowStage.setScene(scene);

        // Don't close main window when log window closes
        logWindowStage.initModality(Modality.NONE);

        logWindowStage.show();

        appendtoUiLog("=== SumCompare Processing Log ===");
        appendtoUiLog("Started at: " + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        appendtoUiLog("");
    }

    // @FXML
    // private void onShowCliCommand() {
    // StringBuilder cliCommand = new StringBuilder("java -jar sumcompare.jar");

    // // Required options
    // if (null != sourceTextField.getText() &&
    // !sourceTextField.getText().trim().isEmpty()) {
    // cliCommand.append(" -s \"").append(sourceTextField.getText()).append("\"");
    // } else {
    // showError("Source directory is required");
    // return;
    // }

    // if (!sourceDuplicateCheckBox.isSelected()) {
    // if (null != targetTextField.getText() &&
    // !targetTextField.getText().trim().isEmpty()) {
    // cliCommand.append(" -t \"").append(targetTextField.getText()).append("\"");
    // } else {
    // showError("Target directory is required (unless in source duplicate check
    // mode)");
    // return;
    // }
    // } else {
    // cliCommand.append(" -sd");
    // cliCommand.append(" -t \"").append(sourceTextField.getText()).append("\"");
    // }

    // if (null != algorithmComboBox.getValue()) {
    // cliCommand.append(" -z ").append(algorithmComboBox.getValue());
    // } else {
    // showError("Checksum algorithm is required");
    // return;
    // }

    // // Thread count
    // if (threadCountSpinner != null && null != threadCountSpinner.getValue()) {
    // int threadCount = threadCountSpinner.getValue();
    // // Only include if not using default (number of processors)
    // if (threadCount != Runtime.getRuntime().availableProcessors()) {
    // cliCommand.append(" -tc ").append(threadCount);
    // }
    // }

    // // Optional flags
    // if (dryRunCheckBox.isSelected()) {
    // cliCommand.append(" -d");
    // }

    // if (keepStructureCheckBox.isSelected()) {
    // cliCommand.append(" -k");
    // }

    // if (backupCheckBox.isSelected()) {
    // cliCommand.append(" -b");
    // }

    // if (preserveDateCheckBox.isSelected()) {
    // cliCommand.append(" -p");
    // }

    // if (createReportCheckBox.isSelected()) {
    // cliCommand.append(" -o");
    // }

    // if (null != moveFilesCheckBox && moveFilesCheckBox.isSelected()) {
    // cliCommand.append(" -m");

    // if (null != permanentlyDeleteCheckBox &&
    // permanentlyDeleteCheckBox.isSelected()) {
    // cliCommand.append(" -pd");
    // }
    // }

    // // Date-based organization options
    // if (null != dateFoldersCheckBox && dateFoldersCheckBox.isSelected()) {
    // cliCommand.append(" -df");

    // if (dateSourceComboBox != null && null != dateSourceComboBox.getValue()) {
    // String dateSource = dateSourceComboBox.getValue();
    // String enumName = dateSource.contains(" ") ? dateSource.substring(0,
    // dateSource.indexOf(" "))
    // : dateSource;
    // cliCommand.append(" -ds ").append(enumName);
    // }

    // if (datePatternComboBox != null && null != datePatternComboBox.getValue()) {
    // String datePattern = datePatternComboBox.getValue();
    // String enumName = datePattern.contains(" ") ? datePattern.substring(0,
    // datePattern.indexOf(" "))
    // : datePattern;
    // cliCommand.append(" -dp ").append(enumName);
    // }

    // if (null != dateTargetField && !dateTargetField.getText().trim().isEmpty()) {
    // cliCommand.append(" -dt \"").append(dateTargetField.getText()).append("\"");
    // }

    // if (null != useMetadataCheckBox && useMetadataCheckBox.isSelected()) {
    // cliCommand.append(" -um");
    // }
    // }

    // // Duplicate handling options
    // if (null != renameDuplicatesCheckBox &&
    // renameDuplicatesCheckBox.isSelected()) {
    // cliCommand.append(" -rd");

    // if (null != duplicatePrefixField &&
    // !duplicatePrefixField.getText().trim().isEmpty()) {
    // cliCommand.append(" -rp
    // \"").append(duplicatePrefixField.getText()).append("\"");
    // }
    // }

    // // Cleanup options
    // if (null != deleteEmptyFoldersCheckBox &&
    // deleteEmptyFoldersCheckBox.isSelected()) {
    // cliCommand.append(" -de");
    // }

    // // Logging options
    // if (null != writeLogToFileCheckBox && writeLogToFileCheckBox.isSelected()) {
    // cliCommand.append(" -wl");

    // if (null != logDirectoryField &&
    // !logDirectoryField.getText().trim().isEmpty()) {
    // cliCommand.append(" -ld
    // \"").append(logDirectoryField.getText()).append("\"");
    // }
    // }

    // // Add -y flag to skip acceptance prompt
    // cliCommand.append(" -y");

    // // Show the command in a dialog
    // Alert alert = new Alert(Alert.AlertType.INFORMATION);
    // alert.setTitle("Command Line Equivalent");
    // alert.setHeaderText("Equivalent CLI Command");

    // TextArea textArea = new TextArea(cliCommand.toString());
    // textArea.setEditable(false);
    // textArea.setWrapText(true);
    // textArea.setPrefRowCount(10);

    // alert.getDialogPane().setContent(textArea);
    // alert.getDialogPane().setPrefWidth(800);

    // // Add copy button
    // ButtonType copyButton = new ButtonType("Copy to Clipboard");
    // ButtonType closeButton = new ButtonType("Close",
    // ButtonBar.ButtonData.CANCEL_CLOSE);
    // alert.getButtonTypes().setAll(copyButton, closeButton);

    // alert.showAndWait().ifPresent(response -> {
    // if (response == copyButton) {
    // javafx.scene.input.Clipboard clipboard =
    // javafx.scene.input.Clipboard.getSystemClipboard();
    // javafx.scene.input.ClipboardContent content = new
    // javafx.scene.input.ClipboardContent();
    // content.putString(cliCommand.toString());
    // clipboard.setContent(content);
    // appendLog("CLI command copied to clipboard");
    // }
    // });
    // }

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

    // FIXME: This method is HUGE - break it up into smaller methods
    private void startComparisonTask() {
        startTime = Instant.now();
        enableControls(false);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Starting comparison...");

        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Populate singleton propertiesObject from UI
                    PropertiesObject propertiesObject = PropertiesObject.getInstance();
                    propertiesObject.populateFromController(
                            sourceTextField,
                            targetTextField,
                            dryRunCheckBox,
                            keepStructureCheckBox,
                            backupCheckBox,
                            preserveDateCheckBox,
                            createReportCheckBox,
                            sourceDuplicateCheckBox,
                            useMetadataCheckBox,
                            renameDuplicatesCheckBox,
                            duplicatePrefixField,
                            deleteEmptyFoldersCheckBox,
                            moveFilesCheckBox,
                            permanentlyDeleteCheckBox,
                            dateFoldersCheckBox,
                            dateSourceComboBox,
                            datePatternComboBox,
                            dateTargetField,
                            algorithmComboBox,
                            threadCountSpinner);

                    appendtoUiLog(
                            "Using comparison algorithm: "
                                    + PropertiesObject.getInstance().getDigestType().getAlgorithm());

                    // Begin Source Backup
                    if (PropertiesObject.getInstance().isBackupFirst()) {
                        appendtoUiLog(
                                "Backup requested: A backup of the source directory will be created before processing");
                        FileUtilsLocal.zipDirectory();
                    } else {
                        appendtoUiLog("Backup not requested: Proceeding without backup");
                    }

                    // Setup the target folder scan thread
                    Thread targetScanThread = new Thread(() -> {
                        try {
                            // NOTE: if in source duplicate check only mode, we still need to scan the
                            // source as the target

                            if (PropertiesObject.getInstance().isSourceDuplicateCheckOnly()) {
                                FileUtilsLocal.getTargetDirectoryContentsArray(
                                        PropertiesObject.getInstance().getSourceLocation());
                            } else {
                                FileUtilsLocal.getTargetDirectoryContentsArray(
                                        PropertiesObject.getInstance().getTargetLocation());
                            }

                            int targetCount = TargetFileArraySingleton.getInstance().getArray().size();
                            String targetItemCountMsg = "Found " + targetCount + " files in target";
                            statusLabel.setText(targetItemCountMsg);
                            appendtoUiLog(targetItemCountMsg);

                            // Step 3: Compute target checksums
                            String targetItemCksumMsg = "Computing target checksums...";
                            statusLabel.setText(targetItemCksumMsg);
                            appendtoUiLog(targetItemCksumMsg);

                            FileUtilsLocal.createTargetFileChecksumMap(
                                    TargetFileArraySingleton.getInstance(),
                                    PropertiesObject.getInstance().getDigestType());

                            String targetItemCksumCompleteMsg = "Target checksums completed";
                            statusLabel.setText(targetItemCksumCompleteMsg);
                            appendtoUiLog(targetItemCksumCompleteMsg);
                        } catch (Exception e) {
                            log.error("Error scanning target directory", e);
                            String errorMsg = "ERROR scanning target: " + e.getMessage();
                            statusLabel.setText(errorMsg);
                            appendtoUiLog(errorMsg);
                        }
                    });

                    // setup the source folder scan thread
                    Thread sourceScanThread = new Thread(() -> {
                        try {
                            FileUtilsLocal.getSourceDirectoryContentsArray(
                                    PropertiesObject.getInstance().getSourceLocation());
                            int sourceCount = SourceFileArraySingleton.getInstance().getArray().size();
                            String msg = "Source Location contains  " + sourceCount + " items";

                            statusLabel.setText(msg);
                            appendtoUiLog(msg);
                            updateScannedCount(sourceCount);

                        } catch (Exception e) {
                            log.error("Error scanning source directory", e);
                            String errorMsg = "ERROR scanning source: " + e.getMessage();
                            statusLabel.setText(errorMsg);
                            appendtoUiLog(errorMsg);
                        }
                    });

                    // Start both scan threads
                    sourceScanThread.start();
                    targetScanThread.start();

                    // Wait for both threads to complete before proceeding
                    String sourceScanWaitMsg = "Waiting for source directory scan to complete...";
                    log.debug(sourceScanWaitMsg);
                    appendtoUiLog(sourceScanWaitMsg);

                    String targetScanWaitMsg = "Waiting for target directory scan to complete...";
                    log.debug(targetScanWaitMsg);
                    appendtoUiLog(targetScanWaitMsg);

                    sourceScanThread.join();
                    String sourceScanCompleteMsg = "Source scan thread completed";
                    log.debug(sourceScanCompleteMsg);
                    appendtoUiLog(sourceScanCompleteMsg);

                    targetScanThread.join();
                    String targetScanCompleteMsg = "Target scan thread completed";
                    log.debug(targetScanCompleteMsg);
                    appendtoUiLog(targetScanCompleteMsg);

                    // Step 5: Process source files
                    appendtoUiLog("Beginning to process source files...");

                    processSourceFiles();

                    // Step 6: Generate report if requested
                    if (PropertiesObject.getInstance().isCreateOutputFile()) {
                        updateMessage("Generating Excel report...");
                        ReportUtils.createOutputExcel();
                        updateMessage("Report created: Copy_Output.xlsx");
                    }

                    // Update final statistics
                    int copied = CopiedFileHashMapSingleton.getInstance().getMap().size();
                    int duplicates = MatchingFileHashMapSingleton.getInstance().getMap().size();
                    updateCopiedCount(copied);
                    updateDuplicatesCount(duplicates);

                    // Delete empty folders in source if enabled
                    deleteEmptyFolders(PropertiesObject.getInstance().getSourceLocation());

                    updateMessage("\n=== COMPLETED SUCCESSFULLY ===");
                    updateMessage("Files copied: " + copied);
                    updateMessage("Duplicates found: " + duplicates);

                    progressBar.setProgress(1.0);
                    statusLabel.setText("Completed");

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
                progressBar.setProgress(0);
                statusLabel.setText("Failed");
                showError("Comparison failed: " + getException().getMessage());
            }

            @Override
            protected void cancelled() {
                stopElapsedTimeUpdater();
                enableControls(true);
                progressBar.setProgress(0);
                statusLabel.setText("Cancelled");
            }
        };

        // Bind message property to status and log
        currentTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (null != newMsg && !newMsg.isEmpty()) {
                statusLabel.setText(newMsg);
                appendtoUiLog(newMsg);
            }
        });

        // Start elapsed time updater
        startElapsedTimeUpdater();

        // Run task in background thread
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void processSourceFiles() throws Exception {
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

                    // Get the current file to be processed from the array
                    File thisSourceFile = new File(sourceFile);

                    // Get the metadata for the current file
                    // log.debug("attempting to get file metadata for: {}",
                    // thisSourceFile.getAbsolutePath());
                    FileMetadata fileMetadata = new FileMetadata();

                    fileMetadata = FileMetadataExtractor.getFileMetadata(thisSourceFile,
                            PropertiesObject.getInstance());

                    // In date-sort-only mode, skip duplicate checking and just organize files into
                    // date folders
                    if (PropertiesObject.getInstance().isSourceDuplicateCheckOnly()
                            && PropertiesObject.getInstance().isOrganizeDateFolders()) {
                        // Just copy/organize the files without any duplicate checking
                        String targetPath = calculateTargetPath(fileMetadata);
                        CopiedFileHashMapSingleton.getInstance().addToMap(sourceFile, targetPath);

                        File targetFile = new File(targetPath);

                        // Ensure date-based folder exists before copying
                        org.bofus.sumcompare.localutil.DateFolderOrganizer.ensureDateFolderExists(targetFile);

                        log.debug("Move instead of copy: {}", PropertiesObject.getInstance().isMoveInsteadOfCopy());

                        if (PropertiesObject.getInstance().isMoveInsteadOfCopy()) {
                            if (PropertiesObject.getInstance().isDryRun()) {
                                String fileName = thisSourceFile.getName();
                                String logMsg = String.format("(DRYRUN) - Would move (date organize) %s ", fileName);
                                appendtoUiLog(logMsg);
                                log.debug(logMsg);
                                updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                                return;
                            }
                            // Move file: copy then delete/trash source
                            org.apache.commons.io.FileUtils.copyFile(thisSourceFile, targetFile,
                                    PropertiesObject.getInstance().isPreserveFileDate());
                            if (deleteOrTrashFile(thisSourceFile,
                                    PropertiesObject.getInstance().isPermanentlyDelete())) {
                                String fileName = thisSourceFile.getName();
                                String action = PropertiesObject.getInstance().isPermanentlyDelete() ? "Moved (deleted)"
                                        : "Moved (to trash)";
                                String logMsg = String.format("%s %s", action, fileName);
                                appendtoUiLog(logMsg);
                                log.debug(logMsg);
                            } else {
                                String fileName = thisSourceFile.getName();
                                String logMsg = String.format("Copied but failed to delete source [%s]: %s",
                                        fileName);
                                appendtoUiLog(logMsg);
                                log.debug(logMsg);
                            }
                        } else {
                            if (PropertiesObject.getInstance().isDryRun()) {
                                String fileName = thisSourceFile.getName();
                                String logMsg = String.format("(DRYRUN) - Would copy (date organize) %s ", fileName);
                                appendtoUiLog(logMsg);
                                log.debug(logMsg);
                                updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                                return;
                            }
                            // Normal copy
                            org.apache.commons.io.FileUtils.copyFile(thisSourceFile, targetFile,
                                    PropertiesObject.getInstance().isPreserveFileDate());
                            String fileName = thisSourceFile.getName();
                            String logMsg = String.format("Organized [%s]", fileName);
                            appendtoUiLog(logMsg);
                            log.debug(logMsg);
                        }
                        // }

                        updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                    } else {
                        // Normal mode: check for duplicates
                        MessageDigest threadDigest = (MessageDigest) PropertiesObject.getInstance().getDigestType()
                                .clone();
                        String sourceCheckSum = FileUtilsLocal.getFileChecksum(threadDigest, thisSourceFile);

                        List<String> existingFiles = FileUtilsLocal
                                .getKeysWithValue(TargetFileHashMapSingleton.getInstance().getMap(), sourceCheckSum);

                        if (TargetFileHashMapSingleton.getInstance().getMap().containsKey(sourceCheckSum)) {
                            String existingFile = TargetFileHashMapSingleton.getInstance().getMap().get(sourceCheckSum);
                            String sourceFileName = FileUtilsLocal.getFileName(sourceFile);
                            String targetFileName = FileUtilsLocal.getFileName(existingFile);

                            // Handle duplicate: either rename source file or just log it
                            if (PropertiesObject.getInstance().isRenameDuplicates()) {
                                // Rename the duplicate source file
                                String prefix = PropertiesObject.getInstance().getDuplicatePrefix();
                                File sourceFileObj = new File(sourceFile);
                                String newFileName = prefix + sourceFileObj.getName();
                                File renamedFile = new File(sourceFileObj.getParent(), newFileName);

                                if (PropertiesObject.getInstance().isDryRun()) {
                                    String logMsg = String.format("(DRYRUN) - Would rename duplicate %s -> %s ",
                                            sourceFileName, newFileName);
                                    appendtoUiLog(logMsg);
                                    log.debug(logMsg);
                                } else {
                                    // Actually rename the file
                                    if (sourceFileObj.renameTo(renamedFile)) {
                                        String logMsg = String.format("Renamed duplicate %s -> %s ",
                                                sourceFileName, newFileName);
                                        appendtoUiLog(logMsg);
                                        log.debug(logMsg);
                                    } else {
                                        String logMsg = String.format("Failed to rename [%s]: %s ",
                                                sourceFileName);
                                        appendtoUiLog(logMsg);
                                        log.debug(logMsg);
                                    }
                                }
                                MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                            } else {
                                // Original behavior: just log the duplicate
                                if (sourceFileName.equals(targetFileName)) {
                                    MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                                } else {
                                    String logMsg = String.format("Duplicate %s -> %s ",
                                            sourceFileName, existingFile);
                                    appendtoUiLog(logMsg);
                                    log.debug(logMsg);
                                    MatchingFileHashMapSingleton.getInstance().addToMap(sourceFile, existingFile);
                                }
                            }
                        } else {
                            // File needs to be copied
                            String targetPath = calculateTargetPath(fileMetadata);
                            CopiedFileHashMapSingleton.getInstance().addToMap(sourceFile, targetPath);

                            if (PropertiesObject.getInstance().isDryRun()) {
                                String fileName = thisSourceFile.getName();
                                String action = PropertiesObject.getInstance().isMoveInsteadOfCopy() ? "move" : "copy";
                                String logMsg = String.format("(DRYRUN) - Would %s %s ",
                                        action, fileName);
                                appendtoUiLog(logMsg);
                                log.debug(logMsg);
                            } else {
                                File targetFile = new File(targetPath);

                                // Ensure date-based folder exists before copying
                                if (PropertiesObject.getInstance().isOrganizeDateFolders()) {
                                    if (PropertiesObject.getInstance().isDryRun()) {
                                        String fileName = thisSourceFile.getName();
                                        String logMsg = String.format(
                                                "(DRYRUN) - Would ensure date folder exists for %s ",
                                                fileName);
                                        appendtoUiLog(logMsg);
                                        log.debug(logMsg);
                                        updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                                        return;
                                    }
                                    org.bofus.sumcompare.localutil.DateFolderOrganizer
                                            .ensureDateFolderExists(targetFile);
                                }

                                if (PropertiesObject.getInstance().isMoveInsteadOfCopy()) {
                                    // Move file: copy then delete source
                                    org.apache.commons.io.FileUtils.copyFile(thisSourceFile, targetFile,
                                            PropertiesObject.getInstance().isPreserveFileDate());

                                    if (PropertiesObject.getInstance().isDryRun()) {
                                        String fileName = thisSourceFile.getName();
                                        String logMsg = String.format("(DRYRUN) - Would have deleted file %s ",
                                                fileName);
                                        appendtoUiLog(logMsg);
                                        log.debug(logMsg);
                                    } else {
                                        if (deleteOrTrashFile(thisSourceFile,
                                                PropertiesObject.getInstance().isPermanentlyDelete())) {
                                            String fileName = thisSourceFile.getName();
                                            String action = PropertiesObject.getInstance().isPermanentlyDelete()
                                                    ? "deleted"
                                                    : "to trash";
                                            String logMsg = String.format("Moved %s %s ",
                                                    action, fileName);
                                            appendtoUiLog(logMsg);
                                            log.debug(logMsg);
                                        } else {
                                            String fileName = thisSourceFile.getName();
                                            String action = PropertiesObject.getInstance().isPermanentlyDelete()
                                                    ? "delete"
                                                    : "to trash";
                                            String logMsg = String.format(
                                                    "Copied but failed to %s source %s ",
                                                    action, fileName);
                                            appendtoUiLog(logMsg);
                                            log.debug(logMsg);
                                        }
                                    }
                                } else {
                                    // Normal copy
                                    if (PropertiesObject.getInstance().isDryRun()) {
                                        String fileName = thisSourceFile.getName();
                                        String logMsg = String.format("(DRYRUN) - Would copy %s ", fileName);
                                        appendtoUiLog(logMsg);
                                        log.debug(logMsg);
                                        updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                                    } else {
                                        org.apache.commons.io.FileUtils.copyFile(thisSourceFile, targetFile,
                                                PropertiesObject.getInstance().isPreserveFileDate());
                                        String fileName = thisSourceFile.getName();
                                        String logMsg = String.format("Copied [%s]: %s (%s)",
                                                fileName);
                                        appendtoUiLog(logMsg);
                                        log.debug(logMsg);

                                    }
                                }
                            }

                            updateCopiedCount(CopiedFileHashMapSingleton.getInstance().getMap().size());
                        }

                        // Update duplicate count only in normal mode (not date-sort-only)
                        updateDuplicatesCount(MatchingFileHashMapSingleton.getInstance().getMap().size());
                    }
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

    // FIXME: This method is where I need to validate functionality
    private String calculateTargetPath(FileMetadata fileMetadata) {

        File sourceFile = new File(fileMetadata.getAbsolutePath());
        File targetFile = new File(PropertiesObject.getInstance().getTargetLocation(), sourceFile.getName());

        // Use date-based folder organization if enabled
        if (PropertiesObject.getInstance().isOrganizeDateFolders()) {
            try {
                File baseTargetDir = new File(fileMetadata.getDateTargetLocation());

                targetFile = org.bofus.sumcompare.localutil.DateFolderOrganizer.generateDateBasedTargetPath(
                        sourceFile,
                        baseTargetDir,
                        PropertiesObject.getInstance().getDateSource(),
                        PropertiesObject.getInstance().getDatePattern(),
                        PropertiesObject.getInstance().isKeepSourceStructure(),
                        PropertiesObject.getInstance().isUseMetadata(),
                        fileMetadata);

                return targetFile.getAbsolutePath();
            } catch (Exception e) {
                log.error("Error generating date-based path for {}, falling back to standard path", sourceFile, e);
                // Fallback to standard logic
                return PropertiesObject.getInstance().getTargetLocation() + File.separator + targetFile.getName();
            }
        } else if (PropertiesObject.getInstance().isKeepSourceStructure()) {
            String sourceBasePath = sourceFile.getAbsolutePath()
                    .replace(PropertiesObject.getInstance().getSourceLocation(), "");
            String tempPath = org.apache.commons.io.FilenameUtils.getPath(sourceBasePath);
            return PropertiesObject.getInstance().getTargetLocation() + File.separator + tempPath + File.separator
                    + targetFile.getName();
        } else {
            return PropertiesObject.getInstance().getTargetLocation() + File.separator + targetFile.getName();
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
        if (null != timerTask && timerTask.isRunning()) {
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

    private void updateModeStatusLabel() {
        Platform.runLater(() -> {
            boolean sourceDuplicateMode = sourceDuplicateCheckBox.isSelected();
            boolean dateFoldersMode = null != dateFoldersCheckBox && dateFoldersCheckBox.isSelected();

            if (sourceDuplicateMode && dateFoldersMode) {
                // Date-Sort-Only Mode
                statusLabel.setText(
                        "Mode: DATE-SORT-ONLY (Check source against itself for duplicates and Organize by date)");
                statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); // Blue color
            } else if (sourceDuplicateMode) {
                // Source Duplicate Check Mode
                statusLabel.setText("Mode: SOURCE DUPLICATE CHECK (Check source against itself for duplicates)");
                statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); // Orange color
            } else if (dateFoldersMode) {
                // Normal mode with date organization
                statusLabel.setText("Mode: NORMAL with date organization");
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); // Green color
            } else {
                // Normal mode
                statusLabel.setText("Ready");
                statusLabel.setStyle(""); // Reset to default
            }
        });
    }

    /**
     * Deletes or moves a file to trash based on the permanently delete setting.
     * 
     * @param file              the file to delete
     * @param permanentlyDelete if true, permanently delete; if false, move to trash
     * @return true if the operation was successful
     */
    private boolean deleteOrTrashFile(File file, boolean permanentlyDelete) {
        if (permanentlyDelete) {
            // Permanent deletion
            return file.delete();
        } else {
            // Try to move to trash using Desktop API
            if (java.awt.Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.MOVE_TO_TRASH)) {
                        return desktop.moveToTrash(file);
                    }
                } catch (Exception e) {
                    log.warn("Failed to move file to trash, falling back to permanent delete: {}", file.getName(), e);
                }
            }
            // Fallback to permanent delete if trash is not supported
            log.warn("Trash not supported on this system, permanently deleting: {}", file.getName());
            return file.delete();
        }
    }

    private void deleteEmptyFolders(String sourceDirectory) {
        if (!PropertiesObject.getInstance().isDeleteEmptyFolders()) {
            String logmsg = "Skipping deletion of empty folders as delete empty folders is disabled.";
            appendtoUiLog(logmsg);
            log.debug(logmsg);
            return;
        }

        if (PropertiesObject.getInstance().isDryRun()) {
            String logmsg = "(DRYRUN) - Skipping deletion of empty folders as dry run is enabled.";
            appendtoUiLog(logmsg);
            log.debug(logmsg);
            return;
        }

        File sourceDir = new File(sourceDirectory);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }

        appendtoUiLog("Cleaning up empty folders...");
        int deletedCount = deleteEmptyFoldersRecursive(sourceDir, sourceDir);
        if (deletedCount > 0) {
            int finalCount = deletedCount;
            appendtoUiLog("Deleted " + finalCount + " empty folder(s)");
        } else {
            appendtoUiLog("No empty folders found");
        }
    }

    private int deleteEmptyFoldersRecursive(File directory, File rootDir) {
        int deletedCount = 0;

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        // Don't delete the root source directory itself
        if (directory.equals(rootDir)) {
            File[] children = directory.listFiles();
            if (null != children) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        deletedCount += deleteEmptyFoldersRecursive(child, rootDir);
                    }
                }
            }
            return deletedCount;
        }

        // First, recursively process subdirectories
        File[] children = directory.listFiles();
        if (null != children) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deletedCount += deleteEmptyFoldersRecursive(child, rootDir);
                }
            }
        }

        // After processing children, check if this directory is now empty
        children = directory.listFiles();
        if (null != children && children.length == 0) {
            if (directory.delete()) {
                log.trace("Deleted empty folder: {}", directory.getAbsolutePath());
                deletedCount++;
            }
        }

        return deletedCount;
    }

    private void updateStartButtonState() {
        Platform.runLater(() -> {
            boolean hasSource = null != sourceTextField.getText() && !sourceTextField.getText().trim().isEmpty();
            boolean hasTarget = null != targetTextField.getText() && !targetTextField.getText().trim().isEmpty();
            boolean hasAlgorithm = null != algorithmComboBox.getValue();
            boolean isSourceDuplicateMode = sourceDuplicateCheckBox.isSelected();

            // Valid if: has source, has algorithm, and (has target OR is in source
            // duplicate mode)
            boolean isValid = hasSource && hasAlgorithm && (hasTarget || isSourceDuplicateMode);

            if (isValid) {
                // Green button when ready
                startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            } else {
                // Reset to default style
                startButton.setStyle("");
            }
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

    /**
     * Enables or disables file logging by controlling the FILE appender in Logback.
     * Creates a new file appender with timestamped filename in the user-selected
     * directory.
     * 
     * @param enabled true to enable file logging, false to disable
     */
    private void setFileLoggingEnabled(boolean enabled) {
        try {
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

            // Remove existing file appender if present
            ch.qos.logback.core.Appender<?> existingAppender = rootLogger.getAppender("FILE");
            if (null != existingAppender) {
                existingAppender.stop();
                rootLogger.detachAppender("FILE");
            }

            if (enabled) {
                // Get log directory from field or use default
                String logDir = null != logDirectoryField && !logDirectoryField.getText().isEmpty()
                        ? logDirectoryField.getText()
                        : System.getProperty("user.home") + "/.sumcompare/logs";

                // Create log directory if it doesn't exist
                File logDirFile = new File(logDir);
                if (!logDirFile.exists()) {
                    logDirFile.mkdirs();
                }

                // Generate timestamped filename
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                String logFilePath = logDir + "/sumcompare_" + timestamp + ".log";

                // Create new file appender
                ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory
                        .getILoggerFactory();

                ch.qos.logback.core.FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new ch.qos.logback.core.FileAppender<>();
                fileAppender.setContext(loggerContext);
                fileAppender.setName("FILE");
                fileAppender.setFile(logFilePath);
                fileAppender.setAppend(true);

                // Set encoder pattern
                ch.qos.logback.classic.encoder.PatternLayoutEncoder encoder = new ch.qos.logback.classic.encoder.PatternLayoutEncoder();
                encoder.setContext(loggerContext);
                encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
                encoder.start();

                fileAppender.setEncoder(encoder);
                fileAppender.start();

                rootLogger.addAppender(fileAppender);

                log.info("File logging enabled: {}", logFilePath);
                appendtoUiLog("File logging enabled: " + logFilePath);
            } else {
                log.info("File logging disabled");
                appendtoUiLog("File logging disabled");
            }
        } catch (Exception e) {
            log.error("Failed to toggle file logging", e);
            showError("Failed to toggle file logging: " + e.getMessage());
        }
    }
}
