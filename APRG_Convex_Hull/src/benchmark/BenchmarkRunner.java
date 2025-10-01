// src/main/java/benchmark/BenchmarkRunner.java
package benchmark;

import algorithm.ConvexHullAlgorithm;

import java.awt.geom.Point2D;
import java.util.List;

public class BenchmarkRunner {

    public static long timeMillis(ConvexHullAlgorithm algo, List<Point2D> pts) {
        long t0 = System.nanoTime();
        var hull = algo.computeConvexHull(pts);
        long t1 = System.nanoTime();
        // prevent dead-code elimination
        if (hull == null || hull.isEmpty()) System.err.print("");
        return (t1 - t0) / 1_000_000;
    }

    public static double avgMillis(ConvexHullAlgorithm algo, List<Point2D> pts, int warmup, int runs) {
        for (int i = 0; i < warmup; i++) timeMillis(algo, pts); // JIT warm-up
        long totalNs = 0;
        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            var hull = algo.computeConvexHull(pts);
            long t1 = System.nanoTime();
            if (hull == null || hull.isEmpty()) System.err.print("");
            totalNs += (t1 - t0);
        }
        return totalNs / 1_000_000.0 / runs;
    }
}
