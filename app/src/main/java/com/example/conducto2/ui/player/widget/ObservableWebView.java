package com.example.conducto2.ui.player.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import androidx.annotation.NonNull;

/**
 * A custom WebView that provides callbacks for scroll and scale changes.
 */
public class ObservableWebView extends WebView {

    /**
     * An interface to listen for transformation changes (scroll and scale).
     */
    public interface OnTransformationChangeListener {
        /**
         * Called when the scroll position of the view changes.
         * @param scrollX The new horizontal scroll position.
         * @param scrollY The new vertical scroll position.
         */
        void onScrollChange(int scrollX, int scrollY);
        /**
         * Called when the scale of the view changes.
         * @param scale The new scale factor.
         */
        void onScaleChange(float scale);
    }

    /** The listener instance for transformation changes. */
    private OnTransformationChangeListener onTransformationChangeListener;
    /** The last recorded scale of the WebView, used to detect changes. */
    private float lastScale = 1.0f;

    public ObservableWebView(@NonNull Context context) {
        super(context);
    }

    public ObservableWebView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the listener for transformation changes.
     * @param listener The listener to set.
     */
    public void setOnTransformationChangeListener(OnTransformationChangeListener listener) {
        this.onTransformationChangeListener = listener;
    }

    /**
     * This method is called when the scroll position of the view changes.
     * It notifies the listener of the scroll change and also checks for scale changes, as zooming often triggers scroll events.
     * @param l Current horizontal scroll origin.
     * @param t Current vertical scroll origin.
     * @param oldl Previous horizontal scroll origin.
     * @param oldt Previous vertical scroll origin.
     */
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