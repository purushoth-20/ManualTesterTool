package ui;

import core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CaptureToggle extends JWindow {

    private final EvidenceModel model;
    private final File evidenceFolder = ProjectPaths.evidenceFolder();
    private final EvidenceWriter writer;

    /** Non-null when this test case should be written as a sheet inside a shared workbook. */
    private final File sharedWorkbookFile;

    private long lastCaptureTime;
    private int stepCounter = 0;
    private Point dragOffset;

    // NEW: tracks whether the Alt key is physically held, app-wide, regardless
    // of which component currently has keyboard focus. Read at the moment
    // Capture's ActionEvent fires — see onCapture().
    private static volatile boolean altHeld = false;
    private static boolean altDispatcherInstalled = false;

    private final JButton captureButton = new JButton("\uD83D\uDCF8");
    private final JButton retakeButton = new JButton("\u21BA");
    private final JComboBox<String> resultBox = new JComboBox<>(new String[]{"\u2013", "\u2714 Pass", "\u2716 Fail"});
    private final JButton finishButton = new JButton("\u2713");
    private final JLabel countLabel = new JLabel("0");

    // NEW: hint/comment field for the most recent capture — same "only the
    // last entry is editable from the toolbar" pattern resultBox already uses.
    private final JTextField hintField = new JTextField();

    public CaptureToggle(EvidenceModel model) {
        this(model, null);
    }

    public CaptureToggle(EvidenceModel model, File sharedWorkbookFile) {
        this.model = model;
        this.writer = EvidenceWriter.forModel(model);
        this.sharedWorkbookFile = sharedWorkbookFile;
        this.lastCaptureTime = System.currentTimeMillis();

        installAltKeyTracker(); // NEW

        setAlwaysOnTop(true);
        setLayout(new BorderLayout());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
        bar.setBackground(new Color(32, 32, 32));
        bar.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));

        Font smallFont = new Font("SansSerif", Font.PLAIN, 12);

        captureButton.setFont(smallFont.deriveFont(14f));
        captureButton.setMargin(new Insets(1, 4, 1, 4));
        captureButton.setFocusable(false);
        captureButton.setToolTipText(nextStepTooltip());
        captureButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        retakeButton.setFont(smallFont.deriveFont(14f));
        retakeButton.setMargin(new Insets(1, 4, 1, 4));
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

        // NEW: hint field setup
        hintField.setFont(smallFont);
        hintField.setEnabled(false);
        hintField.setPreferredSize(new Dimension(110, 22));
        hintField.setToolTipText("Hint (required if Fail)");
        hintField.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        finishButton.setFont(smallFont.deriveFont(13f));
        finishButton.setMargin(new Insets(1, 4, 1, 4));
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
        bar.add(hintField); // NEW
        bar.add(finishButton);

        add(bar, BorderLayout.CENTER);

        captureButton.addActionListener(e -> onCapture());
        retakeButton.addActionListener(e -> onRetake());
        resultBox.addActionListener(e -> onResultChange());
        finishButton.addActionListener(e -> onFinish());

        // NEW: persist hint text onto the last entry as the user types, and
        // relax the red "required" border the moment it's no longer blank.
        hintField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void sync() {
                StepEntry last = model.getLastEntry();
                if (last == null) return;
                last.setHint(hintField.getText());
                refreshHintBorder(last);
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { sync(); }
        });

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

    // NEW: installs a single, app-wide key dispatcher (once) that tracks
    // whether Alt is currently held, independent of focus. This is what lets
    // "Alt + click Capture" be detected even though captureButton itself
    // never receives key events directly.
    private static synchronized void installAltKeyTracker() {
        if (altDispatcherInstalled) {
            return;
        }
        altDispatcherInstalled = true;
        KeyEventDispatcher dispatcher = e -> {
            if (e.getKeyCode() == KeyEvent.VK_ALT) {
                altHeld = (e.getID() == KeyEvent.KEY_PRESSED);
            }
            return false; // never consume — let the event propagate normally
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    private String nextStepTooltip() {
        return "Capture \u2014 next: " + model.describeStep(stepCounter + 1);
    }

    private void onCapture() {
        captureButton.setEnabled(false);
        try {
            // NEW: Alt+click merges this capture into the CURRENT step instead
            // of advancing — but only while Gherkin was parsed successfully.
            // In fallback (no-Gherkin) mode, every capture always advances,
            // exactly as before.
            boolean mergeIntoCurrentStep = model.hasGherkinSteps() && altHeld && stepCounter > 0;

            setVisible(false);
            Thread.sleep(150);

            BufferedImage raw = ScreenshotUtil.captureScreen();
            long now = System.currentTimeMillis();
            String timestamp = ScreenshotUtil.currentTimestamp();
            String durationLabel = ScreenshotUtil.formatDuration(now - lastCaptureTime);
            lastCaptureTime = now;

            BufferedImage annotated = ScreenshotUtil.embedTimestampAndDuration(raw, timestamp, durationLabel);

            int effectiveStepNumber;
            String description;
            if (mergeIntoCurrentStep) {
                effectiveStepNumber = stepCounter; // stays on the same step, NOT stepCounter + 1
                description = model.describeStep(stepCounter);
            } else {
                stepCounter++;
                effectiveStepNumber = stepCounter;
                description = model.describeStep(stepCounter);
            }

            File imagesDir = new File(evidenceFolder, model.getFileName() + "_images");
            // NEW: merged captures get a "b"/"c"... suffix on the filename so they
            // don't collide with the primary screenshot for that step on disk.
            String fileSuffix = mergeIntoCurrentStep ? ("step" + effectiveStepNumber + "_" + System.nanoTime()) : ("step" + effectiveStepNumber);
            File imageFile = new File(imagesDir, fileSuffix + ".png");
            ScreenshotUtil.saveImage(annotated, imageFile);

            StepEntry entry = new StepEntry(effectiveStepNumber, description, imageFile, timestamp, durationLabel,
                    mergeIntoCurrentStep);
            model.addEntry(entry);

            saveInterim();

            setVisible(true);
            countLabel.setText(String.valueOf(stepCounter));
            resultBox.setSelectedIndex(0);
            resultBox.setEnabled(true);
            retakeButton.setEnabled(true);

            // NEW: reset hint field for the new entry
            hintField.setText("");
            hintField.setEnabled(true);
            refreshHintBorder(entry);

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
        // NEW: only roll the step counter back if the removed entry actually
        // consumed a new step number — a merged (continuation) capture didn't.
        if (!removed.isContinuation()) {
            stepCounter--;
        }
        countLabel.setText(String.valueOf(stepCounter));

        boolean hasEntries = !model.getEntries().isEmpty();
        resultBox.setEnabled(hasEntries);
        resultBox.setSelectedIndex(0);
        retakeButton.setEnabled(hasEntries);
        captureButton.setEnabled(true);
        captureButton.setToolTipText(nextStepTooltip());

        // NEW: restore hint field to whatever the now-last entry holds (or disable if none)
        StepEntry newLast = model.getLastEntry();
        hintField.setEnabled(newLast != null);
        hintField.setText(newLast != null ? newLast.getHint() : "");
        if (newLast != null) {
            refreshHintBorder(newLast);
        }

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

            // NEW: Adobe Mode — mark the defect before this screenshot is embedded.
            BufferedImage current = readImage(last.getImageFile());
            if (current == null) {
                JOptionPane.showMessageDialog(this,
                        "Could not open the screenshot for Adobe Mode:\n" + last.getImageFile(),
                        "Adobe Mode error", JOptionPane.ERROR_MESSAGE);
            } else {
                AdobeModeDialog dialog = new AdobeModeDialog(this, current);
                BufferedImage annotated = dialog.showDialog();

                // NEW: the modal Adobe Mode dialog is owned by Swing's hidden
                // shared frame (see AdobeModeDialog), not by this window, so
                // closing it does not automatically hand keyboard focus back
                // here. Without this, hintField shows its red "required"
                // border but silently ignores typing until the user clicks
                // some other window and back. Reclaim focus explicitly,
                // whether Embed or Cancel was clicked.
                toFront();
                requestFocus();
                SwingUtilities.invokeLater(hintField::requestFocusInWindow);

                if (annotated != null) {
                    // Embed clicked (>=1 mark guaranteed by the dialog itself) —
                    // overwrite the same file path, so NEITHER writer needs any
                    // change to pick up the annotated version.
                    try {
                        ScreenshotUtil.saveImage(annotated, last.getImageFile());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Could not save annotated image: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // Cancelled — don't force Fail without annotation; revert to unset.
                    last.setResult(StepEntry.PENDING);
                    resultBox.setSelectedIndex(0);
                }
            }
        } else {
            return;
        }

        refreshHintBorder(last); // NEW

        try {
            saveInterim();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // NEW: red border while Fail + hint blank (mandatory); gray otherwise.
    private void refreshHintBorder(StepEntry entry) {
        boolean requiresHint = StepEntry.FAIL.equals(entry.getResult()) && entry.getHint().isBlank();
        hintField.setBorder(BorderFactory.createLineBorder(requiresHint ? Color.RED : Color.GRAY));
    }

    // NEW
    private BufferedImage readImage(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            return javax.imageio.ImageIO.read(file);
        } catch (IOException ex) {
            return null;
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
        // NEW: block finishing if any Fail entry is still missing its mandatory hint.
        List<StepEntry> missingHints = model.getEntries().stream()
                .filter(en -> StepEntry.FAIL.equals(en.getResult()) && en.getHint().isBlank())
                .collect(Collectors.toList());
        if (!missingHints.isEmpty()) {
            String steps = missingHints.stream()
                    .map(en -> String.valueOf(en.getStepNumber()))
                    .distinct()
                    .collect(Collectors.joining(", "));
            JOptionPane.showMessageDialog(this,
                    "Hint is mandatory for every Fail screenshot.\nMissing for step(s): " + steps,
                    "Hint required", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
