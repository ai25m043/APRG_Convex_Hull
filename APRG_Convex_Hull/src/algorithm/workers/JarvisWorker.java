// src/main/java/algorithm/workers/JarvisWorker.java
package algorithm.workers;

import algorithm.progress.HullProgressListener;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Animated Jarvis March (Gift Wrapping).
 * Publishes the growing hull after each vertex is added.
 *
 * Orientation convention:
 *   orient(a,b,c) > 0  => c is to the LEFT of ab (counter-clockwise turn)
 *   orient(a,b,c) < 0  => RIGHT
 *   |orient| <= EPS    => collinear
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

        // Work on a de-duplicated copy for robustness
        List<Point2D> points = deduplicate(input);
        int n = points.size();
        if (n <= 1) return new ArrayList<>(points);

        // Find leftmost (then lowest) starting point
        int leftmost = 0;
        for (int i = 1; i < n; i++) {
            Point2D p = points.get(i);
            Point2D s = points.get(leftmost);
            if (p.getX() < s.getX() ||
                    (Math.abs(p.getX() - s.getX()) < EPS && p.getY() < s.getY())) {
                leftmost = i;
            }
        }

        List<Point2D> hull = new ArrayList<>();
        int p = leftmost;
        do {
            hull.add(points.get(p));
            int q = (p + 1) % n;

            // Choose the "most left" next point; break ties by farthest distance
            for (int r = 0; r < n; r++) {
                if (r == p || r == q) continue;
                double o = orient(points.get(p), points.get(q), points.get(r));
                if (o > EPS || (Math.abs(o) <= EPS && dist2(points.get(p), points.get(r)) > dist2(points.get(p), points.get(q)))) {
                    q = r;
                }
            }

            p = q;

            // publish current partial hull for animation
            publish(cloneList(hull));
            sleep(delayMillis);

        } while (p != leftmost);

        return hull;
    }

    @Override
    protected void process(List<List<Point2D>> chunks) {
        if (chunks.isEmpty()) return;
        List<Point2D> current = chunks.get(chunks.size() - 1);
        // For Jarvis we have a single "chain": send it as the lower, leave upper empty.
        listener.onChainsUpdated(current, List.of());
    }

    @Override
    protected void done() {
        try {
            List<Point2D> result = get();
            listener.onFinished(result);
        } catch (Exception e) {
            listener.onFinished(List.of());
        }
    }

    /** Signed cross product: >0 => LEFT turn, <0 => RIGHT turn, ~0 => collinear. */
    private static double orient(Point2D a, Point2D b, Point2D c) {
        double x1 = b.getX() - a.getX(), y1 = b.getY() - a.getY();
        double x2 = c.getX() - a.getX(), y2 = c.getY() - a.getY();
        double cross = x1 * y2 - y1 * x2;
        if (Math.abs(cross) < EPS) return 0.0;
        return cross;
    }

    private static double dist2(Point2D a, Point2D b) {
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

    /** Remove near-duplicates by rounding to 12 decimals (robust for UI-scale inputs). */
    private static List<Point2D> deduplicate(List<Point2D> points) {
        Set<String> seen = new HashSet<>();
        List<Point2D> unique = new ArrayList<>(points.size());
        for (Point2D p : points) {
            String key = String.format(Locale.ROOT, "%.12f,%.12f", p.getX(), p.getY());
            if (seen.add(key)) unique.add(p);
        }
        return unique;
    }
}
