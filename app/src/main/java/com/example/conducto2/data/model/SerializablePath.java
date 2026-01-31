package com.example.conducto2.data.model;

import android.graphics.Path;
import android.graphics.PathMeasure;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to make the android.graphics.Path object serializable.
 * It works by deconstructing the Path into a list of points and then
 * reconstructing it. This is a simplified version and may not perfectly
 * preserve all Path features (like curves), but works well for freehand lines.
 */
public class SerializablePath implements Serializable {
    private List<float[]> points;

    public SerializablePath(Path path) {
        this.points = new ArrayList<>();
        PathMeasure pm = new PathMeasure(path, false);
        float[] coords = new float[2];
        // Approximate the path with a series of points
        for (float i = 0; i < pm.getLength(); i++) {
            pm.getPosTan(i, coords, null);
            points.add(new float[]{coords[0], coords[1]});
        }
    }

    public Path toPath() {
        Path path = new Path();
        if (points != null && !points.isEmpty()) {
            path.moveTo(points.get(0)[0], points.get(0)[1]);
            for (int i = 1; i < points.size(); i++) {
                path.lineTo(points.get(i)[0], points.get(i)[1]);
            }
        }
        return path;
    }
}