// src/main/java/io/PointLoader.java
package io;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;

public class PointLoader {
    /**
     * Format lt. Angabe:
     * Zeile 1: n
     * Zeile 2..n+1: x,y (Float/Doubles) – Komma-separiert
     */
    public static List<Point2D> load(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            if (line == null) return List.of();
            int n = Integer.parseInt(line.trim());
            List<Point2D> pts = new ArrayList<>(n);
            for (int i=0;i<n;i++){
                String s = br.readLine();
                if (s==null) break;
                String[] parts = s.split(",");
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                pts.add(new Point2D.Double(x,y));
            }
            return pts;
        }
    }
}
