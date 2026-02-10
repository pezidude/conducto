package com.example.conducto2.ui.player.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import androidx.annotation.NonNull;

public class ObservableWebView extends WebView {

    public interface OnTransformationChangeListener {
        void onScrollChange(int scrollX, int scrollY);
        void onScaleChange(float scale);
    }

    private OnTransformationChangeListener onTransformationChangeListener;
    private float lastScale = 1.0f;

    public ObservableWebView(@NonNull Context context) {
        super(context);
    }

    public ObservableWebView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnTransformationChangeListener(OnTransformationChangeListener listener) {
        this.onTransformationChangeListener = listener;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // TODO: reimplement with a method that's not deprecated.
        super.onScrollChanged(l, t, oldl, oldt);
        if (onTransformationChangeListener != null) {
            onTransformationChangeListener.onScrollChange(l, t);
            
            // Zooming often triggers a scroll event, so this is a reliable place to check for scale changes.
            float currentScale = getScale();
            if (Math.abs(currentScale - lastScale) > 0.01) { // Use a threshold to avoid minor fluctuations
            }
            onTransformationChangeListener.onScaleChange(currentScale);
            lastScale = currentScale;
        }
    }
}