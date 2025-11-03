package org.bofus.sumcompare.localutil;

import java.beans.PropertyVetoException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bofus.sumcompare.Main;
import org.bofus.sumcompare.model.ExistingTargetFileObject;
import org.bofus.sumcompare.singletons.CopiedFileHashMapSingleton;
import org.bofus.sumcompare.singletons.ExistingTargetFileObjectArraySingleton;
import org.bofus.sumcompare.singletons.MatchingFileHashMapSingleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportUtils {
	public static void createOutputExcel() throws IOException, SQLException, PropertyVetoException {
		Workbook workbook = new XSSFWorkbook();
		CreationHelper createHelper = workbook.getCreationHelper();

		// Create a Font for styling header cells
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());

		// Create a CellStyle with the font
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		// ==================================================
		// Create the WorkSheet for the files copied (or would be copied)
		// ==================================================
		Sheet copiedSheet = workbook.createSheet("Files Copied");
		// Create a Row
		Row copiedHeaderRow = copiedSheet.createRow(0);

		String[] copiedheaderColumnNames = { "Source File", "Target File" };
		// Create Header cells
		for (int i = 0; i < copiedheaderColumnNames.length; i++) {
			Cell cell = copiedHeaderRow.createCell(i);
			cell.setCellValue(copiedheaderColumnNames[i]);
			cell.setCellStyle(headerCellStyle);
		}

		// Add the rows for the file data
		int copiedRowCount = 1;
		for (String key : CopiedFileHashMapSingleton.getInstance().getMap().keySet()) {
			Row currentRow = copiedSheet.createRow(copiedRowCount);
			currentRow.createCell(0).setCellValue(key);
			currentRow.createCell(1).setCellValue(CopiedFileHashMapSingleton.getInstance().getMap().get(key));

			copiedRowCount++;
		}

		for (int i = 0; i < copiedheaderColumnNames.length; i++) {
			copiedSheet.autoSizeColumn(i);
		}

		// ==================================================
		// Create the WorkSheet for the files that are duplicates already in the target
		// directory
		// ==================================================
		Sheet targetDupeSheet = workbook.createSheet("Target Duplicate Files");
		// Create a Row
		Row targetDupeheaderRow = targetDupeSheet.createRow(0);

		String[] targetDupeheaderColumnNames = { "Current File", "Duplicate File", "CheckSum" };
		// Create Header cells
		for (int i = 0; i < targetDupeheaderColumnNames.length; i++) {
			Cell cell = targetDupeheaderRow.createCell(i);
			cell.setCellValue(targetDupeheaderColumnNames[i]);
			cell.setCellStyle(headerCellStyle);
		}

		// Add the rows for the file data
		for (int targetDupeArrayCount = 0; targetDupeArrayCount < ExistingTargetFileObjectArraySingleton.getInstance()
				.getArray().size(); targetDupeArrayCount++) {
			ExistingTargetFileObject thisObject = ExistingTargetFileObjectArraySingleton.getInstance().getArray()
					.get(targetDupeArrayCount);
			Row currentRow = targetDupeSheet.createRow(targetDupeArrayCount + 1);
			currentRow.createCell(0).setCellValue(thisObject.getCurrentFile());
			currentRow.createCell(1).setCellValue(thisObject.getExistingFile());
			currentRow.createCell(2).setCellValue(thisObject.getFileChecksum());
		}

		for (int i = 0; i < targetDupeheaderColumnNames.length; i++) {
			targetDupeSheet.autoSizeColumn(i);
		}

		// ==================================================
		// Create the WorkSheet for the files NOT copied
		// ==================================================
		Sheet notCopiedSheet = workbook.createSheet("Files Not Copied");
		// Create a Row
		Row notCopiedheaderRow = notCopiedSheet.createRow(0);

		String[] notCopiedheaderColumnNames = { "Source File", "Matching File in Target" };
		// Create Header cells
		for (int i = 0; i < notCopiedheaderColumnNames.length; i++) {
			Cell cell = notCopiedheaderRow.createCell(i);
			cell.setCellValue(notCopiedheaderColumnNames[i]);
			cell.setCellStyle(headerCellStyle);
		}

		// Add the rows for the file data
		int notCopiedRowCount = 1;
		for (String key : MatchingFileHashMapSingleton.getInstance().getMap().keySet()) {
			Row currentRow = notCopiedSheet.createRow(notCopiedRowCount);
			currentRow.createCell(0).setCellValue(key);
			currentRow.createCell(1).setCellValue(MatchingFileHashMapSingleton.getInstance().getMap().get(key));
			notCopiedRowCount++;
		}

		for (int i = 0; i < notCopiedheaderColumnNames.length; i++) {
			notCopiedSheet.autoSizeColumn(i);
		}

		// Write the output to a file
		FileOutputStream fileOut = new FileOutputStream("Copy_Output.xlsx");
		workbook.write(fileOut);
		fileOut.close();

		// Closing the workbook
		workbook.close();

	}

}
