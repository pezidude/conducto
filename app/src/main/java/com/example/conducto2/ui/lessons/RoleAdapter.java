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
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;
import java.util.Map;

public class RoleAdapter extends FirestoreRecyclerAdapter<Role, RoleAdapter.RoleViewHolder> {

    private OnSelectVoicesClickListener listener;
    private OnRoleNameChangedListener nameListener;

    public interface OnSelectVoicesClickListener {
        void onSelectVoicesClick(Role role, String docId);
    }

    public interface OnRoleNameChangedListener {
        void onRoleNameChanged(String docId, String newName);
    }

    public RoleAdapter(@NonNull FirestoreRecyclerOptions<Role> options, 
                       OnSelectVoicesClickListener listener,
                       OnRoleNameChangedListener nameListener) {
        super(options);
        this.listener = listener;
        this.nameListener = nameListener;
    }

    @NonNull
    @Override
    public RoleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_role, parent, false);
        return new RoleViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull RoleViewHolder holder, int position, @NonNull Role role) {
        String docId = getSnapshots().getSnapshot(position).getId();
        
        // Remove existing text watcher to avoid loops or incorrect updates
        if (holder.nameWatcher != null) {
            holder.etRoleName.removeTextChangedListener(holder.nameWatcher);
        }

        holder.etRoleName.setText(role.getName());
        
        StringBuilder sb = new StringBuilder();
        if (role.getSelectedVoicesPerPart() != null) {
            for (Map.Entry<String, List<String>> entry : role.getSelectedVoicesPerPart().entrySet()) {
                sb.append("Part ").append(entry.getKey()).append(": Voices ").append(entry.getValue().toString()).append("\n");
            }
        }
        holder.tvSelectedVoices.setText(sb.length() > 0 ? sb.toString().trim() : "No voices selected");

        holder.btnSelectVoices.setOnClickListener(v -> listener.onSelectVoicesClick(role, docId));

        holder.nameWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (nameListener != null) {
                    nameListener.onRoleNameChanged(docId, s.toString());
                }
            }
        };
        holder.etRoleName.addTextChangedListener(holder.nameWatcher);
    }

    static class RoleViewHolder extends RecyclerView.ViewHolder {
        EditText etRoleName;
        TextView tvSelectedVoices;
        Button btnSelectVoices;
        TextWatcher nameWatcher;

        public RoleViewHolder(@NonNull View itemView) {
            super(itemView);
            etRoleName = itemView.findViewById(R.id.et_role_name);
            tvSelectedVoices = itemView.findViewById(R.id.tv_selected_voices);
            btnSelectVoices = itemView.findViewById(R.id.btn_select_voices);
        }
    }
}