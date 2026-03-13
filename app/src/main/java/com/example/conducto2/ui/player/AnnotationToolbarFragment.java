package com.example.conducto2.ui.player;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.conducto2.R;
import com.example.conducto2.ui.player.widget.AnnotationView;

/**
 * A fragment that provides a toolbar for annotation controls, including tool selection, color picking, undo, and clear.
 */
public class AnnotationToolbarFragment extends Fragment {

    /**
     * An interface for the activity to implement to receive callbacks from the toolbar.
     */
    public interface ToolbarListener {
        /**
         * Called when a new annotation tool is selected.
         * @param mode The selected annotation mode.
         */
        void onToolSelected(AnnotationView.AnnotationMode mode);
        /**
         * Called when a new color is selected.
         * @param color The selected color.
         */
        void onColorSelected(int color);
        /** Called when the undo button is clicked. */
        void onUndo();
        /** Called when the clear button is clicked. */
        void onClear();
    }

    /** The listener instance to send callbacks to. */
    private ToolbarListener listener;
    /** A view that displays the currently selected color. */
    private View colorIndicator;
    /** The currently selected color for annotations. */
    private int selectedColor = Color.RED;

    /**
     * Attaches the fragment to the context and ensures the context implements {@link ToolbarListener}.
     * @param context The context to attach to.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarListener) {
            listener = (ToolbarListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ToolbarListener");
        }
    }

    /**
     * Inflates the layout for the fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_annotation_toolbar, container, false);
    }

    /**
     * Sets up the views and click listeners for the toolbar buttons.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        colorIndicator = view.findViewById(R.id.color_indicator);
        updateColorIndicator();

        ImageButton btnScroll = view.findViewById(R.id.btn_scroll);
        ImageButton btnHighlight = view.findViewById(R.id.btn_highlight);
        ImageButton btnDynamic = view.findViewById(R.id.btn_dynamic);
        ImageButton btnColorPicker = view.findViewById(R.id.btn_color_picker);
        ImageButton btnUndo = view.findViewById(R.id.btn_undo);
        ImageButton btnClear = view.findViewById(R.id.btn_clear);

        btnScroll.setOnClickListener(v -> listener.onToolSelected(AnnotationView.AnnotationMode.SCROLL));
        btnHighlight.setOnClickListener(v -> listener.onToolSelected(AnnotationView.AnnotationMode.HIGHLIGHT));
        btnDynamic.setOnClickListener(v -> listener.onToolSelected(AnnotationView.AnnotationMode.DYNAMIC));
        btnUndo.setOnClickListener(v -> listener.onUndo());
        btnClear.setOnClickListener(v -> listener.onClear());
        btnColorPicker.setOnClickListener(v -> showColorPickerDialog());
        colorIndicator.setOnClickListener(v -> showColorPickerDialog());
    }

    /**
     * Displays a dialog for the user to select an annotation color.
     */
    private void showColorPickerDialog() {
        // Simple dialog with a few color choices
        final int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.BLACK, Color.YELLOW};
        String[] colorNames = {"Red", "Blue", "Green", "Black", "Yellow"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Color")
                .setItems(colorNames, (dialog, which) -> {
                    selectedColor = colors[which];
                    updateColorIndicator();
                    listener.onColorSelected(selectedColor);
                });
        builder.create().show();
    }

    /**
     * Updates the color of the color indicator view to reflect the currently selected color.
     */
    private void updateColorIndicator() {
        if (colorIndicator != null) {
            GradientDrawable background = (GradientDrawable) colorIndicator.getBackground().mutate();
             if (background != null) {
                background.setColor(selectedColor);
            }
        }
    }
}