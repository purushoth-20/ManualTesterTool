package app;

import ui.MainFrame;

import javax.swing.SwingUtilities;

public class MainApp {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}