package core;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class WordEvidenceWriter implements EvidenceWriter {

    @Override
    public File write(EvidenceModel model, File evidenceFolder, String outputBaseName) throws IOException {
        evidenceFolder.mkdirs();
        File outputFile = new File(evidenceFolder, outputBaseName + ".docx");

        try (XWPFDocument doc = new XWPFDocument()) {

            applyNarrowMargins(doc);

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText(outputBaseName);
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            // Tunables
            final int imageWidthPoints = 460;   // bigger screenshot
            final int indent = 200;             // tighter indent to use the extra width

            for (StepEntry entry : model.getEntries()) {
                XWPFParagraph desc = doc.createParagraph();
                desc.setIndentationLeft(indent);
                desc.setSpacingBefore(80);
                desc.setSpacingAfter(40);
                XWPFRun descRun = desc.createRun();
                descRun.setBold(true);
                descRun.setFontSize(11);
                descRun.setText("Step " + entry.getStepNumber() + ": " + entry.getDescription());

                XWPFParagraph imgPara = doc.createParagraph();
                imgPara.setIndentationLeft(indent);
                imgPara.setSpacingAfter(40);
                XWPFRun imgRun = imgPara.createRun();
                if (entry.getImageFile() != null && entry.getImageFile().exists()) {
                    try (var in = new java.io.FileInputStream(entry.getImageFile())) {
                        BufferedImage img = ImageIO.read(entry.getImageFile());
                        int emuWidth = Units.toEMU(imageWidthPoints);
                        int emuHeight = img != null
                                ? Units.toEMU((double) imageWidthPoints * img.getHeight() / img.getWidth())
                                : Units.toEMU(260);
                        imgRun.addPicture(in, org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG,
                                entry.getImageFile().getName(), emuWidth, emuHeight);
                    } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                        throw new IOException(e);
                    }
                }

                XWPFParagraph resultPara = doc.createParagraph();
                resultPara.setIndentationLeft(indent);
                resultPara.setSpacingAfter(100);
                XWPFRun resultRun = resultPara.createRun();
                resultRun.setBold(true);
                resultRun.setFontSize(10);
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

    /** Narrow margins (0.5in each side) so the wider screenshot fits without overflowing the page. */
    private void applyNarrowMargins(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr()
                ? doc.getDocument().getBody().getSectPr()
                : doc.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setLeft(BigInteger.valueOf(720));   // 0.5 inch (720 twips)
        pageMar.setRight(BigInteger.valueOf(720));
        pageMar.setTop(BigInteger.valueOf(720));
        pageMar.setBottom(BigInteger.valueOf(720));
    }
}