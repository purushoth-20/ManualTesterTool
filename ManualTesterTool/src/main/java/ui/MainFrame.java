package ui;

import core.EvidenceModel;
import core.GherkinParser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class MainFrame extends JFrame {

    private final JComboBox<String> formatBox = new JComboBox<>(new String[]{"Word (.docx)", "Excel (.xlsx)"});
    private final JTextField fileNameField = new JTextField();
    private final JTextArea gherkinArea = new JTextArea(10, 40);
    private final JLabel statusLabel = new JLabel(" ");

    public MainFrame() {
        super("Manual Tester Tool - Setup");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        topPanel.add(new JLabel("Output Format:"));
        topPanel.add(formatBox);
        topPanel.add(new JLabel("File Name (required):"));
        topPanel.add(fileNameField);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(6, 6));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        centerPanel.add(new JLabel("Gherkin steps (optional \u2014 leave blank to use Step 1, Step 2, ... instead):"), BorderLayout.NORTH);
        gherkinArea.setLineWrap(true);
        gherkinArea.setWrapStyleWord(true);
        centerPanel.add(new JScrollPane(gherkinArea), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel.setForeground(java.awt.Color.RED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 12));
        bottomPanel.add(statusLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("Submit Gherkin & Start");
        submitButton.addActionListener(e -> onSubmit());
        buttonPanel.add(submitButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(560, 460));
        pack();
        setLocationRelativeTo(null);
    }

    private void onSubmit() {
        String fileName = fileNameField.getText() == null ? "" : fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            statusLabel.setText("File name is required \u2014 the app can't proceed without it.");
            return;
        }
        // Keep the filename filesystem-safe
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        String gherkinText = gherkinArea.getText();

        List<String> steps;
        if (GherkinParser.isBlank(gherkinText)) {
            // Allowed: proceed with no Gherkin -> fallback Step 1, Step 2... mode
            steps = null;
        } else if (!GherkinParser.isValidGherkin(gherkinText)) {
            statusLabel.setText("That doesn't look like valid Gherkin. Use Given/When/Then/And/But, or leave the box empty.");
            return;
        } else {
            steps = GherkinParser.parseSteps(gherkinText);
        }

        EvidenceModel.Format format = formatBox.getSelectedIndex() == 0
                ? EvidenceModel.Format.WORD
                : EvidenceModel.Format.EXCEL;

        EvidenceModel model = new EvidenceModel(fileName, format, steps);

        JOptionPane.showMessageDialog(this,
                "Setup complete. A floating capture toggle will now appear.\n"
                        + "Click it to capture + embed a screenshot for each step.",
                "Ready", JOptionPane.INFORMATION_MESSAGE);

        dispose();
        new CaptureToggle(model).setVisible(true);
    }
}
