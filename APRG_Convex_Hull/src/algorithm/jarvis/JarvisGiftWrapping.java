// src/main/java/algorithm/jarvis/JarvisGiftWrapping.java
package algorithm.jarvis;

import algorithm.ConvexHullAlgorithm;

import java.awt.geom.Point2D;
import java.util.*;

public class JarvisGiftWrapping implements ConvexHullAlgorithm {
    private static final double EPS = 1e-12;

    @Override
    public List<Point2D> computeConvexHull(List<Point2D> points) {
        if (points == null || points.size() <= 1)
            return points == null ? List.of() : new ArrayList<>(points);

        // Start: linkester (bei Gleichstand: niedrigster y)
        int start = 0;
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).getX() < points.get(start).getX()
                    || (Math.abs(points.get(i).getX()-points.get(start).getX())<EPS
                    && points.get(i).getY() < points.get(start).getY())) {
                start = i;
            }
        }
        List<Point2D> hull = new ArrayList<>();
        int p = start;
        do {
            hull.add(points.get(p));
            int q = (p + 1) % points.size();
            for (int r = 0; r < points.size(); r++) {
                if (r == p) continue;
                double o = orient(points.get(p), points.get(q), points.get(r));
                // wir suchen "am weitesten links": o < 0 -> r ist links von pq? (Orientationsdefinition beachten)
                if (o < 0 || (Math.abs(o) < EPS && dist(points.get(p), points.get(r)) > dist(points.get(p), points.get(q))))
                    q = r;
            }
            p = q;
        } while (p != start);

        return hull;
    }

    private static double orient(Point2D a, Point2D b, Point2D c) {
        return (b.getX()-a.getX())*(c.getY()-a.getY()) - (b.getY()-a.getY())*(c.getX()-a.getX());
    }
    private static double dist(Point2D a, Point2D b) {
        double dx = a.getX()-b.getX(), dy = a.getY()-b.getY();
        return dx*dx + dy*dy;
    }
}
