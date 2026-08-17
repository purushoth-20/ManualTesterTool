package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvidenceModel {

    public enum Format {
        WORD, EXCEL
    }

    private String fileName;
    private final Format format;
    private final List<String> gherkinSteps;
    private final List<StepEntry> entries = new ArrayList<>();

    public EvidenceModel(String fileName, Format format, List<String> gherkinSteps) {
        this.fileName = fileName;
        this.format = format;
        this.gherkinSteps = (gherkinSteps == null) ? Collections.emptyList() : gherkinSteps;
    }

    public String getFileName() {
        return fileName;
    }

    public Format getFormat() {
        return format;
    }

    public boolean hasGherkinSteps() {
        return !gherkinSteps.isEmpty();
    }

    public List<String> getGherkinSteps() {
        return gherkinSteps;
    }

    public List<StepEntry> getEntries() {
        return entries;
    }

    public void addEntry(StepEntry entry) {
        entries.add(entry);
    }

    public StepEntry removeLastEntry() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.remove(entries.size() - 1);
    }

    public StepEntry getLastEntry() {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    /**
     * Description for a given capture number. Uses the matching Gherkin line
     * while available; once Gherkin steps run out, falls back to "Step N"
     * indefinitely (no cap, no "exhausted" state).
     */
    public String describeStep(int stepNumberOneIndexed) {
        int idx = stepNumberOneIndexed - 1;
        if (hasGherkinSteps() && idx < gherkinSteps.size()) {
            return gherkinSteps.get(idx);
        }
        return "Step " + stepNumberOneIndexed;
    }

    public boolean hasAnyFailure() {
        return entries.stream().anyMatch(e -> StepEntry.FAIL.equals(e.getResult()));
    }

    public String fileExtension() {
        return format == Format.WORD ? ".docx" : ".xlsx";
    }
}