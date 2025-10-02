package io;

import java.awt.geom.Point2D;
import java.util.*;

public class PointGenerator {
    public static List<Point2D> uniformRandom(int n, double minX, double maxX, double minY, double maxY, long seed) {
        Random rnd = new Random(seed);
        List<Point2D> pts = new ArrayList<>(n);
        for (int i=0;i<n;i++){
            double x = minX + rnd.nextDouble() * (maxX - minX);
            double y = minY + rnd.nextDouble() * (maxY - minY);
            pts.add(new Point2D.Double(x,y));
        }
        return pts;
    }
}
