package com.example.conducto2.data.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class DynamicAnnotation extends Annotation {

    private Target target;
    private Content content;

    public DynamicAnnotation() {
        super(null, "ghost_dynamic");
    }

    public DynamicAnnotation(String id, Target target, Content content) {
        super(id, "ghost_dynamic");
        this.target = target;
        this.content = content;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public static class Target implements Serializable {
        @PropertyName("note_id")
        public String noteId;

        public Target() {}

        public Target(String noteId) {
            this.noteId = noteId;
        }
    }

    public static class Content implements Serializable {
        public String symbol;
        @PropertyName("original_was")
        public String originalWas;

        public Content() {}

        public Content(String symbol, String originalWas) {
            this.symbol = symbol;
            this.originalWas = originalWas;
        }
    }
}