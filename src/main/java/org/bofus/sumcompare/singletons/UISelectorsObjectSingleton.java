package org.bofus.sumcompare.singletons;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Data;

@Data
public class UISelectorsObjectSingleton {

    private static UISelectorsObjectSingleton instance;

    private UISelectorsObjectSingleton() {
        // Private constructor to prevent instantiation
    }

    public static UISelectorsObjectSingleton getInstance() {
        if (instance == null) {
            instance = new UISelectorsObjectSingleton();
        }
        return instance;
    }

    private TextField sourceTextField;

    private TextField targetTextField;

    private Button targetBrowseButton;

    private ComboBox<String> algorithmComboBox;

    private Spinner<Integer> threadCountSpinner;

    private CheckBox dryRunCheckBox;

    private CheckBox keepStructureCheckBox;

    private CheckBox backupCheckBox;

    private CheckBox preserveDateCheckBox;

    private CheckBox createReportCheckBox;

    private CheckBox writeLogToFileCheckBox;

    private TextField logDirectoryField;

    private Button logDirectoryBrowseButton;

    private Button startButton;

    private Button cancelButton;

    private ProgressBar progressBar;

    private Label statusLabel;

    // Log window components (not in FXML - created dynamically)
    private Stage logWindowStage;
    private TextArea logTextArea;

    private Label scannedCountLabel;

    private Label copiedCountLabel;

    private Label duplicatesCountLabel;

    private Label elapsedTimeLabel;

    private CheckBox sourceDuplicateCheckBox;

    private CheckBox dateFoldersCheckBox;

    private ComboBox<String> dateSourceComboBox;

    private ComboBox<String> datePatternComboBox;

    private TextField dateTargetField;

    private Button dateTargetBrowseButton;

    private CheckBox useMetadataCheckBox;

    private CheckBox renameDuplicatesCheckBox;

    private TextField duplicatePrefixField;

    private CheckBox deleteEmptyFoldersCheckBox;

    private CheckBox moveFilesCheckBox;

    private CheckBox permanentlyDeleteCheckBox;

}
