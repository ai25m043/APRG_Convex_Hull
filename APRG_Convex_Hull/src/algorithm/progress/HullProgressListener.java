// src/main/java/algorithm/progress/HullProgressListener.java
package algorithm.progress;

import java.awt.geom.Point2D;
import java.util.List;

public interface HullProgressListener {
    void onChainsUpdated(List<Point2D> lower, List<Point2D> upper); // während Aufbau
    void onFinished(List<Point2D> hull);                            // final
}
