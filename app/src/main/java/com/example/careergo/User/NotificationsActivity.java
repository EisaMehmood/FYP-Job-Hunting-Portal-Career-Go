package com.example.careergo.User;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Adapters.NotificationsAdapter;
import com.example.careergo.Model.Notification;
import com.example.careergo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private MaterialButton btnAllNotifications, btnUnreadNotifications;
    private RecyclerView rvNotifications;
    private LinearProgressIndicator progressBar;
    private TextView tvEmptyState;
    private BottomNavigationView bottomNavigationView;

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // Adapter and Lists
    private NotificationsAdapter notificationsAdapter;
    private List<Notification> notificationList;
    private List<Notification> filteredNotificationList;

    // State
    private boolean showingAllNotifications = true;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        initializeViews();
        initializeFirebase();
        setupToolbar();
        setupBottomNavigation();
        setupRecyclerView();
        setupButtonListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAuthenticationAndLoadNotifications();
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Buttons
        btnAllNotifications = findViewById(R.id.btnAllNotifications);
        btnUnreadNotifications = findViewById(R.id.btnUnreadNotifications);

        // RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications);

        // Progress and Empty State
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Initialize lists
        notificationList = new ArrayList<>();
        filteredNotificationList = new ArrayList<>();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_jobs) {
                // Open Jobs Activity
                Intent jobsIntent = new Intent(NotificationsActivity.this, JobActivity.class);
                startActivity(jobsIntent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                // Already on Notifications, do nothing
                return true;
            } else if (itemId == R.id.navigation_home) {
                // Open Home Activity
                Intent homeIntent = new Intent(NotificationsActivity.this, UserHome.class);
                startActivity(homeIntent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Open Profile Activity
                Intent profileIntent = new Intent(NotificationsActivity.this, UserProfile.class);
                startActivity(profileIntent);
                return true;
            }
            return false;
        });

        // Set the notifications item as selected by default
        bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);
    }

    private void setupRecyclerView() {
        notificationsAdapter = new NotificationsAdapter(filteredNotificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(notificationsAdapter);
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
            // All button active
            btnAllNotifications.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btnAllNotifications.setTextColor(getResources().getColor(R.color.white));

            // Unread button inactive
            btnUnreadNotifications.setBackgroundColor(getResources().getColor(R.color.white));
            btnUnreadNotifications.setTextColor(getResources().getColor(R.color.primary_color));
        } else {
            // Unread button active
            btnUnreadNotifications.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btnUnreadNotifications.setTextColor(getResources().getColor(R.color.white));

            // All button inactive
            btnAllNotifications.setBackgroundColor(getResources().getColor(R.color.white));
            btnAllNotifications.setTextColor(getResources().getColor(R.color.primary_color));
        }
    }

    private void checkAuthenticationAndLoadNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not authenticated, redirect to login or handle accordingly
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        fetchUserNotifications();

        // Set initial button states
        updateButtonStates();
    }

    private void fetchUserNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);

        mDatabase.child("notifications")
                .orderByChild("userId")
                .equalTo(currentUserId)
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

                        // Sort by timestamp (newest first)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            notificationList.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
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
                        rvNotifications.setVisibility(View.GONE);
                    }
                });
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
            if (showingAllNotifications) {
                tvEmptyState.setText("No notifications yet");
            } else {
                tvEmptyState.setText("No unread notifications");
            }
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);
        }
    }
}