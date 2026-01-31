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

public class AnnotationToolbarFragment extends Fragment {

    public interface ToolbarListener {
        void onToolSelected(AnnotationView.AnnotationMode mode);
        void onColorSelected(int color);
        void onUndo();
        void onClear();
    }

    private ToolbarListener listener;
    private View colorIndicator;
    private int selectedColor = Color.RED;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarListener) {
            listener = (ToolbarListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ToolbarListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_annotation_toolbar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        colorIndicator = view.findViewById(R.id.color_indicator);
        updateColorIndicator();

        ImageButton btnScroll = view.findViewById(R.id.btn_scroll);
        ImageButton btnBrush = view.findViewById(R.id.btn_brush);
        ImageButton btnText = view.findViewById(R.id.btn_text);
        ImageButton btnColorPicker = view.findViewById(R.id.btn_color_picker);
        ImageButton btnUndo = view.findViewById(R.id.btn_undo);
        ImageButton btnClear = view.findViewById(R.id.btn_clear);

        btnScroll.setOnClickListener(v -> listener.onToolSelected(AnnotationView.AnnotationMode.SCROLL));
        btnBrush.setOnClickListener(v -> listener.onToolSelected(AnnotationView.AnnotationMode.BRUSH));
        btnText.setOnClickListener(v -> listener.onToolSelected(AnnotationView.AnnotationMode.TEXT));
        btnUndo.setOnClickListener(v -> listener.onUndo());
        btnClear.setOnClickListener(v -> listener.onClear());
        btnColorPicker.setOnClickListener(v -> showColorPickerDialog());
        colorIndicator.setOnClickListener(v -> showColorPickerDialog());
    }

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

    private void updateColorIndicator() {
        if (colorIndicator != null) {
            GradientDrawable background = (GradientDrawable) colorIndicator.getBackground().mutate();
             if (background != null) {
                background.setColor(selectedColor);
            }
        }
    }
}