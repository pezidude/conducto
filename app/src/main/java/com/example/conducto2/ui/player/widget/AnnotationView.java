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

public class AnnotationView extends View {

    public enum AnnotationMode { BRUSH, TEXT, SCROLL }

    private Path currentPath;
    private Paint brushPaint;
    private List<Annotation> annotations = new ArrayList<>();
    private AnnotationMode currentMode = AnnotationMode.SCROLL;
    private int currentColor = Color.RED;
    private float currentStrokeWidth = 8f;
    private float currentTextSize = 48f;

    // Transformation properties
    private int scrollX = 0;
    private int scrollY = 0;
    private float scale = 1.0f;

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
        
        canvas.save();
        
        // **Corrected Order of Operations**
        // 1. Translate the canvas to the current scroll position.
        canvas.translate(-scrollX, -scrollY);
        // 2. Scale the canvas around the new (0,0) point.
        canvas.scale(scale, scale);

        // Always draw annotations regardless of the mode
        for (Annotation annotation : annotations) {
            annotation.draw(canvas);
        }
        // Only draw the "in-progress" path if in Brush mode
        if (currentMode == AnnotationMode.BRUSH && currentPath != null) {
            canvas.drawPath(currentPath, brushPaint);
        }
        
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMode == AnnotationMode.SCROLL) {
            return false; // Pass touch event to the WebView below
        }

        // Convert screen coordinates to "world" coordinates by accounting for scale and scroll
        float x = (event.getX() / scale) + scrollX;
        float y = (event.getY() / scale) + scrollY;

        if (currentMode == AnnotationMode.BRUSH) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (currentPath != null) currentPath.lineTo(x, y);
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
        
        invalidate();
        return true;
    }

    public void addTextAnnotation(String text, float x, float y) {
        if (text != null && !text.isEmpty()) {
            annotations.add(new TextAnnotation(text, x, y, currentColor, currentTextSize));
            invalidate();
        }
    }

    // --- Transformation Setters ---
    public void setScroll(int scrollX, int scrollY) {
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        invalidate();
    }
    
    public void setScale(float scale) {
        this.scale = scale;
        invalidate();
    }

    public int getScrollXPosition() { return this.scrollX; }
    public int getScrollYPosition() { return this.scrollY; }
    public float getScale() { return this.scale; }


    // --- Customization ---
    public void setMode(AnnotationMode mode) {
        this.currentMode = mode;
        if (mode == AnnotationMode.SCROLL) currentPath = null;
        invalidate();
    }

    public AnnotationMode getMode() { return this.currentMode; }
    public void setCurrentColor(int color) {
        this.currentColor = color;
        brushPaint.setColor(this.currentColor);
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
    public List<Annotation> getAnnotations() { return new ArrayList<>(annotations); }
    public void setAnnotations(List<Annotation> loadedAnnotations) {
        this.annotations = new ArrayList<>(loadedAnnotations);
        invalidate();
    }
}