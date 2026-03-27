package com.example.careergo.Admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.careergo.Adapters.NotificationsAdapter;
import com.example.careergo.Model.Notification;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminNotificationsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialButton btnAllNotifications, btnUnreadNotifications;
    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private DatabaseReference mDatabase;
    private NotificationsAdapter notificationsAdapter;
    private List<Notification> notificationList;
    private List<Notification> filteredNotificationList;
    private boolean showingAllNotifications = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notifications);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchNotifications();
        setupButtonListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        btnAllNotifications = findViewById(R.id.btnAllNotifications);
        btnUnreadNotifications = findViewById(R.id.btnUnreadNotifications);
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        notificationList = new ArrayList<>();
        filteredNotificationList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        notificationsAdapter = new NotificationsAdapter(filteredNotificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(notificationsAdapter);
    }

    private void fetchNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        notificationList.clear();
                        for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                            Notification notification = notificationSnapshot.getValue(Notification.class);
                            if (notification != null) {
                                notification.setId(notificationSnapshot.getKey());
                                notificationList.add(notification);
                            }
                        }
                        filterNotificationsByReadStatus();
                        progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        tvEmptyState.setText("Error loading notifications");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupButtonListeners() {
        btnAllNotifications.setOnClickListener(v -> {
            showingAllNotifications = true;
            updateButtonStates();
            filterNotificationsByReadStatus();
        });

        btnUnreadNotifications.setOnClickListener(v -> {
            showingAllNotifications = false;
            updateButtonStates();
            filterNotificationsByReadStatus();
        });
    }

    private void updateButtonStates() {
        if (showingAllNotifications) {
            btnAllNotifications.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btnAllNotifications.setTextColor(getResources().getColor(R.color.white));
            btnUnreadNotifications.setBackgroundColor(getResources().getColor(R.color.transparent));
            btnUnreadNotifications.setTextColor(getResources().getColor(R.color.primary_color));
        } else {
            btnUnreadNotifications.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btnUnreadNotifications.setTextColor(getResources().getColor(R.color.white));
            btnAllNotifications.setBackgroundColor(getResources().getColor(R.color.transparent));
            btnAllNotifications.setTextColor(getResources().getColor(R.color.primary_color));
        }
    }

    private void filterNotificationsByReadStatus() {
        filteredNotificationList.clear();
        for (Notification notification : notificationList) {
            if (showingAllNotifications || !notification.isRead()) {
                filteredNotificationList.add(notification);
            }
        }
        notificationsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredNotificationList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            tvEmptyState.setText(showingAllNotifications ? "No notifications" : "No unread notifications");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }
}