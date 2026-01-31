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
 * A utility class to add swipe-to-edit and swipe-to-delete functionality to a RecyclerView.
 * This class encapsulates the drawing of the swipe background and icons,
 * and delegates the swipe actions to a listener.
 */
public class SwipeHelper extends ItemTouchHelper.SimpleCallback {

    private final SwipeActions swipeActions;

    /**
     * Interface to be implemented by activities or fragments to handle swipe actions.
     */
    public interface SwipeActions {
        /**
         * Called when an item is swiped to the left (for editing).
         * @param position The position of the swiped item.
         */
        void onSwipeLeft(int position);

        /**
         * Called when an item is swiped to the right (for deleting).
         * @param position The position of the swiped item.
         */
        void onSwipeRight(int position);
    }

    public SwipeHelper(SwipeActions swipeActions) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.swipeActions = swipeActions;
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
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        Context context = itemView.getContext();
        Drawable icon;
        ColorDrawable background;

        if (dX > 0) { // Swiping right (delete)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
            background = new ColorDrawable(Color.RED);
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
        } else { // Swiping left (edit)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
            background = new ColorDrawable(Color.BLUE);
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconRight = itemView.getRight() - iconMargin;
            int iconLeft = iconRight - icon.getIntrinsicWidth();
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
        }
        background.draw(c);
        icon.draw(c);
    }
}
