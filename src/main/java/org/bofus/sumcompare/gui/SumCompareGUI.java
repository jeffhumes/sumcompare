package org.bofus.sumcompare.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX GUI application for SumCompare file deduplication tool.
 * Provides a user-friendly interface as an alternative to the CLI.
 */
public class SumCompareGUI extends Application {
    private static final Logger logger = LoggerFactory.getLogger(SumCompareGUI.class);

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

            logger.info("SumCompare GUI started successfully");
        } catch (Exception e) {
            logger.error("Failed to start GUI", e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
