package core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class GherkinParser {

    private static final Pattern STEP_LINE =
            Pattern.compile("^\\s*(Given|When|Then|And|But)\\b.*", Pattern.CASE_INSENSITIVE);

    private GherkinParser() {
    }

    public static boolean isBlank(String gherkinText) {
        return gherkinText == null || gherkinText.trim().isEmpty();
    }

    public static boolean isValidGherkin(String gherkinText) {
        if (isBlank(gherkinText)) {
            return false;
        }
        for (String line : gherkinText.split("\\r?\\n")) {
            if (STEP_LINE.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    public static List<String> parseSteps(String gherkinText) {
        List<String> steps = new ArrayList<>();
        if (isBlank(gherkinText)) {
            return steps;
        }
        for (String line : gherkinText.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (STEP_LINE.matcher(trimmed).matches()) {
                steps.add(trimmed);
            }
        }
        return steps;
    }
}