package com.example.conducto2.data.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.Serializable;

/**
 * Represents a text annotation placed on the sheet music.
 * Extends the abstract Annotation class.
 */
public class TextAnnotation extends Annotation implements Serializable {

    private String text;
    private float x, y;
    private int color;
    private float textSize;

    /**
     * Constructs a new TextAnnotation.
     * @param text The text content.
     * @param x The x-coordinate of the text's starting point.
     * @param y The y-coordinate of the text's baseline.
     * @param color The color of the text.
     * @param textSize The font size of the text.
     */
    public TextAnnotation(String text, float x, float y, int color, float textSize) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.textSize = textSize;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        canvas.drawText(text, x, y, paint);
    }

    // Getters and setters can be added here if you need to edit annotations later
    public String getText() {
        return text;
    }
}