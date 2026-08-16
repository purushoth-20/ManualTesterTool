package core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses Gherkin step text into a list of step descriptions.
 *
 * Rules (per spec):
 *  - Blank/empty input is ALLOWED -> caller should treat as "no Gherkin" (fallback Step N mode).
 *  - Non-blank input MUST contain at least one valid Gherkin keyword line
 *    (Given/When/Then/And/But), otherwise it is INVALID and the user must be
 *    re-prompted -- the app must not silently proceed.
 */
public final class GherkinParser {

    private static final Pattern STEP_LINE =
            Pattern.compile("^\\s*(Given|When|Then|And|But)\\b.*", Pattern.CASE_INSENSITIVE);

    private GherkinParser() {
    }

    public static boolean isBlank(String gherkinText) {
        return gherkinText == null || gherkinText.trim().isEmpty();
    }

    /** True if the text has at least one recognizable Given/When/Then/And/But line. */
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

    /**
     * Extracts each Given/When/Then/And/But line as a separate step description,
     * in the order they appear. Non-step lines (blank lines, comments, feature/
     * scenario headers) are ignored.
     */
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
