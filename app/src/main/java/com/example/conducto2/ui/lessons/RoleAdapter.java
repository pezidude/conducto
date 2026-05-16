package com.example.conducto2.ui.lessons;

import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.Role;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;
import java.util.Map;

public class RoleAdapter extends FirestoreRecyclerAdapter<Role, RoleAdapter.RoleViewHolder> {

    private OnSelectPartsClickListener listener;
    private OnRoleNameClickListener nameListener;
    private OnDeleteRoleClickListener deleteListener;

    public interface OnSelectPartsClickListener {
        void onSelectPartsClick(Role role, String docId);
    }

    public interface OnRoleNameClickListener {
        void onRoleNameClick(Role role, String docId);
    }

    public interface OnDeleteRoleClickListener {
        void onDeleteRoleClick(String docId);
    }

    public RoleAdapter(@NonNull FirestoreRecyclerOptions<Role> options, 
                       OnSelectPartsClickListener listener,
                       OnRoleNameClickListener nameListener,
                       OnDeleteRoleClickListener deleteListener) {
        super(options);
        this.listener = listener;
        this.nameListener = nameListener;
        this.deleteListener = deleteListener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getSnapshots().getSnapshot(position).getId().hashCode();
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
        
        holder.currentDocId = docId;
        String remoteName = role.getName() != null ? role.getName() : "";
        holder.etRoleName.setText(remoteName);

        // Make the EditText act as a button for the dialog
        holder.etRoleName.setFocusable(false);
        holder.etRoleName.setCursorVisible(false);
        holder.etRoleName.setOnClickListener(v -> {
            if (nameListener != null) {
                nameListener.onRoleNameClick(role, docId);
            }
        });

        StringBuilder sb = new StringBuilder();
        if (role.getSelectedPartIds() != null) {
            for (String partId : role.getSelectedPartIds()) {
                sb.append("Part: ").append(partId).append("\n");
            }
        }
        holder.tvSelectedParts.setText(sb.length() > 0 ? sb.toString().trim() : "No parts selected");

        holder.btnSelectParts.setOnClickListener(v -> listener.onSelectPartsClick(role, docId));
        holder.btnDeleteRole.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteRoleClick(docId);
            }
        });
    }

    static class RoleViewHolder extends RecyclerView.ViewHolder {
        EditText etRoleName;
        TextView tvSelectedParts;
        Button btnSelectParts;
        ImageButton btnDeleteRole;
        TextWatcher nameWatcher; // Kept for class compatibility if needed elsewhere
        String currentDocId;
        Runnable updateRunnable;

        public RoleViewHolder(@NonNull View itemView) {
            super(itemView);
            etRoleName = itemView.findViewById(R.id.et_role_name);
            tvSelectedParts = itemView.findViewById(R.id.tv_selected_voices); // Using existing ID
            btnSelectParts = itemView.findViewById(R.id.btn_select_voices); // Using existing ID
            btnDeleteRole = itemView.findViewById(R.id.btn_delete_role);
        }
    }
}
