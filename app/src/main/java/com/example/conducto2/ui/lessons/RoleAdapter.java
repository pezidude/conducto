package com.example.conducto2.ui.lessons;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.RoleViewHolder> {

    private List<Role> roles;
    private OnSelectVoicesClickListener listener;

    public interface OnSelectVoicesClickListener {
        void onSelectVoicesClick(int position);
    }

    public RoleAdapter(List<Role> roles, OnSelectVoicesClickListener listener) {
        this.roles = roles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_role, parent, false);
        return new RoleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoleViewHolder holder, int position) {
        Role role = roles.get(position);
        holder.etRoleName.setText(role.getName());
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : role.getSelectedVoicesPerPart().entrySet()) {
            sb.append("Part ").append(entry.getKey()).append(": Voices ").append(entry.getValue().toString()).append("\n");
        }
        holder.tvSelectedVoices.setText(sb.length() > 0 ? sb.toString().trim() : "No voices selected");

        holder.btnSelectVoices.setOnClickListener(v -> listener.onSelectVoicesClick(position));

        holder.etRoleName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                role.setName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public int getItemCount() {
        return roles.size();
    }

    static class RoleViewHolder extends RecyclerView.ViewHolder {
        EditText etRoleName;
        TextView tvSelectedVoices;
        Button btnSelectVoices;

        public RoleViewHolder(@NonNull View itemView) {
            super(itemView);
            etRoleName = itemView.findViewById(R.id.et_role_name);
            tvSelectedVoices = itemView.findViewById(R.id.tv_selected_voices);
            btnSelectVoices = itemView.findViewById(R.id.btn_select_voices);
        }
    }
}