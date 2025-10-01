package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * ConvexHullPanel
 * - Draws input points
 * - Draws "live" lower/upper chains during algorithm progress
 * - Draws final convex hull when available
 *
 * Coordinate handling:
 *   World (data) -> Screen mapping with padding and Y-inversion.
 */
public class ConvexHullPanel extends JPanel {

    // immutable reference for points (you can replace via setPoints if needed)
    private List<Point2D> points;

    // live (in-progress) chains published during computation
    private List<Point2D> liveLower = Collections.emptyList();
    private List<Point2D> liveUpper = Collections.emptyList();

    // final hull (drawn bold)
    private List<Point2D> finalHull = Collections.emptyList();

    // drawing config
    private static final int PAD = 30;
    private static final int POINT_SIZE = 6;

    // optional toggles
    private boolean showFrame = true;
    private boolean closeHullLoop = true; // draw last edge to first

    public ConvexHullPanel(List<Point2D> points, List<Point2D> hull) {
        this.points = points == null ? Collections.emptyList() : points;
        this.finalHull = hull == null ? Collections.emptyList() : hull;
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    /** Replace input point set (e.g., when regenerating points). */
    public void setPoints(List<Point2D> points) {
        this.points = points == null ? Collections.emptyList() : points;
        // reset visuals
        this.liveLower = Collections.emptyList();
        this.liveUpper = Collections.emptyList();
        this.finalHull = Collections.emptyList();
        repaint();
    }

    /** Called repeatedly during computation to show current chains. */
    public void setLiveChains(List<Point2D> lower, List<Point2D> upper) {
        this.liveLower = lower == null ? Collections.emptyList() : lower;
        this.liveUpper = upper == null ? Collections.emptyList() : upper;
        repaint();
    }

    /** Called when the final hull is ready. */
    public void setFinalHull(List<Point2D> hull) {
        this.finalHull = hull == null ? Collections.emptyList() : hull;
        repaint();
    }

    /** Optional UI toggles */
    public void setShowFrame(boolean showFrame) { this.showFrame = showFrame; repaint(); }
    public void setCloseHullLoop(boolean closeHullLoop) { this.closeHullLoop = closeHullLoop; repaint(); }

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        Graphics2D g = (Graphics2D) gRaw.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (points.isEmpty()) {
            g.dispose();
            return;
        }

        // compute world bounds from points (so all content fits nicely)
        double minX = points.stream().mapToDouble(Point2D::getX).min().orElse(0);
        double maxX = points.stream().mapToDouble(Point2D::getX).max().orElse(1);
        double minY = points.stream().mapToDouble(Point2D::getY).min().orElse(0);
        double maxY = points.stream().mapToDouble(Point2D::getY).max().orElse(1);
        if (Math.abs(maxX - minX) < 1e-9) maxX = minX + 1;
        if (Math.abs(maxY - minY) < 1e-9) maxY = minY + 1;

        int w = getWidth(), h = getHeight();
        int drawW = Math.max(1, w - 2 * PAD);
        int drawH = Math.max(1, h - 2 * PAD);

        final double fMinX = minX, fMaxX = maxX, fMinY = minY, fMaxY = maxY;
        Function<Point2D, Point2D> toScreen = p -> {
            double nx = (p.getX() - fMinX) / (fMaxX - fMinX); // 0..1
            double ny = (p.getY() - fMinY) / (fMaxY - fMinY); // 0..1
            double sx = PAD + nx * drawW;
            double sy = PAD + (1 - ny) * drawH;               // invert Y for screen coords
            return new Point2D.Double(sx, sy);
        };

        // optional frame/background
        if (showFrame) {
            g.setColor(new Color(235, 235, 235));
            g.fillRect(PAD, PAD, drawW, drawH);
            g.setColor(new Color(200, 200, 200));
            g.drawRect(PAD, PAD, drawW, drawH);
        }

        // draw input points
        g.setColor(new Color(50, 50, 50));
        for (Point2D p : points) {
            Point2D sp = toScreen.apply(p);
            double r = POINT_SIZE / 2.0;
            Shape dot = new Ellipse2D.Double(sp.getX() - r, sp.getY() - r, POINT_SIZE, POINT_SIZE);
            g.fill(dot);
        }

        // live lower chain (solid blue)
        drawPolyline(g, toScreen, liveLower, new BasicStroke(2f), new Color(0, 120, 230));

        // live upper chain (dashed green)
        float[] dash = {8f, 8f};
        drawPolyline(
                g, toScreen, liveUpper,
                new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f),
                new Color(0, 170, 120)
        );

        // final hull (bold dark) — optionally closed
        if (finalHull.size() >= 2) {
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(3f));
            g.setColor(new Color(20, 20, 20));

            int limit = closeHullLoop ? finalHull.size() : finalHull.size() - 1;
            for (int i = 0; i < limit; i++) {
                Point2D a = toScreen.apply(finalHull.get(i));
                Point2D b = toScreen.apply(finalHull.get((i + 1) % finalHull.size()));
                if (!closeHullLoop && i == finalHull.size() - 1) break;
                g.draw(new Line2D.Double(a, b));
            }
            g.setStroke(old);

            // emphasize hull vertices
            g.setColor(new Color(20, 20, 20));
            for (Point2D p : finalHull) {
                Point2D sp = toScreen.apply(p);
                double r = POINT_SIZE / 2.0 + 1;
                Shape dot = new Ellipse2D.Double(sp.getX() - r, sp.getY() - r, POINT_SIZE + 2, POINT_SIZE + 2);
                g.fill(dot);
            }
        }

        g.dispose();
    }

    private void drawPolyline(Graphics2D g,
                              Function<Point2D, Point2D> toScreen,
                              List<Point2D> pts,
                              Stroke stroke,
                              Color color) {
        if (pts == null || pts.size() < 2) return;
        Stroke old = g.getStroke();
        Color oldC = g.getColor();
        g.setStroke(stroke);
        g.setColor(color);
        for (int i = 0; i < pts.size() - 1; i++) {
            Point2D a = toScreen.apply(pts.get(i));
            Point2D b = toScreen.apply(pts.get(i + 1));
            g.draw(new Line2D.Double(a, b));
        }
        g.setStroke(old);
        g.setColor(oldC);
    }
}
