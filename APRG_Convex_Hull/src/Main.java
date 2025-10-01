
import algorithm.Algorithms;
import algorithm.ConvexHullAlgorithm;
import algorithm.andrew.AndrewMonotoneChain;
import algorithm.jarvis.JarvisGiftWrapping;
import benchmark.BenchmarkRunner;
import io.PointGenerator;
import io.PointLoader;
import ui.ConvexHullFrame;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.List;

/**
 * Entry point with two modes:
 *  - Visual mode (default): opens a Swing UI to animate the algorithms.
 *  - Performance mode: runs time measurements on the chosen algorithm(s).
 *
 * CLI flags:
 *   --mode=visual|perf
 *   --algo=andrew|jarvis|all
 *   --file=path/to/points.txt   (format per assignment: first line n, then n lines "x,y")
 *   --n=200                     (only used if --file is not provided)
 *   --seed=1234                 (used for random generation)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String mode   = getArg(args, "--mode", "visual");
        String algo   = getArg(args, "--algo", "andrew");
        String file   = getArg(args, "--file", null);
        int n         = Integer.parseInt(getArg(args, "--n", "200"));
        long seed     = Long.parseLong(getArg(args, "--seed", "1234"));

        // Load or generate points
        List<Point2D> points = (file != null)
                ? PointLoader.load(new File(file))
                : PointGenerator.uniformRandom(n, 1, 10, 1, 10, seed);

        if (mode.equalsIgnoreCase("perf")) {
            runPerf(Algorithms.valueOf(algo), points);
        } else {
            // Visual mode: open Swing UI (ConvexHullFrame handles the animation controls)
            List<Point2D> pts = points; // effectively final for lambda
            SwingUtilities.invokeLater(() -> {
                ConvexHullFrame frame = new ConvexHullFrame(pts);
                frame.setVisible(true);
            });
        }
    }

    /* ------------------------ Helpers ------------------------ */

    private static String getArg(String[] args, String key, String def) {
        for (String a : args) {
            if (a.startsWith(key + "=")) return a.split("=", 2)[1];
        }
        return def;
    }

    private static void runPerf(Algorithms algo, List<Point2D> points) {
        switch (algo) {
            case Algorithms.ALL -> {
                timeOnce("andrew", new AndrewMonotoneChain(), points);
                timeOnce("jarvis", new JarvisGiftWrapping(), points);
            }
            case Algorithms.JARVIS -> timeOnce("jarvis", new JarvisGiftWrapping(), points);
            case Algorithms.ANDREW -> timeOnce("andrew", new AndrewMonotoneChain(), points);
        }
    }

    private static void timeOnce(String name, ConvexHullAlgorithm impl, List<Point2D> points) {
        long ms = BenchmarkRunner.timeMillis(impl, points);
        System.out.printf("Algorithm: %s | n=%d | time=%d ms%n", name, points.size(), ms);
    }
}
