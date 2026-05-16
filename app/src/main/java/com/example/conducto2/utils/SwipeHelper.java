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
 * A utility class to add swipe actions to a RecyclerView.
 * This class encapsulates the drawing of the swipe background and icons,
 * and delegates the swipe actions to a listener.
 */
public class SwipeHelper extends ItemTouchHelper.SimpleCallback {

    private final SwipeActions swipeActions;
    private int leftIconResId = R.drawable.ic_edit;
    private int rightIconResId = R.drawable.ic_delete;
    private int leftColorResId = R.color.brand_primary;
    private int rightColorResId = R.color.error;
    private int leftIconTintResId = R.color.white;
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
        return false;
    }

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
        return 0.2f; // Require only 20% swipe to trigger action for a sleeker feel
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 10f; // Make it very hard to swipe off by accident
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        Context context = itemView.getContext();
        
        // Clamp the distance to 1/4 of the screen width to keep it short and sleek.
        float maxSwipeDistance = itemView.getWidth() / 4f;
        float clampedDx = Math.max(-maxSwipeDistance, Math.min(dX, maxSwipeDistance));

        Drawable icon;
        ColorDrawable background;

        if (clampedDx > 0) { // Swiping right
            icon = ContextCompat.getDrawable(context, rightIconResId);
            if (icon != null) {
                icon = icon.mutate();
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon, ContextCompat.getColor(context, rightIconTintResId));
            }
            background = new ColorDrawable(ContextCompat.getColor(context, rightColorResId));
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            
            // Only draw if we have swiped enough to show the icon
            if (clampedDx > iconMargin) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) clampedDx), itemView.getBottom());
                background.draw(c);
                icon.draw(c);
            }
        } else if (clampedDx < 0) { // Swiping left
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

        super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive);
    }
}
