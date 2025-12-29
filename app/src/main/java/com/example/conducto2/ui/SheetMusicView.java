package com.example.conducto2.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SheetMusicView extends WebView {

    private boolean isEngineReady = false;
    private String pendingXmlData = null;

    public SheetMusicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false); // Hide the ugly android zoom buttons

        // Ensure we load the asset file
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isEngineReady = true;
                if (pendingXmlData != null) {
                    loadXml(pendingXmlData);
                    pendingXmlData = null;
                }
            }
        });

        // Load the viewer from assets
        loadUrl("file:///android_asset/viewer.html");
    }

    // --- Public API for the Activity ---

    /**
     * Reads a MusicXML file string and sends it to the engine.
     */
    public void loadXml(String xmlData) {
        if (!isEngineReady) {
            pendingXmlData = xmlData;
            return;
        }
        // Escape the string to be safe for JS injection
        String escapedXml = xmlData.replace("`", "\\`").replace("$", "\\$");
        // Call the JS function defined in viewer.html
        evaluateJavascript("loadScore(`" + escapedXml + "`);", null);
    }

    /**
     * Advance the visual cursor to the next note/measure.
     */
    public void nextCursor() {
        evaluateJavascript("nextCursor();", null);
    }

    public void resetCursor() {
        evaluateJavascript("resetCursor();", null);
    }
}