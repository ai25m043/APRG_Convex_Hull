package algorithm.workers;

import algorithm.progress.HullProgressListener;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Animated Jarvis March (Gift Wrapping) convex hull algorithm.
 * Publishes the growing hull step by step.
 */
public class JarvisWorker extends SwingWorker<List<Point2D>, List<Point2D>> {

    private final List<Point2D> input;
    private final HullProgressListener listener;
    private final long delayMillis;
    private static final double EPS = 1e-12;

    public JarvisWorker(List<Point2D> input, HullProgressListener listener, long delayMillis) {
        this.input = input;
        this.listener = listener;
        this.delayMillis = delayMillis;
    }

    @Override
    protected List<Point2D> doInBackground() {
        if (input == null || input.size() <= 1) {
            return input == null ? List.of() : new ArrayList<>(input);
        }

        List<Point2D> points = deduplicate(input);
        int n = points.size();

        // Find the leftmost point (and lowest Y if tied)
        int leftmost = 0;
        for (int i = 1; i < n; i++) {
            Point2D p = points.get(i);
            Point2D curr = points.get(leftmost);
            if (p.getX() < curr.getX() || (Math.abs(p.getX() - curr.getX()) < EPS && p.getY() < curr.getY())) {
                leftmost = i;
            }
        }

        List<Point2D> hull = new ArrayList<>();
        int p = leftmost;

        do {
            hull.add(points.get(p));
            int q = (p + 1) % n;

            for (int r = 0; r < n; r++) {
                if (r == p || r == q) continue;

                double orientation = orient(points.get(p), points.get(q), points.get(r));
                if (orientation < 0 || (Math.abs(orientation) < EPS && dist(points.get(p), points.get(r)) > dist(points.get(p), points.get(q)))) {
                    q = r;
                }
            }

            p = q;

            // Publish current partial hull to animate
            publish(cloneList(hull));
            sleep(delayMillis);

        } while (p != leftmost);

        return hull;
    }

    @Override
    protected void process(List<List<Point2D>> chunks) {
        if (chunks.isEmpty()) return;
        List<Point2D> current = chunks.get(chunks.size() - 1);
        listener.onChainsUpdated(current, List.of());  // only one "chain" here
    }

    @Override
    protected void done() {
        try {
            List<Point2D> finalHull = get();
            listener.onFinished(finalHull);
        } catch (Exception e) {
            listener.onFinished(List.of());
        }
    }

    private static double orient(Point2D a, Point2D b, Point2D c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) -
                (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private static double dist(Point2D a, Point2D b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    private static void sleep(long millis) {
        if (millis > 0) {
            try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
        }
    }

    private static List<Point2D> cloneList(List<Point2D> list) {
        return list.stream()
                .map(p -> new Point2D.Double(p.getX(), p.getY()))
                .collect(Collectors.toList());
    }

    private static List<Point2D> deduplicate(List<Point2D> points) {
        Set<String> seen = new HashSet<>();
        List<Point2D> unique = new ArrayList<>();
        for (Point2D p : points) {
            String key = String.format("%.6f,%.6f", p.getX(), p.getY());
            if (seen.add(key)) {
                unique.add(p);
            }
        }
        return unique;
    }
}
