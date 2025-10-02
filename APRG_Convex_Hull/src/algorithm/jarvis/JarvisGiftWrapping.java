package algorithm.jarvis;

import algorithm.ConvexHullAlgorithm;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Jarvis March (Gift Wrapping) for the 2D convex hull.
 *
 * - Returns the hull in counter-clockwise order starting at the leftmost (and, on tie, lowest) point.
 * - Collinear points on an edge are skipped (only extreme endpoints remain).
 * - Time: O(n * h), where h is the number of hull vertices.
 */
public class JarvisGiftWrapping implements ConvexHullAlgorithm {
    private static final double EPS = 1e-12;

    @Override
    public List<Point2D> computeConvexHull(List<Point2D> points) {
        if (points == null || points.size() <= 1) {
            return points == null ? List.of() : new ArrayList<>(points);
        }

        // Work on a de-duplicated copy to avoid degenerate loops.
        List<Point2D> pts = deduplicate(points);
        int n = pts.size();
        if (n <= 1) return new ArrayList<>(pts);

        // 1) Start at the leftmost (and then lowest) point.
        int start = 0;
        for (int i = 1; i < n; i++) {
            Point2D p = pts.get(i);
            Point2D s = pts.get(start);
            if (p.getX() < s.getX() ||
                    (Math.abs(p.getX() - s.getX()) < EPS && p.getY() < s.getY())) {
                start = i;
            }
        }

        // 2) Wrap around: at each step choose the "most left" next point (max positive orientation).
        List<Point2D> hull = new ArrayList<>();
        int p = start;
        do {
            hull.add(pts.get(p));
            int q = (p + 1) % n;
            for (int r = 0; r < n; r++) {
                if (r == p || r == q) continue;
                double o = orient(pts.get(p), pts.get(q), pts.get(r));
                // Choose r if it is MORE to the left of pq (o > 0).
                // If collinear (|o| <= EPS), choose the farther one to keep only extreme endpoints.
                if (o > EPS || (Math.abs(o) <= EPS && dist2(pts.get(p), pts.get(r)) > dist2(pts.get(p), pts.get(q)))) {
                    q = r;
                }
            }
            p = q;
        } while (p != start);

        return hull;
    }

    /** Signed area (twice the triangle area). >0: c is to the LEFT of ab; <0: to the RIGHT; ~0: collinear. */
    private static double orient(Point2D a, Point2D b, Point2D c) {
        double x1 = b.getX() - a.getX(), y1 = b.getY() - a.getY();
        double x2 = c.getX() - a.getX(), y2 = c.getY() - a.getY();
        double cross = x1 * y2 - y1 * x2;
        if (Math.abs(cross) < EPS) return 0.0;
        return cross;
    }

    private static double dist2(Point2D a, Point2D b) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    /** Remove approximate duplicates (by rounding) to avoid zero-length edges / infinite loops. */
    private static List<Point2D> deduplicate(List<Point2D> input) {
        Set<String> seen = new HashSet<>();
        List<Point2D> out = new ArrayList<>(input.size());
        for (Point2D p : input) {
            // quantize for robustness; 12 decimals is typically safe here
            String key = String.format(Locale.ROOT, "%.12f,%.12f", p.getX(), p.getY());
            if (seen.add(key)) out.add(p);
        }
        return out;
    }
}
