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

            final int imageWidthPoints = 460;
            final int indent = 200;

            for (StepEntry entry : model.getEntries()) {
                XWPFParagraph desc = doc.createParagraph();
                desc.setIndentationLeft(indent);
                desc.setSpacingBefore(80);
                desc.setSpacingAfter(40);
                XWPFRun descRun = desc.createRun();
                descRun.setBold(true);
                descRun.setFontSize(11);
                descRun.setText(stepLabel(entry));

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

                // Only show a Result line once it's actually been set (Pass/Fail) —
                // skip it entirely while still PENDING.
                if (!StepEntry.PENDING.equals(entry.getResult())) {
                    XWPFParagraph resultPara = doc.createParagraph();
                    resultPara.setIndentationLeft(indent);
                    resultPara.setSpacingAfter(100);
                    XWPFRun resultRun = resultPara.createRun();
                    resultRun.setBold(true);
                    resultRun.setFontSize(10);
                    resultRun.setText("Result: " + entry.getResult());
                    if (StepEntry.PASS.equals(entry.getResult())) {
                        resultRun.setColor("2E7D32");
                    } else {
                        resultRun.setColor("C62828");
                    }
                }
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                doc.write(out);
            }
        }

        return outputFile;
    }

    /**
     * Builds the step heading. If the description is just the auto-generated
     * fallback ("Step 3"), avoid the "Step 3: Step 3" duplication and show
     * plain "Step 3:" instead. Real Gherkin descriptions still show in full,
     * e.g. "Step 1: Given I launch the app".
     */
    private String stepLabel(StepEntry entry) {
        String fallback = "Step " + entry.getStepNumber();
        if (fallback.equals(entry.getDescription())) {
            return fallback + ":";
        }
        return fallback + ": " + entry.getDescription();
    }

    private void applyNarrowMargins(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr()
                ? doc.getDocument().getBody().getSectPr()
                : doc.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setLeft(BigInteger.valueOf(720));
        pageMar.setRight(BigInteger.valueOf(720));
        pageMar.setTop(BigInteger.valueOf(720));
        pageMar.setBottom(BigInteger.valueOf(720));
    }
}