package com.example.conducto2.ui.player.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.example.conducto2.data.model.Annotation;
import com.example.conducto2.data.model.BrushAnnotation;
import com.example.conducto2.data.model.TextAnnotation;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom view that sits on top of the sheet music and is responsible for
 * drawing and handling user input for annotations.
 */
public class AnnotationView extends View {

    public enum AnnotationMode {
        BRUSH,
        TEXT
    }

    private Path currentPath;
    private Paint brushPaint;
    private List<Annotation> annotations = new ArrayList<>();
    private AnnotationMode currentMode = AnnotationMode.BRUSH;
    private int currentColor = Color.RED;
    private float currentStrokeWidth = 8f;
    private float currentTextSize = 48f;

    public AnnotationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        brushPaint = new Paint();
        brushPaint.setColor(currentColor);
        brushPaint.setStrokeWidth(currentStrokeWidth);
        brushPaint.setStyle(Paint.Style.STROKE);
        brushPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw all saved annotations
        for (Annotation annotation : annotations) {
            annotation.draw(canvas);
        }
        // Draw the current path being drawn by the user
        if (currentPath != null) {
            canvas.drawPath(currentPath, brushPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (currentMode == AnnotationMode.BRUSH) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (currentPath != null) {
                        currentPath.lineTo(x, y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (currentPath != null) {
                        annotations.add(new BrushAnnotation(currentPath, currentColor, currentStrokeWidth));
                        currentPath = null;
                    }
                    break;
                default:
                    return false;
            }
        }
        // In TEXT mode, we let the Activity handle the touch event to show a dialog.

        // Redraw the view
        invalidate();
        return true;
    }

    public void addTextAnnotation(String text, float x, float y) {
        if (text != null && !text.isEmpty()) {
            annotations.add(new TextAnnotation(text, x, y, currentColor, currentTextSize));
            invalidate(); // Redraw with the new text
        }
    }
    
    // --- Getters and Setters for customization ---

    public void setMode(AnnotationMode mode) {
        this.currentMode = mode;
    }

    public AnnotationMode getMode() {
        return this.currentMode;
    }

    public void setCurrentColor(int color) {
        this.currentColor = color;
        brushPaint.setColor(this.currentColor);
    }

    public void setStrokeWidth(float width) {
        this.currentStrokeWidth = width;
        brushPaint.setStrokeWidth(this.currentStrokeWidth);
    }

    public void setTextSize(float size) {
        this.currentTextSize = size;
    }

    public void undo() {
        if (!annotations.isEmpty()) {
            annotations.remove(annotations.size() - 1);
            invalidate();
        }
    }

    public void clearAnnotations() {
        annotations.clear();
        invalidate();
    }

    public List<Annotation> getAnnotations() {
        return new ArrayList<>(annotations); // Return a copy
    }

    public void setAnnotations(List<Annotation> loadedAnnotations) {
        this.annotations = new ArrayList<>(loadedAnnotations);
        invalidate();
    }
}