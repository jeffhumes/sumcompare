package org.bofus.sumcompare.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * JavaFX GUI application for SumCompare file deduplication tool.
 * Provides a user-friendly interface as an alternative to the CLI.
 */
@Slf4j
public class SumCompareGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sumcompare.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("SumCompare - Intelligent File Deduplication");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(700);
            primaryStage.setMinHeight(500);
            primaryStage.show();

            log.info("SumCompare GUI started successfully");
        } catch (Exception e) {
            log.error("Failed to start GUI", e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
