package ui;

import core.ScreenshotUtil;
import core.WordWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FloatingCapture extends JWindow {

    private Point initialClick;

    public FloatingCapture() {
        setAlwaysOnTop(true);
        setSize(120, 45);
        setLocation(400, 300);

        JButton captureBtn = new JButton("📸 Capture");
        captureBtn.setFocusPainted(false);

        captureBtn.addActionListener(e -> capture());

        add(captureBtn);

        // 🔑 THIS IS THE FIX
        makeDraggable(this);
        makeDraggable(captureBtn);

        setVisible(true);
    }

    // ================= DRAG SUPPORT =================
    private void makeDraggable(Component component) {
        MouseAdapter adapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // get window location
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                // determine movement
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                // move window
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        };

        component.addMouseListener(adapter);
        component.addMouseMotionListener(adapter);
    }

    // ================= CAPTURE =================
    private void capture() {
        try {
            // hide before screenshot
            setVisible(false);
            Thread.sleep(200);

            String imgPath = ScreenshotUtil.capture();
            WordWriter.addScreenshot(imgPath);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error capturing screenshot",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            ex.printStackTrace();
        } finally {
            // show again
            setVisible(true);
        }
    }
}
