// src/main/java/algorithm/andrew/AndrewMonotoneChain.java
package algorithm.andrew;

import algorithm.ConvexHullAlgorithm;

import java.awt.geom.Point2D;
import java.util.*;

public class AndrewMonotoneChain implements ConvexHullAlgorithm {
    private static final double EPS = 1e-12;

    @Override
    public List<Point2D> computeConvexHull(List<Point2D> points) {
        if (points == null || points.size() <= 1)
            return points == null ? List.of() : new ArrayList<>(points);

        List<Point2D> pts = new ArrayList<>(points);
        pts.sort(Comparator.comparingDouble(Point2D::getX).thenComparingDouble(Point2D::getY));
        pts = dedup(pts);

        List<Point2D> lower = new ArrayList<>();
        for (Point2D p : pts) {
            while (lower.size() >= 2 && orient(lower.get(lower.size()-2), lower.get(lower.size()-1), p) <= 0)
                lower.remove(lower.size()-1);
            lower.add(p);
        }

        List<Point2D> upper = new ArrayList<>();
        for (int i = pts.size()-1; i >= 0; i--) {
            Point2D p = pts.get(i);
            while (upper.size() >= 2 && orient(upper.get(upper.size()-2), upper.get(upper.size()-1), p) <= 0)
                upper.remove(upper.size()-1);
            upper.add(p);
        }

        if (!lower.isEmpty()) lower.remove(lower.size()-1);
        if (!upper.isEmpty()) upper.remove(upper.size()-1);
        lower.addAll(upper);
        return lower;
    }

    private static double orient(Point2D a, Point2D b, Point2D c) {
        double x1 = b.getX()-a.getX(), y1 = b.getY()-a.getY();
        double x2 = c.getX()-a.getX(), y2 = c.getY()-a.getY();
        double cross = x1*y2 - y1*x2;
        if (Math.abs(cross) < EPS) return 0.0;
        return cross;
    }
    private static List<Point2D> dedup(List<Point2D> sorted) {
        List<Point2D> out = new ArrayList<>(sorted.size());
        Point2D prev = null;
        for (Point2D p : sorted) {
            if (prev == null || Math.abs(prev.getX()-p.getX())>EPS || Math.abs(prev.getY()-p.getY())>EPS) {
                out.add(p); prev = p;
            }
        }
        return out;
    }
}
