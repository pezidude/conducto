package com.example.conducto2.ui.player.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.example.conducto2.data.model.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom view that overlays the sheet music to handle drawing and displaying annotations.
 */
public class AnnotationView extends View {

    /**
     * An enum to define the different annotation modes.
     */
    public enum AnnotationMode {
        /** The user can scroll the view. */
        SCROLL,
        /** The user can draw highlights. */
        HIGHLIGHT,
        /** The user can draw dynamic markings. */
        DYNAMIC
    }

    /** A list of {@link Annotation} objects representing the annotations on the sheet music. */
    private List<Annotation> annotations = new ArrayList<>();
    /** The current annotation mode. */
    private AnnotationMode currentMode = AnnotationMode.SCROLL;
    /** The current color for new annotations. */
    private int currentColor = Color.RED;
    /** The horizontal scroll position of the view. */
    private int scrollX = 0;
    /** The vertical scroll position of the view. */
    private int scrollY = 0;
    /** The current zoom scale of the view. */
    private float scale = 1.0f;

    public AnnotationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The method where the annotations would be drawn on the canvas.
     * Currently, the drawing logic is handled by the webview.
     * @param canvas The canvas on which the background will be drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Drawing logic will be handled by the webview
    }

    /**
     * Sets the scroll position of the view.
     * @param scrollX The horizontal scroll position.
     * @param scrollY The vertical scroll position.
     */
    public void setScroll(int scrollX, int scrollY) {
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        invalidate();
    }

    /**
     * Sets the zoom scale of the view.
     * @param scale The new zoom scale.
     */
    public void setScale(float scale) {
        this.scale = scale;
        invalidate();
    }

    /**
     * Gets the horizontal scroll position.
     * @return The horizontal scroll position.
     */
    public int getScrollXPosition() {
        return this.scrollX;
    }

    /**
     * Gets the vertical scroll position.
     * @return The vertical scroll position.
     */
    public int getScrollYPosition() {
        return this.scrollY;
    }

    /**
     * Gets the current zoom scale.
     * @return The current zoom scale.
     */
    public float getScale() {
        return this.scale;
    }

    /**
     * Sets the current annotation mode.
     * @param mode The new annotation mode.
     */
    public void setMode(AnnotationMode mode) {
        this.currentMode = mode;
        invalidate();
    }

    /**
     * Gets the current annotation mode.
     * @return The current annotation mode.
     */
    public AnnotationMode getMode() {
        return this.currentMode;
    }

    /**
     * Sets the color for new annotations.
     * @param color The new color.
     */
    public void setCurrentColor(int color) {
        this.currentColor = color;
    }

    /**
     * Removes the last added annotation.
     */
    public void undo() {
        if (!annotations.isEmpty()) {
            annotations.remove(annotations.size() - 1);
            invalidate();
        }
    }

    /**
     * Removes all annotations.
     */
    public void clearAnnotations() {
        annotations.clear();
        invalidate();
    }

    /**
     * Returns a copy of the list of annotations.
     * @return A list of annotations.
     */
    public List<Annotation> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    /**
     * Sets the list of annotations, replacing any existing ones.
     * @param loadedAnnotations The new list of annotations.
     */
    public void setAnnotations(List<Annotation> loadedAnnotations) {
        this.annotations = new ArrayList<>(loadedAnnotations);
        invalidate();
    }

    /**
     * A placeholder method for adding text annotations in the future.
     * @param text The text to add.
     * @param x The x-coordinate of the text.
     * @param y The y-coordinate of the text.
     */
    public void addTextAnnotation(String text, float x, float y) {
        // This will be implemented later
    }
}