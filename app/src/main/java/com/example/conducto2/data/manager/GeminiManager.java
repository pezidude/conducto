package com.example.conducto2.data.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.conducto2.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * GeminiManager
 * 
 * An orchestration service for integrating Google's Gemini Generative AI into the application.
 * It manages the lifecycle of AI requests, providing support for both stateless 
 * prompts (text/multimodal) and stateful chat sessions.
 * 
 * Key Features:
 * 1. Recursive Fallback Mechanism: Automatically cycles through a prioritized list of 
 *    AI models if a specific model fails or returns an empty response.
 * 2. Async Execution: Offloads AI network calls to a dedicated single-thread executor 
 *    to maintain UI responsiveness.
 * 3. UI-Thread Marshaling: Uses an Android {@link Handler} to post results back to 
 *    the main thread safely.
 */
public class GeminiManager {

    /** The developer API key retrieved from resources. */
    private final String apiKey;

    /** 
     * Prioritized list of Gemini models. 
     * The system will attempt these in order to ensure maximum uptime even if 
     * certain model versions are deprecated or at capacity.
     */
    private static final String[] MODEL_FALLBACKS = {
            "gemini-2.0-flash",      // Verified working
            "gemini-1.5-pro",        // Stronger available model
            "gemini-1.5-flash",      // Faster available model
            "gemini-1.0-pro"         // Legacy baseline
    };

    /** Callback interface for AI response handling. */
    public interface GeminiCallback {
        void onSuccess(String result);
        void onError(Throwable error);
    }

    /** Singleton instance reference. */
    private static volatile GeminiManager instance;
    
    /** Background thread for AI operations. */
    private final Executor executor;

    /** Reference to an active stateful chat session. */
    private ChatFutures currentChatSession;

    /** Handler for UI thread updates. */
    private final Handler mainHandler;

    private GeminiManager(Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.apiKey = context.getString(R.string.gemini_api_key);
    }

    /**
     * Singleton getter.
     * @param context Application context.
     * @return The global GeminiManager instance.
     */
    public static GeminiManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GeminiManager.class) {
                if (instance == null) {
                    instance = new GeminiManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Dispatches a text-only prompt to the AI.
     * @param prompt The user's question or instruction.
     * @param callback Result listener.
     */
    public void sendMessage(String prompt, GeminiCallback callback) {
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        // Initiation point for the fallback chain (index 0).
        executeRequestWithFallback(content, 0, callback);
    }

    /**
     * Dispatches a multimodal prompt containing both text and image data.
     * @param prompt The text context.
     * @param photo The bitmap image.
     * @param callback Result listener.
     */
    public void sendMessageWithPhoto(String prompt, Bitmap photo, GeminiCallback callback) {
        Content content = new Content.Builder()
                .addImage(photo)
                .addText(prompt)
                .build();

        executeRequestWithFallback(content, 0, callback);
    }

    /**
     * Converts an ImageView's content into a Bitmap before sending to the AI.
     */
    public void sendMessageWithImageView(String prompt, ImageView imageView, GeminiCallback callback) {
        if (imageView == null || imageView.getDrawable() == null) {
            callback.onError(new Exception("ImageView is empty or null"));
            return;
        }
        // Step 1: Utility conversion from UI components to raw pixels.
        Bitmap bitmap = drawableToBitmap(imageView.getDrawable());
        if (bitmap != null) {
            sendMessageWithPhoto(prompt, bitmap, callback);
        } else {
            callback.onError(new Exception("Failed to convert Drawable to Bitmap"));
        }
    }

    /**
     * Core Logic: Recursive model fallback algorithm.
     * Tries the model at modelIndex; on failure, increments the index and recurses.
     */
    private void executeRequestWithFallback(Content content, int modelIndex, GeminiCallback callback) {
        // Stop Condition: If we've exhausted all available models.
        if (modelIndex >= MODEL_FALLBACKS.length) {
            mainHandler.post(() -> callback.onError(new Exception("All AI models failed to respond.")));
            return;
        }

        String currentModelName = MODEL_FALLBACKS[modelIndex];
        Log.d("GeminiManager", "Trying model: " + currentModelName);

        GenerativeModel gm = new GenerativeModel(currentModelName, apiKey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                mainHandler.post(() -> {
                    // Logic: Empty responses are treated as partial failures to trigger the next model.
                    if (resultText != null) {
                        callback.onSuccess(resultText);
                    } else {
                        executeRequestWithFallback(content, modelIndex + 1, callback);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("GeminiManager", "Model " + currentModelName + " failed: " + t.getMessage());
                // Fallback Trigger: Recursive call to attempt the next model in the priority list.
                executeRequestWithFallback(content, modelIndex + 1, callback);
            }
        }, executor);
    }

    /**
     * Utility: Manually draws a Drawable into a new Bitmap.
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        // Safety: Ensure bitmap has at least 1x1 dimensions.
        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Initializes a new stateful chat session using the primary model.
     */
    public void startNewChat() {
        GenerativeModel gm = new GenerativeModel(MODEL_FALLBACKS[0], apiKey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        currentChatSession = model.startChat();
    }

    /**
     * Sends a message within a persistent chat conversation.
     * Auto-initializes the session if it hasn't been started.
     */
    public void sendChatMessage(String message, GeminiCallback callback) {
        if (currentChatSession == null) {
            startNewChat();
        }

        Content content = new Content.Builder()
                .addText(message)
                .build();

        ListenableFuture<GenerateContentResponse> response = currentChatSession.sendMessage(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                mainHandler.post(() -> {
                    if (resultText != null) {
                        callback.onSuccess(resultText);
                    } else {
                        callback.onError(new Exception("Empty response"));
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("GeminiManager", "Chat Error: " + t.getMessage());
                mainHandler.post(() -> callback.onError(t));
            }
        }, executor);
    }
}