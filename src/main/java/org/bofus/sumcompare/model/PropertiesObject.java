package org.bofus.sumcompare.model;

import org.bofus.sumcompare.localutil.DateFolderOrganizer;
import org.bofus.sumcompare.localutil.FileUtilsLocal;

import lombok.Data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

@Data
public class PropertiesObject {
	private static PropertiesObject instance;

	private PropertiesObject() {
		// Private constructor to prevent instantiation
	}

	public static PropertiesObject getInstance() {
		if (instance == null) {
			instance = new PropertiesObject();
		}
		return instance;
	}

	/**
	 * Populates the singleton properties from UI components in the controller.
	 * This method should be called before starting any processing operations.
	 */
	public void populateFromController(
			TextField sourceTextField,
			TextField targetTextField,
			CheckBox dryRunCheckBox,
			CheckBox keepStructureCheckBox,
			CheckBox backupCheckBox,
			CheckBox preserveDateCheckBox,
			CheckBox createReportCheckBox,
			CheckBox sourceDuplicateCheckBox,
			CheckBox useMetadataCheckBox,
			CheckBox renameDuplicatesCheckBox,
			TextField duplicatePrefixField,
			CheckBox deleteEmptyFoldersCheckBox,
			CheckBox moveFilesCheckBox,
			CheckBox permanentlyDeleteCheckBox,
			CheckBox dateFoldersCheckBox,
			ComboBox<String> dateSourceComboBox,
			ComboBox<String> datePatternComboBox,
			TextField dateTargetField,
			ComboBox<String> algorithmComboBox,
			Spinner<Integer> threadCountSpinner) {

		this.sourceLocation = sourceTextField.getText();
		this.targetLocation = targetTextField.getText();
		this.dryRun = dryRunCheckBox.isSelected();
		this.keepSourceStructure = keepStructureCheckBox.isSelected();
		this.backupFirst = backupCheckBox.isSelected();
		this.preserveFileDate = preserveDateCheckBox.isSelected();
		this.createOutputFile = createReportCheckBox.isSelected();
		this.sourceDuplicateCheckOnly = sourceDuplicateCheckBox.isSelected();
		this.useMetadata = null != useMetadataCheckBox && useMetadataCheckBox.isSelected();

		// Set duplicate renaming options
		this.renameDuplicates = null != renameDuplicatesCheckBox && renameDuplicatesCheckBox.isSelected();

		if (null != duplicatePrefixField && !duplicatePrefixField.getText().trim().isEmpty()) {
			this.duplicatePrefix = duplicatePrefixField.getText().trim();
		} else {
			this.duplicatePrefix = "DUPLICATE_FILE_";
		}

		// Set cleanup options
		this.deleteEmptyFolders = null != deleteEmptyFoldersCheckBox && deleteEmptyFoldersCheckBox.isSelected();
		this.moveInsteadOfCopy = null != moveFilesCheckBox && moveFilesCheckBox.isSelected();
		this.permanentlyDelete = null != permanentlyDeleteCheckBox && permanentlyDeleteCheckBox.isSelected();

		// Set date-based folder organization
		if (null != dateFoldersCheckBox && dateFoldersCheckBox.isSelected()) {
			this.organizeDateFolders = true;

			// Set date target directory (defaults to source if empty)
			if (null != dateTargetField && !dateTargetField.getText().trim().isEmpty()) {
				this.dateTargetDirectory = dateTargetField.getText().trim();
			} else {
				// Default to source directory
				this.dateTargetDirectory = sourceTextField.getText();
			}

			// Set date source
			if (null != dateSourceComboBox) {
				String dateSourceStr = dateSourceComboBox.getValue();
				// Extract enum name before the space (e.g., "MODIFIED (last changed)" ->
				// "MODIFIED")
				String enumName = dateSourceStr.contains(" ")
						? dateSourceStr.substring(0, dateSourceStr.indexOf(" "))
						: dateSourceStr;
				this.dateSource = DateFolderOrganizer.DateSource.valueOf(enumName);
			} else {
				this.dateSource = DateFolderOrganizer.DateSource.MODIFIED;
			}

			// Set date pattern
			if (null != datePatternComboBox) {
				String datePatternStr = datePatternComboBox.getValue();
				// Extract enum name before the space (e.g., "YEAR_MONTH (2025-11)" ->
				// "YEAR_MONTH")
				String enumName = datePatternStr.contains(" ")
						? datePatternStr.substring(0, datePatternStr.indexOf(" "))
						: datePatternStr;
				this.datePattern = DateFolderOrganizer.DatePattern.valueOf(enumName);
			} else {
				this.datePattern = DateFolderOrganizer.DatePattern.YEAR_MONTH;
			}

			@SuppressWarnings("unused")
			String orgDescription = DateFolderOrganizer.getOrganizationDescription(
					this.dateSource, this.datePattern);
		} else {
			this.organizeDateFolders = false;
		}

		// Set digest type
		String algorithm = algorithmComboBox.getValue();
		MessageDigest digest = null;
		try {
			digest = FileUtilsLocal.SetDigestType(algorithm);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		this.digestType = digest;

		// Set thread count
		if (null != threadCountSpinner && null != threadCountSpinner.getValue()) {
			this.threadCount = threadCountSpinner.getValue();
		} else {
			this.threadCount = Runtime.getRuntime().availableProcessors();
		}
	}

	private String sourceLocation;
	private String targetLocation;
	private MessageDigest digestType;
	private boolean postCopyRemove;
	private boolean preserveFileDate;
	private boolean createOutputFile;
	private boolean dryRun;
	private boolean backupFirst;
	private boolean keepSourceStructure;
	private boolean organizeDateFolders;
	private DateFolderOrganizer.DateSource dateSource;
	private DateFolderOrganizer.DatePattern datePattern;
	private boolean sourceDuplicateCheckOnly;
	private String dateTargetDirectory;
	private boolean useMetadata;
	private boolean renameDuplicates;
	private String duplicatePrefix;
	private boolean deleteEmptyFolders;
	private boolean moveInsteadOfCopy;
	private boolean permanentlyDelete;
	private int threadCount;
}
