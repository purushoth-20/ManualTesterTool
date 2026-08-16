package core;

import java.io.File;

/**
 * Represents a single captured evidence step:
 * a Gherkin (or fallback "Step N") description, its screenshot,
 * the timestamp/duration burned into that screenshot, and the
 * pass/fail verdict chosen by the user.
 */
public class StepEntry {

    public static final String PENDING = "PENDING";
    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";

    private final int stepNumber;
    private final String description;
    private final File imageFile;
    private final String timestamp;
    private final String duration;
    private String result;

    public StepEntry(int stepNumber, String description, File imageFile, String timestamp, String duration) {
        this.stepNumber = stepNumber;
        this.description = description;
        this.imageFile = imageFile;
        this.timestamp = timestamp;
        this.duration = duration;
        this.result = PENDING;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getDescription() {
        return description;
    }

    public File getImageFile() {
        return imageFile;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDuration() {
        return duration;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
