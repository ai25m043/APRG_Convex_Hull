package benchmark;

import algorithm.ConvexHullAlgorithm;

import java.awt.geom.Point2D;
import java.util.List;

public class BenchmarkRunner {

    /** Single timing (milliseconds). */
    public static long timeMillis(ConvexHullAlgorithm algo, List<Point2D> pts) {
        long t0 = System.nanoTime();
        var hull = algo.computeConvexHull(pts);
        long t1 = System.nanoTime();
        // prevent dead-code elimination
        if (hull == null || hull.isEmpty()) System.err.print("");
        return (t1 - t0) / 1_000_000;
    }

    /** Average over 'runs' executions (no warmup). */
    public static double avgMillis(ConvexHullAlgorithm algo, List<Point2D> pts, int runs) {
        long totalNs = 0L;
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
