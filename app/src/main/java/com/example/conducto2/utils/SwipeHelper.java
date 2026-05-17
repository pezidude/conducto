package com.example.conducto2.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;

/**
 * SwipeHelper
 * 
 * A reusable utility class that extends {@link ItemTouchHelper.SimpleCallback} to provide 
 * high-performance, visually rich swipe actions for RecyclerView items.
 * 
 * This class encapsulates the complex canvas drawing logic required to render background 
 * colors and action icons behind a swiped item. It supports both left and right swipe 
 * directions and delegates execution back to the host via the {@link SwipeActions} interface.
 * 
 * Features:
 * 1. Clamped Swiping: Prevents items from being swiped off the screen entirely.
 * 2. Visual Feedback: Renders icons and colored backgrounds as the user drags.
 * 3. Custom Thresholds: Configured for a "sleek" feel with high escape velocity requirements.
 */
public class SwipeHelper extends ItemTouchHelper.SimpleCallback {

    /** The listener interface that handles the business logic for swipe events. */
    private final SwipeActions swipeActions;

    /** Resource ID for the icon shown when swiping to the left. */
    private int leftIconResId = R.drawable.ic_edit;

    /** Resource ID for the icon shown when swiping to the right. */
    private int rightIconResId = R.drawable.ic_delete;

    /** Color resource for the left-swipe background. */
    private int leftColorResId = R.color.brand_primary;

    /** Color resource for the right-swipe background. */
    private int rightColorResId = R.color.error;

    /** Color resource for the left icon tint. */
    private int leftIconTintResId = R.color.white;

    /** Color resource for the right icon tint. */
    private int rightIconTintResId = R.color.white;

    /**
     * Interface to be implemented by activities or fragments to handle swipe actions.
     */
    public interface SwipeActions {
        /**
         * Called when an item is swiped to the left.
         * @param position The position of the swiped item.
         */
        void onSwipeLeft(int position);

        /**
         * Called when an item is swiped to the right.
         * @param position The position of the swiped item.
         */
        void onSwipeRight(int position);
    }

    /**
     * Initializes the helper for left and right swiping.
     * @param swipeActions The callback implementation.
     */
    public SwipeHelper(SwipeActions swipeActions) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.swipeActions = swipeActions;
    }

    public void setLeftAction(int iconResId, int colorResId) {
        this.leftIconResId = iconResId;
        this.leftColorResId = colorResId;
    }

    public void setLeftAction(int iconResId, int colorResId, int iconTintResId) {
        this.leftIconResId = iconResId;
        this.leftColorResId = colorResId;
        this.leftIconTintResId = iconTintResId;
    }

    public void setRightAction(int iconResId, int colorResId) {
        this.rightIconResId = iconResId;
        this.rightColorResId = colorResId;
    }

    public void setRightAction(int iconResId, int colorResId, int iconTintResId) {
        this.rightIconResId = iconResId;
        this.rightColorResId = colorResId;
        this.rightIconTintResId = iconTintResId;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // Moving/Reordering is not supported by this implementation.
    }

    /**
     * Triggers the corresponding swipe action based on the drag direction.
     */
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;

        if (direction == ItemTouchHelper.RIGHT) {
            swipeActions.onSwipeRight(position);
        } else {
            swipeActions.onSwipeLeft(position);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // High sensitivity: action triggers at 20% swipe width.
        return 0.2f; 
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        // High resistance: Prevents accidental flings from triggering actions.
        return defaultValue * 10f; 
    }

    /**
     * Performs custom canvas drawing to render the action metadata behind the swiped item.
     * Implements clamping logic to ensure a short, sleek swipe distance.
     */
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        Context context = itemView.getContext();
        
        // Clamp the distance to 1/4 of the screen width for better UI control.
        float maxSwipeDistance = itemView.getWidth() / 4f;
        float clampedDx = Math.max(-maxSwipeDistance, Math.min(dX, maxSwipeDistance));

        Drawable icon;
        ColorDrawable background;

        if (clampedDx > 0) { // Interaction: Swiping right
            icon = ContextCompat.getDrawable(context, rightIconResId);
            if (icon != null) {
                icon = icon.mutate();
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon, ContextCompat.getColor(context, rightIconTintResId));
            }
            background = new ColorDrawable(ContextCompat.getColor(context, rightColorResId));
            
            // Positioning Logic: Center the icon vertically and align it with the swipe edge.
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            
            // Only draw background/icon once the user has swiped past the icon's margin.
            if (clampedDx > iconMargin) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) clampedDx), itemView.getBottom());
                background.draw(c);
                icon.draw(c);
            }
        } else if (clampedDx < 0) { // Interaction: Swiping left
            icon = ContextCompat.getDrawable(context, leftIconResId);
            if (icon != null) {
                icon = icon.mutate();
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon, ContextCompat.getColor(context, leftIconTintResId));
            }
            background = new ColorDrawable(ContextCompat.getColor(context, leftColorResId));
            
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconRight = itemView.getRight() - iconMargin;
            int iconLeft = iconRight - icon.getIntrinsicWidth();

            if (Math.abs(clampedDx) > iconMargin) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getRight() + ((int) clampedDx), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);
                icon.draw(c);
            }
        }

        // Pass the clamped Dx back to the superclass to render the view translation.
        super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive);
    }
}