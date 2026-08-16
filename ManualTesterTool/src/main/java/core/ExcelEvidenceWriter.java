package core;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class ExcelEvidenceWriter implements EvidenceWriter {

    private static final int IMAGE_ROW_HEIGHT = 220;

    @Override
    public File write(EvidenceModel model, File evidenceFolder) throws IOException {
        evidenceFolder.mkdirs();
        File outputFile = new File(evidenceFolder, model.getFileName() + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Evidence");
            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 45 * 256);
            sheet.setColumnWidth(2, 45 * 256);
            sheet.setColumnWidth(3, 12 * 256);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] headers = {"Step", "Description", "Screenshot", "Result"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            XSSFDrawing drawing = sheet.createDrawingPatriarch();

            int rowIdx = 1;
            for (StepEntry entry : model.getEntries()) {
                Row row = sheet.createRow(rowIdx);
                row.setHeightInPoints(IMAGE_ROW_HEIGHT);

                row.createCell(0).setCellValue(entry.getStepNumber());
                row.createCell(1).setCellValue(entry.getDescription());

                CellStyle resultStyle = wb.createCellStyle();
                Font resultFont = wb.createFont();
                resultFont.setBold(true);
                if (StepEntry.PASS.equals(entry.getResult())) {
                    resultFont.setColor(IndexedColors.GREEN.getIndex());
                } else if (StepEntry.FAIL.equals(entry.getResult())) {
                    resultFont.setColor(IndexedColors.RED.getIndex());
                }
                resultStyle.setFont(resultFont);
                Cell resultCell = row.createCell(3);
                resultCell.setCellValue(entry.getResult());
                resultCell.setCellStyle(resultStyle);

                if (entry.getImageFile() != null && entry.getImageFile().exists()) {
                    byte[] bytes = Files.readAllBytes(entry.getImageFile().toPath());
                    int pictureIdx = wb.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_PNG);

                    XSSFClientAnchor anchor = new XSSFClientAnchor();
                    anchor.setCol1(2);
                    anchor.setRow1(rowIdx);
                    anchor.setCol2(3);
                    anchor.setRow2(rowIdx + 1);
                    drawing.createPicture(anchor, pictureIdx);
                }

                rowIdx++;
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                wb.write(out);
            }
        }

        return outputFile;
    }
}
