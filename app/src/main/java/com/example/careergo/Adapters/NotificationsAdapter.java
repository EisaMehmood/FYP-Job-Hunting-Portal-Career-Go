package com.example.careergo.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.careergo.Model.Notification;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private DatabaseReference mDatabase;

    public NotificationsAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void updateList(List<Notification> newList) {
        notificationList = newList;
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNotificationTitle, tvNotificationMessage, tvNotificationTime;
        private Chip chipUnread;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            chipUnread = itemView.findViewById(R.id.chipUnread);
        }

        public void bind(Notification notification) {
            // Set notification type as title
            String title = getNotificationTitle(notification.getType());
            tvNotificationTitle.setText(title);

            tvNotificationMessage.setText(notification.getMessage());

            // Format timestamp
            if (notification.getTimestamp() != null) {
                String timeAgo = getTimeAgo(notification.getTimestamp());
                tvNotificationTime.setText(timeAgo);
            }

            // Show unread indicator
            chipUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Mark as read when clicked
            itemView.setOnClickListener(v -> markAsRead(notification));
        }

        private String getNotificationTitle(String type) {
            switch (type) {
                case "new_application":
                    return "New Job Application";
                case "status_update":
                    return "Application Status Update";
                case "new_job":
                    return "New Job Posted";
                case "system":
                    return "System Notification";
                default:
                    return "Notification";
            }
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just now";
            }
        }

        private void markAsRead(Notification notification) {
            if (!notification.isRead()) {
                mDatabase.child("notifications").child(notification.getId()).child("read")
                        .setValue(true);
            }
        }
    }
}