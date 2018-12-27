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
import org.bofus.sumcompare.singletons.CopiedFileHashMapSingleton;
import org.bofus.sumcompare.singletons.MatchingFileHashMapSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportUtils
{
	private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

	public static void createOutputExcel() throws IOException, SQLException, PropertyVetoException
	{
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

		//==================================================
		// Create the WorkSheet for the files copied (or would be copied)
		//==================================================
		Sheet copiedSheet = workbook.createSheet("Files Copied");
		// Create a Row
		Row copiedHeaderRow = copiedSheet.createRow(0);

		String[] copiedheaderColumnNames = { "Source File", "Target File" };
		// Create Header cells
		for (int i = 0; i < copiedheaderColumnNames.length; i++)
		{
			Cell cell = copiedHeaderRow.createCell(i);
			cell.setCellValue(copiedheaderColumnNames[i]);
			cell.setCellStyle(headerCellStyle);
		}

		// Add the rows for the file data
		int copiedRowCount = 1;
		for (String key : CopiedFileHashMapSingleton.getInstance().getMap().keySet())
		{
			Row currentRow = copiedSheet.createRow(copiedRowCount);
			currentRow.createCell(0).setCellValue(key);
			currentRow.createCell(1).setCellValue(CopiedFileHashMapSingleton.getInstance().getMap().get(key));
			
//			logger.debug("===================================================================================");
//			logger.debug("Source File: " + key);
//			logger.debug("Target File: " + CopiedFileHashMapSingleton.getInstance().getMap().get(key));
//			logger.debug("===================================================================================");
			copiedRowCount++;
		}

		for (int i = 0; i < copiedheaderColumnNames.length; i++)
		{
			copiedSheet.autoSizeColumn(i);
		}

		
		
		
		//==================================================
		// Create the WorkSheet for the files NOT copied
		//==================================================
		Sheet notCopiedSheet = workbook.createSheet("Files Not Copied");
		// Create a Row
		Row notCopiedheaderRow = notCopiedSheet.createRow(0);

		String[] notCopiedheaderColumnNames = { "Source File", "Matching File in Target" };
		// Create Header cells
		for (int i = 0; i < notCopiedheaderColumnNames.length; i++)
		{
			Cell cell = notCopiedheaderRow.createCell(i);
			cell.setCellValue(notCopiedheaderColumnNames[i]);
			cell.setCellStyle(headerCellStyle);
		}

		// Add the rows for the file data
		int notCopiedRowCount = 1;
		for (String key : MatchingFileHashMapSingleton.getInstance().getMap().keySet())
		{
			Row currentRow = notCopiedSheet.createRow(notCopiedRowCount);
			currentRow.createCell(0).setCellValue(key);
			currentRow.createCell(1).setCellValue(MatchingFileHashMapSingleton.getInstance().getMap().get(key));
			
//			logger.debug("===================================================================================");
//			logger.debug("Source File: " + key);
//			logger.debug("Target File: " + MatchingFileHashMapSingleton.getInstance().getMap().get(key));
//			logger.debug("===================================================================================");
			notCopiedRowCount++;
		}


		for (int i = 0; i < notCopiedheaderColumnNames.length; i++)
		{
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
