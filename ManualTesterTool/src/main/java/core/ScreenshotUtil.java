package core;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ScreenshotUtil {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int PADDING = 10;

    private ScreenshotUtil() {
    }

    /** Captures the full screen. */
    public static BufferedImage captureScreen() throws AWTException {
        Robot robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return robot.createScreenCapture(new Rectangle(screenSize));
    }

    public static String currentTimestamp() {
        return TIME_FORMAT.format(new Date());
    }

    /** Formats a millisecond duration as e.g. "3s" or "1m 05s". */
    public static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }

    /**
     * Draws the timestamp in the top-right corner and the duration in the
     * bottom-right corner of the image, each on a semi-transparent badge
     * so it stays legible over any background.
     */
    public static BufferedImage embedTimestampAndDuration(BufferedImage source, String timestamp, String durationLabel) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, null);

        Font font = new Font("SansSerif", Font.BOLD, 18);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        drawBadge(g, fm, "Time: " + timestamp, result.getWidth(), 0, true);
        drawBadge(g, fm, "Duration: " + durationLabel, result.getWidth(), result.getHeight(), false);

        g.dispose();
        return result;
    }

    private static void drawBadge(Graphics2D g, FontMetrics fm, String text, int imgWidth, int imgHeight, boolean top) {
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        int x = imgWidth - textWidth - PADDING * 2;
        int y = top ? PADDING : imgHeight - textHeight - PADDING;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x - PADDING / 2, y, textWidth + PADDING, textHeight + PADDING / 2, 8, 8);

        g.setColor(Color.WHITE);
        g.drawString(text, x, y + fm.getAscent() + PADDING / 4);
    }

    public static File saveImage(BufferedImage image, File outputFile) throws IOException {
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, "png", outputFile);
        return outputFile;
    }
}
