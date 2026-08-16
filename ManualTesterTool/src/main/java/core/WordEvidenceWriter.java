package core;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WordEvidenceWriter implements EvidenceWriter {

    @Override
    public File write(EvidenceModel model, File evidenceFolder) throws IOException {
        evidenceFolder.mkdirs();
        File outputFile = new File(evidenceFolder, model.getFileName() + ".docx");

        try (XWPFDocument doc = new XWPFDocument()) {

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText(model.getFileName());
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            for (StepEntry entry : model.getEntries()) {
                // Step description (indented)
                XWPFParagraph desc = doc.createParagraph();
                desc.setIndentationLeft(400);
                desc.setSpacingBefore(200);
                XWPFRun descRun = desc.createRun();
                descRun.setBold(true);
                descRun.setFontSize(12);
                descRun.setText(entry.getStepNumber() + ". " + entry.getDescription());

                // Screenshot (indented to match)
                XWPFParagraph imgPara = doc.createParagraph();
                imgPara.setIndentationLeft(400);
                XWPFRun imgRun = imgPara.createRun();
                if (entry.getImageFile() != null && entry.getImageFile().exists()) {
                    try (var in = new java.io.FileInputStream(entry.getImageFile())) {
                        BufferedImage img = ImageIO.read(entry.getImageFile());
                        int emuWidth = Units.toEMU(420);
                        int emuHeight = img != null
                                ? Units.toEMU(420.0 * img.getHeight() / img.getWidth())
                                : Units.toEMU(240);
                        imgRun.addPicture(in, org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG,
                                entry.getImageFile().getName(), emuWidth, emuHeight);
                    } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                        throw new IOException(e);
                    }
                }

                // Result line (indented)
                XWPFParagraph resultPara = doc.createParagraph();
                resultPara.setIndentationLeft(400);
                resultPara.setSpacingAfter(200);
                XWPFRun resultRun = resultPara.createRun();
                resultRun.setBold(true);
                resultRun.setText("Result: " + entry.getResult());
                if (StepEntry.PASS.equals(entry.getResult())) {
                    resultRun.setColor("2E7D32");
                } else if (StepEntry.FAIL.equals(entry.getResult())) {
                    resultRun.setColor("C62828");
                } else {
                    resultRun.setColor("757575");
                }
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                doc.write(out);
            }
        }

        return outputFile;
    }
}
