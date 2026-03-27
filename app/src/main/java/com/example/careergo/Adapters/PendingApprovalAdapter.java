package com.example.careergo.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Model.UserModel;
import com.example.careergo.R;

import com.example.careergo.Utility.EmailSender;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class PendingApprovalAdapter extends RecyclerView.Adapter<PendingApprovalAdapter.ViewHolder> {

    public interface ApproveCallback {
        void onApprove(UserModel user);
    }

    private List<UserModel> userList;
    private Context context;
    private ApproveCallback callback;

    public PendingApprovalAdapter(List<UserModel> userList, Context context, ApproveCallback callback) {
        this.userList = userList;
        this.context = context;
        this.callback = callback;
    }

    @NonNull
    @Override
    public PendingApprovalAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pending_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingApprovalAdapter.ViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.tvName.setText(user.fname);
        holder.tvEmail.setText(user.email);
        holder.tvRole.setText(user.role);

        holder.btnApprove.setOnClickListener(v -> {
            if (callback != null) callback.onApprove(user);
        });
        // Animate item (fade + slide from right)
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(200);
        holder.itemView.animate()
                .alpha(1f)
                .translationX(0)
                .setDuration(500)
                .start();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        Button btnApprove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            btnApprove = itemView.findViewById(R.id.btnApprove);
        }
    }
}