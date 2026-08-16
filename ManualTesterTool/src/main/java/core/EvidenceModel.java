package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvidenceModel {

    public enum Format {
        WORD, EXCEL
    }

    private final String fileName;
    private final Format format;

    /** Null/empty means "no Gherkin given" -> fallback Step 1, Step 2... mode. */
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

    public StepEntry getLastEntry() {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    /**
     * Description for the NEXT capture (1-indexed step count already taken into entries.size()).
     * Uses the matching Gherkin line if available, otherwise falls back to "Step N".
     */
    public String describeStep(int stepNumberOneIndexed) {
        int idx = stepNumberOneIndexed - 1;
        if (hasGherkinSteps() && idx < gherkinSteps.size()) {
            return gherkinSteps.get(idx);
        }
        return "Step " + stepNumberOneIndexed;
    }

    /** True once every parsed Gherkin step has a captured entry (only meaningful when Gherkin was given). */
    public boolean isComplete() {
        return hasGherkinSteps() && entries.size() >= gherkinSteps.size();
    }

    public String fileExtension() {
        return format == Format.WORD ? ".docx" : ".xlsx";
    }
}
