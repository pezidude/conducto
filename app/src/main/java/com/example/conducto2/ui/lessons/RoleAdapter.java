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

/**
 * RoleAdapter
 * 
 * A specialized RecyclerView adapter that leverages the FirebaseUI FirestoreRecyclerAdapter 
 * to provide a real-time reactive interface for managing draft roles.
 * 
 * This class binds 'Role' documents from Firestore to the 'item_role' layout. 
 * It manages interaction callbacks for editing role names, selecting instrumental parts, 
 * and deleting roles entirely.
 */
public class RoleAdapter extends FirestoreRecyclerAdapter<Role, RoleAdapter.RoleViewHolder> {

    /** Listener for instrumental part selection clicks. */
    private OnSelectPartsClickListener listener;

    /** Listener for role name edit clicks. */
    private OnRoleNameClickListener nameListener;

    /** Listener for role deletion clicks. */
    private OnDeleteRoleClickListener deleteListener;

    /** Interface definition for part selection callback. */
    public interface OnSelectPartsClickListener {
        /**
         * Triggered when the "Select Parts" button is clicked.
         * @param role The role being modified.
         * @param docId The Firestore document ID of the role.
         */
        void onSelectPartsClick(Role role, String docId);
    }

    /** Interface definition for name edit callback. */
    public interface OnRoleNameClickListener {
        /**
         * Triggered when the role name EditText is clicked.
         * @param role The role being modified.
         * @param docId The Firestore document ID of the role.
         */
        void onRoleNameClick(Role role, String docId);
    }

    /** Interface definition for deletion callback. */
    public interface OnDeleteRoleClickListener {
        /**
         * Triggered when the delete icon is clicked.
         * @param docId The Firestore document ID of the role.
         */
        void onDeleteRoleClick(String docId);
    }

    /**
     * Constructs a new RoleAdapter with Firestore options and required interaction listeners.
     * @param options The Firestore query options.
     * @param listener Callback for part selection.
     * @param nameListener Callback for name editing.
     * @param deleteListener Callback for deletion.
     */
    public RoleAdapter(@NonNull FirestoreRecyclerOptions<Role> options, 
                       OnSelectPartsClickListener listener,
                       OnRoleNameClickListener nameListener,
                       OnDeleteRoleClickListener deleteListener) {
        super(options);
        this.listener = listener;
        this.nameListener = nameListener;
        this.deleteListener = deleteListener;
        // Enables optimization based on unique hash codes derived from Firestore document IDs.
        setHasStableIds(true);
    }

    /**
     * Generates a unique stable ID based on the hash code of the Firestore document ID.
     * @param position The position of the item in the adapter.
     * @return A long hash of the document ID.
     */
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

    /**
     * Binds the data from a Role model object to the view holder's UI elements.
     * It also configures click listeners to bridge UI interactions back to the parent Activity.
     * 
     * @param holder The ViewHolder to populate.
     * @param position The position of the item.
     * @param role The Role object fetched from Firestore.
     */
    @Override
    protected void onBindViewHolder(@NonNull RoleViewHolder holder, int position, @NonNull Role role) {
        // Step 1: Extract the unique document ID for this item.
        String docId = getSnapshots().getSnapshot(position).getId();
        
        holder.currentDocId = docId;
        String remoteName = role.getName() != null ? role.getName() : "";
        holder.etRoleName.setText(remoteName);

        // Step 2: Configure the EditText to act as a button.
        // Direct editing is disabled to force the use of a controlled dialog for database updates.
        holder.etRoleName.setFocusable(false);
        holder.etRoleName.setCursorVisible(false);
        holder.etRoleName.setOnClickListener(v -> {
            if (nameListener != null) {
                nameListener.onRoleNameClick(role, docId);
            }
        });

        // Step 3: Construct a summary string of the currently selected XML parts.
        StringBuilder sb = new StringBuilder();
        if (role.getSelectedPartIds() != null) {
            for (String partId : role.getSelectedPartIds()) {
                sb.append("Part: ").append(partId).append("\n");
            }
        }
        // Update the display text or show a placeholder if no parts are selected.
        holder.tvSelectedParts.setText(sb.length() > 0 ? sb.toString().trim() : "No parts selected");

        // Step 4: Bind the interaction buttons to their respective listeners.
        holder.btnSelectParts.setOnClickListener(v -> listener.onSelectPartsClick(role, docId));
        holder.btnDeleteRole.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteRoleClick(docId);
            }
        });
    }

    /**
     * ViewHolder pattern implementation for the Role item.
     * Holds references to all interactive UI components in the item layout.
     */
    static class RoleViewHolder extends RecyclerView.ViewHolder {
        /** Input field for the role's display name. */
        EditText etRoleName;
        
        /** Text display showing the list of selected instrumental parts. */
        TextView tvSelectedParts;
        
        /** Button to open the part selection multi-choice dialog. */
        Button btnSelectParts;
        
        /** Image button to delete the role from the draft collection. */
        ImageButton btnDeleteRole;
        
        /** Placeholder for a text watcher if reactive typing is needed. */
        TextWatcher nameWatcher; 
        
        /** Stores the document ID currently bound to this view holder. */
        String currentDocId;
        
        /** Placeholder for timed update tasks. */
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