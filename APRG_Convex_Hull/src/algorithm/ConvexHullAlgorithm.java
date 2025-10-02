package algorithm;

import java.awt.geom.Point2D;
import java.util.List;

public interface ConvexHullAlgorithm {
    /**
     * Berechnet die konvexe Hülle der gegebenen Punkte.
     * Rückgabe: Punkte der Hülle in CCW-Reihenfolge (Startpunkt nicht dupliziert).
     */
    List<Point2D> computeConvexHull(List<Point2D> points);
}
