package ui;

import algorithm.Algorithms;
import algorithm.ConvexHullAlgorithm;
import algorithm.andrew.AndrewMonotoneChain;
import algorithm.jarvis.JarvisGiftWrapping;
import algorithm.progress.HullProgressListener;
import algorithm.workers.AndrewsWorker;
import algorithm.workers.JarvisWorker;
import benchmark.BenchmarkRunner;
import io.PointGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class ConvexHullFrame extends JFrame {
    private final ConvexHullPanel panel;

    // visual controls
    private final JButton runBtn   = new JButton("Run");
    private final JButton regenBtn = new JButton("New Points");
    // Only visual algos in this combo (exclude ALL)
    private final JComboBox<Algorithms> algoBox =
            new JComboBox<>(new Algorithms[]{Algorithms.ANDREW, Algorithms.JARVIS});

    // benchmark controls (no warmup)
    private final JButton benchBtn = new JButton("Benchmark");
    private final JComboBox<Algorithms> benchAlgoBox =
            new JComboBox<>(new Algorithms[]{Algorithms.ALL, Algorithms.ANDREW, Algorithms.JARVIS});
    private final JSpinner nSpinner     = new JSpinner(new SpinnerNumberModel(200, 3, 1_000_000, 100));
    private final JSpinner seedSpinner  = new JSpinner(new SpinnerNumberModel(1234, 0, Integer.MAX_VALUE, 1));
    private final JSpinner runsSpinner  = new JSpinner(new SpinnerNumberModel(8, 1, 1000, 1));

    // benchmark output
    private final JTextArea benchOut = new JTextArea(7, 20);

    // current dataset
    private List<Point2D> points;

    public ConvexHullFrame(List<Point2D> points) {
        super("Convex Hull – Visual Mode");
        this.points = points;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // canvas
        panel = new ConvexHullPanel(points, null);
        add(panel, BorderLayout.CENTER);

        // toolbar (top)
        JToolBar bar = new JToolBar();
        bar.add(new JLabel("Algorithm: "));
        bar.add(algoBox);
        bar.add(runBtn);
        bar.add(regenBtn);
        bar.addSeparator(new Dimension(20, 0));
        bar.add(new JLabel("Benchmark "));
        bar.add(new JLabel("algo:"));
        bar.add(benchAlgoBox);
        bar.add(new JLabel(" n:"));
        bar.add(nSpinner);
        bar.add(new JLabel(" seed:"));
        bar.add(seedSpinner);
        bar.add(new JLabel(" runs:"));
        bar.add(runsSpinner);
        bar.add(benchBtn);
        add(bar, BorderLayout.NORTH);

        // benchmark output (bottom)
        benchOut.setEditable(false);
        benchOut.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(benchOut);
        scroll.setBorder(BorderFactory.createTitledBorder("Benchmark Results"));
        add(scroll, BorderLayout.SOUTH);

        // actions
        runBtn.addActionListener(e -> runSelected());
        regenBtn.addActionListener(e -> regeneratePoints());
        benchBtn.addActionListener(e -> runBenchmarks());
    }

    /* ================= Visual animation ================= */

    private void runSelected() {
        runBtn.setEnabled(false);

        HullProgressListener listener = new HullProgressListener() {
            @Override public void onChainsUpdated(List<Point2D> lower, List<Point2D> upper) {
                panel.setLiveChains(lower, upper);
            }
            @Override public void onFinished(List<Point2D> hull) {
                panel.setFinalHull(hull);
                runBtn.setEnabled(true);
            }
        };

        Algorithms a = (Algorithms) algoBox.getSelectedItem();
        if (a == Algorithms.ANDREW) {
            new AndrewsWorker(points, listener, 80).execute();
        } else if (a == Algorithms.JARVIS) {
            new JarvisWorker(points, listener, 80).execute();
        } else {
            runBtn.setEnabled(true); // not reachable (combo excludes ALL)
        }
    }

    private void regeneratePoints() {
        int n     = (int)  nSpinner.getValue();
        long seed = ((Number) seedSpinner.getValue()).longValue();
        points = PointGenerator.uniformRandom(n, 1, 10, 1, 10, seed);
        panel.setPoints(points);
        panel.setLiveChains(List.of(), List.of());
        panel.setFinalHull(List.of());
        benchOut.append(String.format("Regenerated points: n=%d seed=%d%n", n, seed));
    }

    /* ================= Benchmarking in UI (no warmup) ================= */

    private void runBenchmarks() {
        benchBtn.setEnabled(false);

        final Algorithms which = (Algorithms) benchAlgoBox.getSelectedItem();
        final int n         = (int)  nSpinner.getValue();
        final long seed     = ((Number) seedSpinner.getValue()).longValue();
        final int runs      = (int)  runsSpinner.getValue();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                appendBenchLine("=== Benchmark ===  n=%d  seed=%d  runs=%d", n, seed, runs);

                // fresh random dataset for stable comparisons
                List<Point2D> pts = PointGenerator.uniformRandom(n, 1, 10, 1, 10, seed);

                switch (which) {
                    case ALL -> {
                        timeOne("andrew", new AndrewMonotoneChain(), pts, runs);
                        timeOne("jarvis", new JarvisGiftWrapping(),  pts, runs);
                    }
                    case ANDREW -> timeOne("andrew", new AndrewMonotoneChain(), pts, runs);
                    case JARVIS -> timeOne("jarvis", new JarvisGiftWrapping(),  pts, runs);
                }

                appendBenchLine("------------------------------");
                return null;
            }

            @Override
            protected void done() {
                benchBtn.setEnabled(true);
            }

            private void timeOne(String name, ConvexHullAlgorithm algo, List<Point2D> pts, int r) {
                double avg = BenchmarkRunner.avgMillis(algo, pts, r);
                appendBenchLine("Algorithm: %-6s | n=%-7d | avg=%.3f ms", name, pts.size(), avg);
            }

            private void appendBenchLine(String fmt, Object... args) {
                String line = String.format(fmt, args) + System.lineSeparator();
                SwingUtilities.invokeLater(() -> benchOut.append(line));
            }
        }.execute();
    }
}
