package ui;

import core.EvidenceModel;
import core.EvidenceWriter;
import core.ExcelEvidenceWriter;
import core.ProjectPaths;
import core.ScreenshotUtil;
import core.StepEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
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

public class CaptureToggle extends JWindow {

    private final EvidenceModel model;
    private final File evidenceFolder = ProjectPaths.evidenceFolder();
    private final EvidenceWriter writer;

    /** Non-null when this test case should be written as a sheet inside a shared workbook. */
    private final File sharedWorkbookFile;

    private long lastCaptureTime;
    private int stepCounter = 0;
    private Point dragOffset;

    private final JButton captureButton = new JButton("\uD83D\uDCF8");
    private final JButton retakeButton = new JButton("\u21BA");
    private final JComboBox<String> resultBox = new JComboBox<>(new String[]{"\u2013", "\u2714 Pass", "\u2716 Fail"});
    private final JButton finishButton = new JButton("\u2713");
    private final JLabel countLabel = new JLabel("0");

    public CaptureToggle(EvidenceModel model) {
        this(model, null);
    }

    public CaptureToggle(EvidenceModel model, File sharedWorkbookFile) {
        this.model = model;
        this.writer = EvidenceWriter.forModel(model);
        this.sharedWorkbookFile = sharedWorkbookFile;
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

        retakeButton.setFont(smallFont.deriveFont(14f));
        retakeButton.setMargin(new java.awt.Insets(1, 4, 1, 4));
        retakeButton.setFocusable(false);
        retakeButton.setToolTipText("Undo last capture (asks to confirm)");
        retakeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        retakeButton.setEnabled(false);

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

        JLabel dragHandle = new JLabel("\u2630");
        dragHandle.setForeground(Color.GRAY);
        dragHandle.setCursor(new Cursor(Cursor.MOVE_CURSOR));
        dragHandle.setToolTipText("Drag to move");

        bar.add(dragHandle);
        bar.add(countLabel);
        bar.add(captureButton);
        bar.add(retakeButton);
        bar.add(resultBox);
        bar.add(finishButton);

        add(bar, BorderLayout.CENTER);

        captureButton.addActionListener(e -> onCapture());
        retakeButton.addActionListener(e -> onRetake());
        resultBox.addActionListener(e -> onResultChange());
        finishButton.addActionListener(e -> onFinish());

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
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screen.width - getWidth() - 20, 20);

        File target = sharedWorkbookFile != null ? sharedWorkbookFile : evidenceFolder;
        bar.setToolTipText("Saving to: " + target.getAbsolutePath());
    }

    private String nextStepTooltip() {
        return "Capture \u2014 next: " + model.describeStep(stepCounter + 1);
    }

    private void onCapture() {
        captureButton.setEnabled(false);
        try {
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

            saveInterim();

            setVisible(true);
            countLabel.setText(String.valueOf(stepCounter));
            resultBox.setSelectedIndex(0);
            resultBox.setEnabled(true);
            retakeButton.setEnabled(true);

            // No cap: once Gherkin steps run out, describeStep() falls back to
            // "Step N" on its own, so Capture stays enabled indefinitely.
            captureButton.setEnabled(true);
            captureButton.setToolTipText(nextStepTooltip());

        } catch (Exception ex) {
            setVisible(true);
            captureButton.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Capture failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Discards the most recent capture (image + entry) — asks to confirm first since it's not reversible. */
    private void onRetake() {
        StepEntry last = model.getLastEntry();
        if (last == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Discard the last capture (Step " + last.getStepNumber() + ")?\nThis can't be undone.",
                "Confirm Retake", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        StepEntry removed = model.removeLastEntry();
        if (removed.getImageFile() != null && removed.getImageFile().exists()) {
            removed.getImageFile().delete();
        }
        stepCounter--;
        countLabel.setText(String.valueOf(stepCounter));

        boolean hasEntries = !model.getEntries().isEmpty();
        resultBox.setEnabled(hasEntries);
        resultBox.setSelectedIndex(0);
        retakeButton.setEnabled(hasEntries);
        captureButton.setEnabled(true);
        captureButton.setToolTipText(nextStepTooltip());

        try {
            saveInterim();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            saveInterim();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Interim autosave, under the plain (no Pass/Fail suffix) name, during capture. */
    private void saveInterim() throws IOException {
        if (sharedWorkbookFile != null) {
            ((ExcelEvidenceWriter) writer).writeAsSheet(model, sharedWorkbookFile, model.getFileName(), null);
        } else {
            writer.write(model, evidenceFolder, model.getFileName());
        }
    }

    private void onFinish() {
        String baseName = model.getFileName();
        String suffix = model.hasAnyFailure() ? "Fail" : "Pass";
        String finalName = baseName + " - " + suffix;

        try {
            File output;
            if (sharedWorkbookFile != null) {
                output = ((ExcelEvidenceWriter) writer).writeAsSheet(model, sharedWorkbookFile, finalName, baseName);
            } else {
                output = writer.write(model, evidenceFolder, finalName);
                File interim = new File(evidenceFolder, baseName + model.fileExtension());
                if (interim.exists() && !interim.getName().equals(output.getName())) {
                    interim.delete();
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Evidence saved to:\n" + output.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);

            promptNextAction(output);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void promptNextAction(File justSaved) {
        boolean isExcel = model.getFormat() == EvidenceModel.Format.EXCEL;
        Object[] options = isExcel
                ? new Object[]{"Add next case to this workbook", "Start new Excel file", "Done"}
                : new Object[]{"Start new document", "Done"};

        int choice = JOptionPane.showOptionDialog(this,
                "Start another test case?", "Continue?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[options.length - 1]);

        dispose();

        if (isExcel) {
            if (choice == 0) {
                File workbookToReuse = (sharedWorkbookFile != null) ? sharedWorkbookFile : justSaved;
                SwingUtilities.invokeLater(() -> new MainFrame(workbookToReuse).setVisible(true));
            } else if (choice == 1) {
                SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
            }
        } else {
            if (choice == 0) {
                SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
            }
        }
    }
}