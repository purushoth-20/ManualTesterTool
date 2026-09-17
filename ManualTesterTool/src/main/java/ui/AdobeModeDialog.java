package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * "Adobe Mode" — the pre-embed defect-marking popup shown when a screenshot
 * is marked Fail. Five tools: Rectangle, Circle, Arrow (dark red, broad
 * stroke), Highlighter (transparent yellow), and Spotlight (a glow confined
 * ONLY to the marked area — everything outside it is left completely
 * untouched, by hard-clipping the paint operation to that region).
 *
 * Usage: new AdobeModeDialog(owner, image).showDialog() returns the
 * flattened, annotated image, or null if the user cancelled.
 */
public class AdobeModeDialog extends JDialog {

    public enum Tool { RECTANGLE, CIRCLE, ARROW, HIGHLIGHTER, SPOTLIGHT }

    private static final Color MARK_COLOR = new Color(139, 0, 0); // dark red
    private static final float MARK_STROKE = 4f;

    /** One user-drawn mark, in image pixel coordinates. */
    private static class Mark {
        Tool tool;
        int x, y, w, h;       // bounding box (rectangle/circle/highlighter/spotlight)
        int x1, y1, x2, y2;   // endpoints (arrow only)
    }

    private final BufferedImage baseImage;
    private final List<Mark> marks = new ArrayList<>();
    private final Deque<Mark> undoStack = new ArrayDeque<>();

    private Tool activeTool = Tool.RECTANGLE;
    private Point dragStart;
    private Mark liveMark; // the mark currently being dragged, before mouse release

    private final Canvas canvas;
    private final JButton embedButton = new JButton("Embed");
    private BufferedImage result; // set on successful Embed; stays null on Cancel

    public AdobeModeDialog(Window owner, BufferedImage baseImage) {
        // NOTE: cannot pass `owner` straight through to super(Window, ...) here.
        // java.awt.Dialog's Window-owner constructor requires the owner to
        // actually be a Frame or a Dialog at runtime (it throws
        // "Wrong parent window" otherwise) even though JDialog's own
        // signature accepts any Window. CaptureToggle is a plain JWindow, so
        // that check always failed and this dialog never opened.
        // APPLICATION_MODAL blocks the whole application regardless of the
        // owner chain, so passing a null Frame here (Swing's shared hidden
        // owner frame) preserves modality; `owner` is kept below only to
        // center this dialog over the real toolbar.
        super((Frame) null, "Adobe Mode — mark the defect", ModalityType.APPLICATION_MODAL);
        this.baseImage = baseImage;
        this.canvas = new Canvas();

        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        canvas.setPreferredSize(new Dimension(baseImage.getWidth(), baseImage.getHeight()));
        refreshEmbedState(); // disabled until >=1 mark exists

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);

        // NEW: CaptureToggle (the owner) is setAlwaysOnTop(true) — without this,
        // this modal dialog opens BEHIND the toolbar (still blocking input, just
        // invisible/unreachable). Matching always-on-top here fixes that.
        setAlwaysOnTop(true);
    }

    /** Shows the dialog modally. Returns the annotated image, or null if cancelled. */
    public BufferedImage showDialog() {
        setVisible(true); // blocks until dispose()
        return result;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.add(toolButton("▭ Rectangle", Tool.RECTANGLE));
        bar.add(toolButton("○ Circle", Tool.CIRCLE));
        bar.add(toolButton("↗ Arrow", Tool.ARROW));
        bar.add(toolButton("▤ Highlighter", Tool.HIGHLIGHTER));
        bar.add(toolButton("✺ Spotlight", Tool.SPOTLIGHT));
        return bar;
    }

    private JButton toolButton(String label, Tool tool) {
        JButton b = new JButton(label);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> activeTool = tool);
        return b;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));

        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> {
            if (!marks.isEmpty()) {
                undoStack.push(marks.remove(marks.size() - 1));
                canvas.repaint();
                refreshEmbedState();
            }
        });

        JButton clearButton = new JButton("Clear all");
        clearButton.addActionListener(e -> {
            marks.clear();
            canvas.repaint();
            refreshEmbedState();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });

        embedButton.setForeground(new Color(70, 160, 90));
        embedButton.addActionListener(e -> {
            result = flatten();
            dispose();
        });

        bar.add(undoButton);
        bar.add(clearButton);
        bar.add(cancelButton);
        bar.add(embedButton);
        return bar;
    }

    /** Embed is only clickable once at least one mark exists — per spec. */
    private void refreshEmbedState() {
        embedButton.setEnabled(!marks.isEmpty());
    }

    private BufferedImage flatten() {
        BufferedImage out = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(baseImage, 0, 0, null);
        for (Mark m : marks) {
            renderMark(g, m);
        }
        g.dispose();
        return out;
    }

    private void renderMark(Graphics2D g2, Mark m) {
        // Plain switch-statement form (not a switch EXPRESSION) — this project
        // targets Java 11 via pom.xml, and arrow-case switch expressions need 14+.
        switch (m.tool) {
            case RECTANGLE: {
                g2.setColor(MARK_COLOR);
                g2.setStroke(new BasicStroke(MARK_STROKE));
                g2.drawRect(m.x, m.y, m.w, m.h);
                break;
            }
            case CIRCLE: {
                g2.setColor(MARK_COLOR);
                g2.setStroke(new BasicStroke(MARK_STROKE));
                g2.drawOval(m.x, m.y, m.w, m.h);
                break;
            }
            case ARROW: {
                drawArrow(g2, m.x1, m.y1, m.x2, m.y2);
                break;
            }
            case HIGHLIGHTER: {
                g2.setColor(new Color(255, 255, 0, 90)); // transparent yellow
                g2.fillRect(m.x, m.y, m.w, m.h);
                break;
            }
            case SPOTLIGHT: {
                // Hard-clip to the marked region so the glow CANNOT touch a
                // single pixel outside it — satisfies "must not affect the
                // rest of the screenshot" by construction, not just intent.
                Shape region = new Ellipse2D.Float(m.x, m.y, m.w, m.h);
                Graphics2D glow = (Graphics2D) g2.create();
                glow.setClip(region);
                float cx = m.x + m.w / 2f;
                float cy = m.y + m.h / 2f;
                float radius = Math.max(1f, Math.max(m.w, m.h) / 2f);
                RadialGradientPaint gradient = new RadialGradientPaint(
                        new Point2D.Float(cx, cy), radius,
                        new float[]{0f, 1f},
                        new Color[]{new Color(255, 255, 150, 170), new Color(255, 255, 150, 0)});
                glow.setPaint(gradient);
                glow.fill(region);
                glow.dispose();
                break;
            }
            default:
                break;
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        g2.setColor(MARK_COLOR);
        g2.setStroke(new BasicStroke(MARK_STROKE));
        g2.drawLine(x1, y1, x2, y2);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int headLen = 18;
        int hx1 = (int) (x2 - headLen * Math.cos(angle - Math.PI / 7));
        int hy1 = (int) (y2 - headLen * Math.sin(angle - Math.PI / 7));
        int hx2 = (int) (x2 - headLen * Math.cos(angle + Math.PI / 7));
        int hy2 = (int) (y2 - headLen * Math.sin(angle + Math.PI / 7));
        g2.drawLine(x2, y2, hx1, hy1);
        g2.drawLine(x2, y2, hx2, hy2);
    }

    /** The drawing surface: renders the base image + all marks, and handles drag-to-draw. */
    private class Canvas extends JPanel {

        // NEW: an already-placed mark currently being dragged to a new
        // position, as opposed to `liveMark`, which is a brand-new mark
        // being drawn. Non-null only between mousePressed and mouseReleased
        // when the press started inside an existing mark.
        private Mark movingMark;

        Canvas() {
            MouseAdapter drag = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Mark hit = findMarkAt(e.getPoint());
                    if (hit != null) {
                        movingMark = hit;
                        dragStart = e.getPoint();
                        return;
                    }
                    dragStart = e.getPoint();
                    liveMark = new Mark();
                    liveMark.tool = activeTool;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (movingMark != null) {
                        int dx = e.getX() - dragStart.x;
                        int dy = e.getY() - dragStart.y;
                        translateMark(movingMark, dx, dy);
                        dragStart = e.getPoint();
                        canvas.repaint();
                        return;
                    }
                    if (dragStart == null || liveMark == null) return;
                    updateLiveMark(e.getPoint());
                    canvas.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (movingMark != null) {
                        movingMark = null;
                        dragStart = null;
                        canvas.repaint();
                        return;
                    }
                    if (liveMark == null) return;
                    updateLiveMark(e.getPoint());
                    // Ignore accidental zero-size clicks (no real drag happened)
                    boolean hasSize = (liveMark.tool == Tool.ARROW)
                            ? (liveMark.x1 != liveMark.x2 || liveMark.y1 != liveMark.y2)
                            : (liveMark.w > 2 && liveMark.h > 2);
                    if (hasSize) {
                        marks.add(liveMark);
                        undoStack.clear();
                        refreshEmbedState();
                    }
                    liveMark = null;
                    dragStart = null;
                    canvas.repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    // NEW: hint that an existing mark can be picked up and moved.
                    boolean overMark = findMarkAt(e.getPoint()) != null;
                    setCursor(new Cursor(overMark ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            };
            addMouseListener(drag);
            addMouseMotionListener(drag);
        }

        /** Topmost mark (last drawn) whose area contains p, or null. */
        private Mark findMarkAt(Point p) {
            for (int i = marks.size() - 1; i >= 0; i--) {
                Mark m = marks.get(i);
                if (containsPoint(m, p)) return m;
            }
            return null;
        }

        private boolean containsPoint(Mark m, Point p) {
            if (m.tool == Tool.ARROW) {
                return distanceToSegment(p.x, p.y, m.x1, m.y1, m.x2, m.y2) <= 8.0;
            }
            return p.x >= m.x && p.x <= m.x + m.w && p.y >= m.y && p.y <= m.y + m.h;
        }

        private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
            double dx = x2 - x1, dy = y2 - y1;
            double lenSq = dx * dx + dy * dy;
            double t = (lenSq == 0) ? 0 : ((px - x1) * dx + (py - y1) * dy) / lenSq;
            t = Math.max(0, Math.min(1, t));
            double projX = x1 + t * dx, projY = y1 + t * dy;
            return Math.hypot(px - projX, py - projY);
        }

        private void translateMark(Mark m, int dx, int dy) {
            if (m.tool == Tool.ARROW) {
                m.x1 += dx; m.y1 += dy;
                m.x2 += dx; m.y2 += dy;
            } else {
                m.x += dx; m.y += dy;
            }
        }

        private void updateLiveMark(Point p) {
            if (liveMark.tool == Tool.ARROW) {
                liveMark.x1 = dragStart.x;
                liveMark.y1 = dragStart.y;
                liveMark.x2 = p.x;
                liveMark.y2 = p.y;
            } else {
                liveMark.x = Math.min(dragStart.x, p.x);
                liveMark.y = Math.min(dragStart.y, p.y);
                liveMark.w = Math.abs(p.x - dragStart.x);
                liveMark.h = Math.abs(p.y - dragStart.y);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(baseImage, 0, 0, null);
            for (Mark m : marks) {
                renderMark(g2, m);
            }
            if (liveMark != null) {
                renderMark(g2, liveMark); // live preview while dragging
            }
        }
    }
}
