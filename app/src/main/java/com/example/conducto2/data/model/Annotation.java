package com.example.conducto2.data.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

/**
 * Abstract base class for all annotations.
 * Defines the common properties such as ID and type.
 * Implements Serializable to allow annotation objects to be passed between components.
 */
public abstract class Annotation implements Serializable {

    private String annotationId;
    private String type;

    public Annotation() {
        // Default constructor required for Firestore
    }

    public Annotation(String annotationId, String type) {
        this.annotationId = annotationId;
        this.type = type;
    }

    @PropertyName("annotation_id")
    public String getAnnotationId() {
        return annotationId;
    }

    @PropertyName("annotation_id")
    public void setAnnotationId(String annotationId) {
        this.annotationId = annotationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}