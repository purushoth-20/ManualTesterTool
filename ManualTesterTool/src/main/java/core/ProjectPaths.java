package core;

import java.io.File;
import java.net.URISyntaxException;

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

            File dir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();

            if (dir.getName().equals("classes")) {
                dir = dir.getParentFile();
            }
            if (dir.getName().equals("target")) {
                dir = dir.getParentFile();
            }
            return dir;
        } catch (URISyntaxException | NullPointerException e) {
            return new File(System.getProperty("user.dir"));
        }
    }
}