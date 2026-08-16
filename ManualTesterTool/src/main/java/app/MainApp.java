package app;

import ui.MainFrame;

import javax.swing.SwingUtilities;

public class MainApp {

    public static void main(String[] args) {
        // Force UTF-8 everywhere
        System.setProperty("file.encoding", "UTF-8");

        javax.swing.SwingUtilities.invokeLater(() -> {
            new ui.MainFrame().setVisible(true);
        });
    }
}
