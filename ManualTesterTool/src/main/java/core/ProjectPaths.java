package core;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Figures out where the project root is, so the evidence folder always ends
 * up in the same predictable place (project_root/evidence) no matter what
 * IntelliJ's Run Configuration "Working directory" happens to be set to.
 *
 * If you'd rather pin it to an exact folder yourself, skip the auto-detection
 * entirely and just hardcode it, e.g.:
 *
 *     public static File evidenceFolder() {
 *         return new File("C:/Users/HP/IdeaProjects/ManualTesterTool/evidence");
 *     }
 */
public final class ProjectPaths {

    private ProjectPaths() {
    }

    public static File evidenceFolder() {
        File root = detectProjectRoot();
        File evidence = new File(root, "evidence");
        evidence.mkdirs();
        return evidence;
    }

    private static File detectProjectRoot() {
        try {
            File codeSource = new File(
                    ProjectPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // Running from IntelliJ: codeSource is .../target/classes
            // Running from a packaged jar: codeSource is .../target/ManualTesterTool.jar
            File dir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();

            if (dir.getName().equals("classes")) {
                dir = dir.getParentFile(); // -> target
            }
            if (dir.getName().equals("target")) {
                dir = dir.getParentFile(); // -> project root
            }
            return dir;
        } catch (URISyntaxException | NullPointerException e) {
            // Fall back to wherever the JVM was actually launched from
            return new File(System.getProperty("user.dir"));
        }
    }
}
