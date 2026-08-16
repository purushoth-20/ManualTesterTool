package core;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtil {

    private static final String SCREENSHOT_DIR = "evidence";

    public static String capture() throws Exception {

        // Ensure directory exists (you already create it manually – safe anyway)
        File dir = new File(SCREENSHOT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenshot = robot.createScreenCapture(screenRect);

        // ===== TIMESTAMP =====
        Graphics2D g2d = screenshot.createGraphics();

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(new Color(0, 0, 0, 180)); // semi-transparent black

        String timestamp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timestamp);

        int x = screenshot.getWidth() - textWidth - 20;
        int y = 30;

        // Optional background for readability
        g2d.fillRoundRect(x - 10, y - 20, textWidth + 20, 28, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.drawString(timestamp, x, y);

        g2d.dispose();
        // =====================

        String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(SCREENSHOT_DIR + "/" + fileName);

        ImageIO.write(screenshot, "png", outputFile);

        return outputFile.getAbsolutePath();
    }
}
