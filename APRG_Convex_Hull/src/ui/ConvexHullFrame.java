// src/main/java/ui/ConvexHullFrame.java
package ui;

import algorithm.Algorithms;
import algorithm.ConvexHullAlgorithm;
import algorithm.andrew.AndrewMonotoneChain;
import algorithm.jarvis.JarvisGiftWrapping;
import algorithm.progress.HullProgressListener;
import algorithm.workers.AndrewsWorker;
import benchmark.BenchmarkRunner;
import io.PointGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class ConvexHullFrame extends JFrame {
    private final ConvexHullPanel panel;

    // top bar
    private final JButton runBtn = new JButton("Run");
    private final JComboBox<Algorithms> algoBox = new JComboBox<>(Algorithms.values());

    // benchmark controls
    private final JButton benchBtn = new JButton("Benchmark");
    private final JComboBox<String> benchAlgoBox = new JComboBox<>(new String[]{"andrew", "jarvis", "all"});
    private final JSpinner nSpinner     = new JSpinner(new SpinnerNumberModel(200, 3, 1_000_000, 100));
    private final JSpinner seedSpinner  = new JSpinner(new SpinnerNumberModel(1234, 0, Integer.MAX_VALUE, 1));
    private final JSpinner warmSpinner  = new JSpinner(new SpinnerNumberModel(3, 0, 100, 1));
    private final JSpinner runsSpinner  = new JSpinner(new SpinnerNumberModel(8, 1, 1000, 1));

    // benchmark output
    private final JTextArea benchOut = new JTextArea(7, 20);

    private final List<Point2D> initialPoints; // reference to current dataset

    public ConvexHullFrame(List<Point2D> points) {
        super("Convex Hull – Visual Mode");
        this.initialPoints = points;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ==== CENTER (canvas) ====
        panel = new ConvexHullPanel(points, null);
        add(panel, BorderLayout.CENTER);

        // ==== NORTH (toolbar) ====
        JToolBar bar = new JToolBar();
        bar.add(new JLabel("Algorithm: "));
        bar.add(algoBox);
        bar.add(runBtn);
        bar.addSeparator(new Dimension(20, 0));
        bar.add(new JLabel("Benchmark "));
        bar.add(new JLabel("algo:"));
        bar.add(benchAlgoBox);
        bar.add(new JLabel(" n:"));
        bar.add(nSpinner);
        bar.add(new JLabel(" seed:"));
        bar.add(seedSpinner);
        bar.add(new JLabel(" warmup:"));
        bar.add(warmSpinner);
        bar.add(new JLabel(" runs:"));
        bar.add(runsSpinner);
        bar.add(benchBtn);
        add(bar, BorderLayout.NORTH);

        // ==== SOUTH (benchmark output) ====
        benchOut.setEditable(false);
        benchOut.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(benchOut);
        scroll.setBorder(BorderFactory.createTitledBorder("Benchmark Results"));
        add(scroll, BorderLayout.SOUTH);

        // actions
        runBtn.addActionListener(e -> runSelected(initialPoints));
        benchBtn.addActionListener(e -> runBenchmarks());
    }

    /* ================= Visual animation (Andrew Worker) ================= */

    private void runSelected(List<Point2D> points) {
        runBtn.setEnabled(false);
        Algorithms a = (Algorithms) algoBox.getSelectedItem();

        HullProgressListener listener = new HullProgressListener() {
            @Override public void onChainsUpdated(List<Point2D> lower, List<Point2D> upper) {
                panel.setLiveChains(lower, upper);
            }
            @Override public void onFinished(List<Point2D> hull) {
                panel.setFinalHull(hull);
                runBtn.setEnabled(true);
            }
        };

        switch (a) {
            case ANDREW -> new AndrewsWorker(points, listener, 80).execute();
            case JARVIS -> {
                // (Optional) you can plug in JarvisWorker for step animation.
                // As a placeholder, compute final hull immediately:
                ConvexHullAlgorithm j = new JarvisGiftWrapping();
                var hull = j.computeConvexHull(points);
                panel.setLiveChains(List.of(), List.of());
                panel.setFinalHull(hull);
                runBtn.setEnabled(true);
            }
        }
    }

    /* ================= Benchmarking in UI ================= */

    private void runBenchmarks() {
        benchBtn.setEnabled(false);

        final String which  = (String) benchAlgoBox.getSelectedItem();
        final int n         = (int) nSpinner.getValue();
        final long seed     = ((Number) seedSpinner.getValue()).longValue();
        final int warmup    = (int) warmSpinner.getValue();
        final int runs      = (int) runsSpinner.getValue();

        // run off-EDT so UI stays responsive
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                appendBenchLine("=== Benchmark ===  n=%d  seed=%d  warmup=%d  runs=%d", n, seed, warmup, runs);

                // fresh random dataset (assignment suggests large sets for stable results)
                List<Point2D> pts = PointGenerator.uniformRandom(n, 1, 10, 1, 10, seed);

                switch (which.toLowerCase()) {
                    case "all" -> {
                        timeOne("andrew", new AndrewMonotoneChain(), pts, warmup, runs);
                        timeOne("jarvis", new JarvisGiftWrapping(),  pts, warmup, runs);
                    }
                    case "jarvis" -> timeOne("jarvis", new JarvisGiftWrapping(),  pts, warmup, runs);
                    case "andrew"  -> timeOne("andrew", new AndrewMonotoneChain(), pts, warmup, runs);
                }

                appendBenchLine("------------------------------");
                return null;
            }

            @Override
            protected void done() {
                benchBtn.setEnabled(true);
            }

            private void timeOne(String name, ConvexHullAlgorithm algo, List<Point2D> pts, int warm, int r) {
                double avg = BenchmarkRunner.avgMillis(algo, pts, warm, r);
                appendBenchLine("Algorithm: %-6s | n=%-7d | avg=%.3f ms", name, pts.size(), avg);
            }

            private void appendBenchLine(String fmt, Object... args) {
                String line = String.format(fmt, args) + System.lineSeparator();
                // publish on EDT
                SwingUtilities.invokeLater(() -> benchOut.append(line));
            }
        }.execute();
    }
}
