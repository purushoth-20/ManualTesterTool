package ui;

import core.EvidenceModel;
import core.EvidenceWriter;
import core.ScreenshotUtil;
import core.StepEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Minimal, draggable floating toggle.
 *  - Small footprint so it doesn't block the page under test.
 *  - Drag anywhere on the bar to reposition (out of the way of what you're validating).
 *  - Step description shown as a tooltip on hover, not as fixed on-screen text.
 *  - One click on the camera icon captures + embeds + saves; the result dropdown
 *    always applies to the most recently captured step.
 */
public class CaptureToggle extends JWindow {

    private final EvidenceModel model;
    private final File evidenceFolder = core.ProjectPaths.evidenceFolder();
    private final EvidenceWriter writer;

    private long lastCaptureTime;
    private int stepCounter = 0;
    private Point dragOffset;

    private final JButton captureButton = new JButton("\uD83D\uDCF8");
    private final JComboBox<String> resultBox = new JComboBox<>(new String[]{"\u2013", "\u2714 Pass", "\u2716 Fail"});
    private final JButton finishButton = new JButton("\u2713");
    private final JLabel countLabel = new JLabel("0");

    public CaptureToggle(EvidenceModel model) {
        this.model = model;
        this.writer = EvidenceWriter.forModel(model);
        this.lastCaptureTime = System.currentTimeMillis();

        setAlwaysOnTop(true);
        setLayout(new BorderLayout());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
        bar.setBackground(new Color(32, 32, 32));
        bar.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));

        Font smallFont = new Font("SansSerif", Font.PLAIN, 12);

        captureButton.setFont(smallFont.deriveFont(14f));
        captureButton.setMargin(new java.awt.Insets(1, 4, 1, 4));
        captureButton.setFocusable(false);
        captureButton.setToolTipText(nextStepTooltip());
        captureButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        countLabel.setForeground(Color.LIGHT_GRAY);
        countLabel.setFont(smallFont);

        resultBox.setFont(smallFont);
        resultBox.setEnabled(false);
        resultBox.setFocusable(false);
        resultBox.setPreferredSize(new Dimension(72, 22));

        finishButton.setFont(smallFont.deriveFont(13f));
        finishButton.setMargin(new java.awt.Insets(1, 4, 1, 4));
        finishButton.setFocusable(false);
        finishButton.setToolTipText("Finish & save");
        finishButton.setForeground(new Color(70, 160, 90));
        finishButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Small drag handle so it's obvious you can move it, without hogging space
        JLabel dragHandle = new JLabel("\u2630");
        dragHandle.setForeground(Color.GRAY);
        dragHandle.setCursor(new Cursor(Cursor.MOVE_CURSOR));
        dragHandle.setToolTipText("Drag to move");

        bar.add(dragHandle);
        bar.add(countLabel);
        bar.add(captureButton);
        bar.add(resultBox);
        bar.add(finishButton);

        add(bar, BorderLayout.CENTER);

        captureButton.addActionListener(e -> onCapture());
        resultBox.addActionListener(e -> onResultChange());
        finishButton.addActionListener(e -> onFinish());

        // Dragging: works from anywhere on the bar (not just the handle),
        // since the whole toggle is tiny anyway.
        MouseAdapter dragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        };
        MouseMotionAdapter moveListener = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset == null) {
                    return;
                }
                Point current = getLocation();
                setLocation(current.x + e.getX() - dragOffset.x,
                        current.y + e.getY() - dragOffset.y);
            }
        };
        bar.addMouseListener(dragListener);
        bar.addMouseMotionListener(moveListener);
        dragHandle.addMouseListener(dragListener);
        dragHandle.addMouseMotionListener(moveListener);

        pack();
        // Default: small, top-right corner, out of the way
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screen.width - getWidth() - 20, 20);

        bar.setToolTipText("Saving to: " + evidenceFolder.getAbsolutePath());
    }

    private String nextStepTooltip() {
        return "Capture \u2014 next: " + model.describeStep(stepCounter + 1);
    }

    private void onCapture() {
        captureButton.setEnabled(false);
        try {
            // Hide the toggle itself so it doesn't appear in its own screenshot
            setVisible(false);
            Thread.sleep(150);

            BufferedImage raw = ScreenshotUtil.captureScreen();

            long now = System.currentTimeMillis();
            String timestamp = ScreenshotUtil.currentTimestamp();
            String durationLabel = ScreenshotUtil.formatDuration(now - lastCaptureTime);
            lastCaptureTime = now;

            BufferedImage annotated = ScreenshotUtil.embedTimestampAndDuration(raw, timestamp, durationLabel);

            stepCounter++;
            String description = model.describeStep(stepCounter);

            File imagesDir = new File(evidenceFolder, model.getFileName() + "_images");
            File imageFile = new File(imagesDir, "step" + stepCounter + ".png");
            ScreenshotUtil.saveImage(annotated, imageFile);

            StepEntry entry = new StepEntry(stepCounter, description, imageFile, timestamp, durationLabel);
            model.addEntry(entry);

            writer.write(model, evidenceFolder);

            setVisible(true);
            countLabel.setText(String.valueOf(stepCounter));
            resultBox.setSelectedIndex(0);
            resultBox.setEnabled(true);

            boolean gherkinExhausted = model.hasGherkinSteps() && model.isComplete();
            captureButton.setEnabled(!gherkinExhausted);
            captureButton.setToolTipText(gherkinExhausted ? "All steps captured" : nextStepTooltip());

        } catch (Exception ex) {
            setVisible(true);
            captureButton.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Capture failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onResultChange() {
        StepEntry last = model.getLastEntry();
        if (last == null) {
            return;
        }
        int selected = resultBox.getSelectedIndex();
        if (selected == 1) {
            last.setResult(StepEntry.PASS);
        } else if (selected == 2) {
            last.setResult(StepEntry.FAIL);
        } else {
            return;
        }
        try {
            writer.write(model, evidenceFolder);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onFinish() {
        try {
            File output = writer.write(model, evidenceFolder);
            JOptionPane.showMessageDialog(this,
                    "Evidence saved to:\n" + output.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        dispose();
    }
}
