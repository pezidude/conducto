package com.example.conducto2.data.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Represents a freehand drawing (a brush stroke) on the sheet music.
 * Extends the abstract Annotation class.
 */
public class BrushAnnotation extends Annotation implements Serializable {

    // transient: Path is not serializable, so we handle it manually.
    private transient Path path;
    private int color;
    private float strokeWidth;

    /**
     * Constructs a new BrushAnnotation.
     * @param path The Path object representing the user's drawing.
     * @param color The color of the stroke.
     * @param strokeWidth The width of the stroke.
     */
    public BrushAnnotation(Path path, int color, float strokeWidth) {
        this.path = new Path(path); // Make a copy to ensure immutability
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        canvas.drawPath(path, paint);
    }

    // --- Custom Serialization for non-serializable Path object ---

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // Saves color and strokeWidth

        // Manually serialize the Path by saving its points
        SerializablePath serializablePath = new SerializablePath(path);
        out.writeObject(serializablePath);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Reads color and strokeWidth

        // Manually deserialize the Path
        SerializablePath serializablePath = (SerializablePath) in.readObject();
        this.path = serializablePath.toPath();
    }
}