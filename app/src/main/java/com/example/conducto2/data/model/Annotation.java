package com.example.conducto2.data.model;

import android.graphics.Canvas;
import java.io.Serializable;

/**
 * Abstract base class for all annotations.
 * Defines the common properties and behaviors, such as drawing capabilities.
 * Implements Serializable to allow annotation objects to be saved.
 */
public abstract class Annotation implements Serializable {

    // Common properties for all annotations can be defined here if needed,
    // for example, a unique ID, creation timestamp, etc.

    /**
     * Abstract method to be implemented by subclasses.
     * This method is responsible for drawing the specific annotation on the canvas.
     * @param canvas The canvas to draw on.
     */
    public abstract void draw(Canvas canvas);
}