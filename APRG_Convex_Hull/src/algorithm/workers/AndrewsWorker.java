// src/main/java/algorithm/workers/AndrewsWorker.java
package algorithm.workers;

import algorithm.progress.HullProgressListener;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

/** Führt Andrew step-by-step aus und publiziert nach jedem Push/Pop. */
public class AndrewsWorker extends SwingWorker<List<Point2D>, AndrewsWorker.State> {
    public static class State {
        public final List<Point2D> lower, upper;
        public State(List<Point2D> lower, List<Point2D> upper) { this.lower=lower; this.upper=upper; }
    }

    private final List<Point2D> input;
    private final HullProgressListener listener;
    private final long delayMs;
    private static final double EPS = 1e-12;

    public AndrewsWorker(List<Point2D> input, HullProgressListener listener, long delayMs) {
        this.input = input; this.listener = listener; this.delayMs = delayMs;
    }

    @Override protected List<Point2D> doInBackground() {
        if (input == null || input.size() <= 1) return input == null ? List.of() : new ArrayList<>(input);
        List<Point2D> pts = new ArrayList<>(input);
        pts.sort(Comparator.comparingDouble(Point2D::getX).thenComparingDouble(Point2D::getY));
        pts = dedup(pts);

        List<Point2D> lower = new ArrayList<>();
        for (Point2D p : pts) {
            while (lower.size() >= 2 && orient(lower.get(lower.size()-2), lower.get(lower.size()-1), p) <= 0) {
                lower.remove(lower.size()-1); publish(snap(lower, null)); sleep();
            }
            lower.add(p); publish(snap(lower, null)); sleep();
        }

        List<Point2D> upper = new ArrayList<>();
        for (int i = pts.size()-1; i >= 0; i--) {
            Point2D p = pts.get(i);
            while (upper.size() >= 2 && orient(upper.get(upper.size()-2), upper.get(upper.size()-1), p) <= 0) {
                upper.remove(upper.size()-1); publish(snap(lower, upper)); sleep();
            }
            upper.add(p); publish(snap(lower, upper)); sleep();
        }

        if (!lower.isEmpty()) lower.remove(lower.size()-1);
        if (!upper.isEmpty()) upper.remove(upper.size()-1);
        lower.addAll(upper);
        return lower;
    }

    @Override protected void process(List<State> chunks) {
        if (chunks.isEmpty()) return;
        State s = chunks.get(chunks.size()-1);
        listener.onChainsUpdated(s.lower, s.upper==null? List.of(): s.upper);
    }

    @Override protected void done() {
        try { listener.onFinished(get()); } catch (Exception e) { listener.onFinished(List.of()); }
    }

    private void sleep(){ if (delayMs>0) try { Thread.sleep(delayMs);} catch(InterruptedException ignored){} }
    private State snap(List<Point2D> lower, List<Point2D> upper){
        return new State(copy(lower), upper==null?null:copy(upper));
    }
    private static List<Point2D> copy(List<Point2D> l){ return l.stream().collect(Collectors.toList()); }
    private static List<Point2D> dedup(List<Point2D> s){
        List<Point2D> out=new ArrayList<>(s.size()); Point2D prev=null;
        for (Point2D p:s){ if(prev==null||Math.abs(prev.getX()-p.getX())>EPS||Math.abs(prev.getY()-p.getY())>EPS){out.add(p);prev=p;} }
        return out;
    }
    private static double orient(Point2D a, Point2D b, Point2D c){
        double x1=b.getX()-a.getX(), y1=b.getY()-a.getY(), x2=c.getX()-a.getX(), y2=c.getY()-a.getY();
        double cross=x1*y2-y1*x2; if (Math.abs(cross)<EPS) return 0.0; return cross;
    }
}
