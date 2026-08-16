package ui;

import core.WordWriter;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainFrame extends JFrame {

    private JTextArea flowArea;
    private JComboBox<String> finalStatusBox;

    public MainFrame() {
        setTitle("Manual Test Evidence Tool");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
    }

    private void initUI() {

        flowArea = new JTextArea();
        flowArea.setLineWrap(true);
        flowArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(flowArea);

        JButton startBtn = new JButton("Start Evidence");
        JButton saveBtn = new JButton("Save Evidence");

        finalStatusBox = new JComboBox<>(new String[]{"PASS", "FAIL", "BLOCKED"});

        startBtn.addActionListener(e -> startEvidence());
        saveBtn.addActionListener(e -> saveEvidence());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(startBtn);
        bottomPanel.add(finalStatusBox);
        bottomPanel.add(saveBtn);

        add(new JLabel("Paste Complete Flow Description"), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ---------------- START EVIDENCE ----------------
    private void startEvidence() {
        try {
            String flowText = flowArea.getText().trim();

            if (flowText.isEmpty()) {
                warn("Flow description cannot be empty");
                return;
            }

            String scenarioName = extractScenario(flowText);

            if (!isValidScenarioName(scenarioName)) {
                warn("""
Invalid scenario name.

Allowed characters:
A–Z a–z 0–9 space _ -

Example:
Scenario: Gmail Login
""");
                return;
            }

            WordWriter.initFromFlow(flowText);

            new FloatingCapture(); // open floating toggle

        } catch (Exception ex) {
            warn(ex.getMessage());
        }
    }

    // ---------------- SAVE ----------------
    private void saveEvidence() {
        try {
            String status = finalStatusBox.getSelectedItem().toString();
            WordWriter.finish(status);

            JOptionPane.showMessageDialog(
                    this,
                    "Evidence saved successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            warn("Error saving evidence");
        }
    }

    // ---------------- VALIDATION ----------------
    private String extractScenario(String text) throws Exception {
        Pattern p = Pattern.compile("(?i)scenario\\s*:\\s*(.+)");
        Matcher m = p.matcher(text);

        if (!m.find()) {
            throw new Exception("Scenario not found. Use 'Scenario:'");
        }
        return m.group(1).trim();
    }

    private boolean isValidScenarioName(String name) {
        return name.matches("[a-zA-Z0-9 _-]+");
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(
                this,
                msg,
                "Warning",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
