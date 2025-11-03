package org.bofus.sumcompare.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for the Log Viewer window.
 * Displays the application log file in real-time.
 */
@Slf4j
public class LogViewerController {

    @FXML
    private TextArea logTextArea;

    private Thread tailThread;
    private AtomicBoolean running = new AtomicBoolean(true);
    private Path logFilePath;

    @FXML
    public void initialize() {
        // Determine log file path
        String userHome = System.getProperty("user.home");
        logFilePath = Paths.get(userHome, ".sumcompare", "logs", "sumcompare.log");

        // Create log directory if it doesn't exist
        try {
            Files.createDirectories(logFilePath.getParent());
        } catch (IOException e) {
            log.error("Failed to create log directory", e);
        }

        // Load existing log content
        loadExistingLog();

        // Start tailing the log file
        startTailing();

        log.info("LogViewerController initialized");
    }

    private void loadExistingLog() {
        if (Files.exists(logFilePath)) {
            try {
                String content = Files.readString(logFilePath);
                Platform.runLater(() -> {
                    logTextArea.setText(content);
                    logTextArea.positionCaret(logTextArea.getLength());
                });
            } catch (IOException e) {
                log.error("Failed to read existing log file", e);
                Platform.runLater(() -> logTextArea.setText("Error loading log file: " + e.getMessage()));
            }
        } else {
            Platform.runLater(() -> logTextArea
                    .setText("Log file not found. It will be created when the application starts logging.\n"));
        }
    }

    private void startTailing() {
        tailThread = new Thread(() -> {
            try {
                long lastPosition = 0;

                // If file exists, start from the end
                if (Files.exists(logFilePath)) {
                    lastPosition = Files.size(logFilePath);
                }

                while (running.get()) {
                    if (Files.exists(logFilePath)) {
                        long currentSize = Files.size(logFilePath);

                        if (currentSize > lastPosition) {
                            // File has grown, read new content
                            try (RandomAccessFile raf = new RandomAccessFile(logFilePath.toFile(), "r")) {
                                raf.seek(lastPosition);
                                String line;
                                StringBuilder newContent = new StringBuilder();

                                while ((line = raf.readLine()) != null) {
                                    if (line.length() > 0) {
                                        // Handle potential encoding issues
                                        line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                                    }
                                    newContent.append(line).append("\n");
                                }

                                if (newContent.length() > 0) {
                                    String finalContent = newContent.toString();
                                    Platform.runLater(() -> {
                                        logTextArea.appendText(finalContent);
                                        logTextArea.positionCaret(logTextArea.getLength());
                                    });
                                }

                                lastPosition = raf.getFilePointer();
                            }
                        } else if (currentSize < lastPosition) {
                            // File was truncated or recreated
                            lastPosition = 0;
                            Platform.runLater(() -> {
                                logTextArea.clear();
                                logTextArea.setText("Log file was truncated or recreated.\n");
                            });
                        }
                    } else {
                        // Wait for log file to be created
                        lastPosition = 0;
                    }

                    // Check every 500ms
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                log.error("Log tail thread interrupted");
            } catch (IOException e) {
                log.error("Error tailing log file", e);
                Platform.runLater(() -> logTextArea.appendText("\nError reading log file: " + e.getMessage() + "\n"));
            }
        });

        tailThread.setDaemon(true);
        tailThread.start();
    }

    @FXML
    private void onClear() {
        logTextArea.clear();
        Platform.runLater(() -> logTextArea.setText("Log display cleared (file not modified).\n"));
    }

    @FXML
    private void onRefresh() {
        logTextArea.clear();
        loadExistingLog();
    }

    @FXML
    private void onClose() {
        running.set(false);
        if (tailThread != null) {
            tailThread.interrupt();
        }
        Stage stage = (Stage) logTextArea.getScene().getWindow();
        stage.close();
    }

    public void shutdown() {
        running.set(false);
        if (tailThread != null) {
            tailThread.interrupt();
        }
    }
}
