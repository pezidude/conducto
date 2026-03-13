package com.example.conducto2.data.manager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

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

public class GeminiManager {

    private static final String API_KEY = ""; // Your key

    // --- 1. Defining the list of fallback models ---
    // The order is important: the system will try the first one, if it fails it will move to the second and so on
    private static final String[] MODEL_FALLBACKS = {
            "gemini-1.5-flash",      // Preferred model (fast and new)
            "gemini-1.5-flash-latest", // First backup (fast and cheap)
            "gemini-pro" // Last backup (stronger, in case the flash models are busy)
    };

    public interface GeminiCallback {
        void onSuccess(String result);
        void onError(Throwable error);
    }

    private static volatile GeminiManager instance;
    private final Executor executor;

    // A variable that will hold the active chat session
    private ChatFutures currentChatSession;


    private final Handler mainHandler;

    private GeminiManager() {
        // Note: we are no longer creating the model here, because the model changes dynamically
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static GeminiManager getInstance() {
        if (instance == null) {
            synchronized (GeminiManager.class) {
                if (instance == null) {
                    instance = new GeminiManager();
                }
            }
        }
        return instance;
    }

    /**
     * Send text only
     */
    public void sendMessage(String prompt, GeminiCallback callback) {
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        // Starting the chain from index 0 (the first model)
        executeRequestWithFallback(content, 0, callback);
    }

    /**
     * Send image
     */
    public void sendMessageWithPhoto(String prompt, Bitmap photo, GeminiCallback callback) {
        Content content = new Content.Builder()
                .addImage(photo)
                .addText(prompt)
                .build();

        // Starting the chain from index 0
        executeRequestWithFallback(content, 0, callback);
    }

    public void sendMessageWithImageView(String prompt, ImageView imageView, GeminiCallback callback) {
        if (imageView == null || imageView.getDrawable() == null) {
            callback.onError(new Exception("ImageView is empty or null"));
            return;
        }
        Bitmap bitmap = drawableToBitmap(imageView.getDrawable());
        if (bitmap != null) {
            sendMessageWithPhoto(prompt, bitmap, callback);
        } else {
            callback.onError(new Exception("Failed to convert Drawable to Bitmap"));
        }
    }

    // --- The heart of the new mechanism ---

    /**
     * A recursive function that tries to send the request.
     * If it fails -> it calls itself again with the next index in the list of models.
     */
    private void executeRequestWithFallback(Content content, int modelIndex, GeminiCallback callback) {
        // Edge case check: if we have gone through all the models and they all failed
        if (modelIndex >= MODEL_FALLBACKS.length) {
            mainHandler.post(() -> callback.onError(new Exception("All AI models failed to respond.")));
            return;
        }

        String currentModelName = MODEL_FALLBACKS[modelIndex];
        Log.d("GeminiManager", "Trying model: " + currentModelName);

        // Creating the specific model for this attempt
        GenerativeModel gm = new GenerativeModel(currentModelName, API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                mainHandler.post(() -> {
                    if (resultText != null) {
                        callback.onSuccess(resultText);
                    } else {
                        // If the response is empty, it is considered an error and we will try the next model
                        executeRequestWithFallback(content, modelIndex + 1, callback);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("GeminiManager", "Model " + currentModelName + " failed: " + t.getMessage());
                // *** This is where the magic happens: instead of returning an error, we try the next model ***
                executeRequestWithFallback(content, modelIndex + 1, callback);
            }
        }, executor);
    }

    // --- Helper functions (unchanged) ---

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }



    public void startNewChat() {
        // We will use the main model (the first in the list) for the chat
        GenerativeModel gm = new GenerativeModel(MODEL_FALLBACKS[0], API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Creating a new and empty chat session
        currentChatSession = model.startChat();
    }


    public void sendChatMessage(String message, GeminiCallback callback) {
        // If a chat has not been started, we will start one automatically
        if (currentChatSession == null) {
            startNewChat();
        }

        Content content = new Content.Builder()
                .addText(message)
                .build();

        // Using the sendMessage of the chat object (and not generateContent)
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