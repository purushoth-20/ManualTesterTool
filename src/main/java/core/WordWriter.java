package core;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordWriter {

    private static XWPFDocument document;
    private static boolean initialized = false;

    private static String scenarioName;
    private static Path outputFile;

    private static final List<String> steps = new ArrayList<>();
    private static int currentStepIndex = 0;

    private static final String OUTPUT_DIR = "evidence";

    // ---------------- INIT ----------------
    public static void initFromFlow(String flowText) throws Exception {

        reset();

        scenarioName = extractScenario(flowText);
        steps.addAll(extractSteps(flowText));

        document = new XWPFDocument();

        // Scenario heading
        XWPFParagraph heading = document.createParagraph();
        heading.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun run = heading.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText("Scenario: " + scenarioName);

        initialized = true;
    }

    // ---------------- ADD SCREENSHOT ----------------
    public static void addScreenshot(String imagePath) throws Exception {

        if (!initialized) return;

        String stepText;
        if (currentStepIndex < steps.size()) {
            stepText = steps.get(currentStepIndex);
        } else {
            stepText = "Execution continuation";
        }

        int stepNumber = currentStepIndex + 1;

        // Step text
        XWPFParagraph stepPara = document.createParagraph();
        XWPFRun stepRun = stepPara.createRun();
        stepRun.setBold(true);
        stepRun.setText("Step " + stepNumber + ": " + stepText);

        // Image
        XWPFParagraph imgPara = document.createParagraph();
        imgPara.setAlignment(ParagraphAlignment.CENTER);

        try (FileInputStream fis = new FileInputStream(imagePath)) {
            XWPFRun imgRun = imgPara.createRun();
            imgRun.addPicture(
                    fis,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    imagePath,
                    Units.toEMU(500),
                    Units.toEMU(280)
            );
        }

        currentStepIndex++;
    }

    // ---------------- FINISH & SAVE ----------------
    public static void finish(String finalStatus) throws Exception {

        if (!initialized) return;

        Files.createDirectories(Path.of(OUTPUT_DIR));

        String safeName = scenarioName.replaceAll("[^a-zA-Z0-9 _-]", "");
        String fileName = safeName + "_" + finalStatus + ".docx";

        outputFile = Path.of(OUTPUT_DIR, fileName);

        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            document.write(fos);
        }

        document.close();
        reset();
    }

    // ---------------- HELPERS ----------------
    private static String extractScenario(String text) throws Exception {
        Pattern p = Pattern.compile("(?i)scenario\\s*:\\s*(.+)");
        Matcher m = p.matcher(text);
        if (!m.find()) {
            throw new Exception("Scenario not found");
        }
        return m.group(1).trim();
    }

    private static List<String> extractSteps(String text) {
        List<String> result = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (!line.toLowerCase().startsWith("scenario")) {
                if (!line.trim().isEmpty()) {
                    result.add(line.trim());
                }
            }
        }
        return result;
    }

    private static void reset() {
        initialized = false;
        currentStepIndex = 0;
        steps.clear();
        document = null;
        outputFile = null;
    }
}
