package core;

import java.io.File;

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

    // NEW: freeform comment, embedded below the screenshot in both writers.
    // Mandatory (enforced in CaptureToggle) when result == FAIL, optional otherwise.
    private String hint = "";

    // NEW: true when this entry is an Alt+click capture merged into the SAME
    // step as the previous entry (rather than a new step). Writers use this
    // to skip re-printing the step heading / step number for this entry.
    private final boolean continuation;

    public StepEntry(int stepNumber, String description, File imageFile, String timestamp, String duration) {
        this(stepNumber, description, imageFile, timestamp, duration, false);
    }

    // NEW overload — used for Alt+click merged captures. Existing 5-arg
    // constructor above is untouched and still works exactly as before.
    public StepEntry(int stepNumber, String description, File imageFile, String timestamp, String duration,
                     boolean continuation) {
        this.stepNumber = stepNumber;
        this.description = description;
        this.imageFile = imageFile;
        this.timestamp = timestamp;
        this.duration = duration;
        this.result = PENDING;
        this.continuation = continuation;
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

    // NEW
    public String getHint() {
        return hint;
    }

    // NEW
    public void setHint(String hint) {
        this.hint = (hint == null) ? "" : hint;
    }

    // NEW
    public boolean isContinuation() {
        return continuation;
    }
}
