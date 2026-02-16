package com.example.conducto2.data.model;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.List;

public class HighlightAnnotation extends Annotation {

    private Target target;
    private Style style;
    private Content content;

    public HighlightAnnotation() {
        super(null, "highlight");
    }

    public HighlightAnnotation(String id, Target target, Style style, Content content) {
        super(id, "highlight");
        this.target = target;
        this.style = style;
        this.content = content;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public static class Target implements Serializable {
        @PropertyName("start_measure")
        public int startMeasure;
        @PropertyName("start_beat")
        public float startBeat;
        @PropertyName("end_measure")
        public int endMeasure;
        @PropertyName("end_beat")
        public float endBeat;

        public Target() {}

        public Target(int startMeasure, float startBeat, int endMeasure, float endBeat) {
            this.startMeasure = startMeasure;
            this.startBeat = startBeat;
            this.endMeasure = endMeasure;
            this.endBeat = endBeat;
        }
    }

    public static class Style implements Serializable {
        public String color;
        public float opacity;

        public Style() {}

        public Style(String color, float opacity) {
            this.color = color;
            this.opacity = opacity;
        }
    }

    public static class Content implements Serializable {
        public String text;
        public List<String> tags;

        public Content() {}

        public Content(String text, List<String> tags) {
            this.text = text;
            this.tags = tags;
        }
    }
}