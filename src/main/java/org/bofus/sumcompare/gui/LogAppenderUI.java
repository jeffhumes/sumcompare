package org.bofus.sumcompare.gui;

import org.bofus.sumcompare.model.PropertiesObject;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogAppenderUI {

    public static TextArea logTextArea;

    public static void appendLog(String message) {
        Platform.runLater(() -> {
            if (null != logTextArea) {
                logTextArea.appendText(message + "\n");
            }
        });

        // Also log to file if file logging is enabled
        if (PropertiesObject.getInstance().isCreateOutputFile()) {
            log.info("[UI] {}", message);
        }
    }

}
