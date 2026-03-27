package com.example.careergo.Employer;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmployerNotificationsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialButton btnAllNotifications, btnUnreadNotifications;
    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private NotificationsAdapter notificationsAdapter;
    private List<Notification> notificationList;
    private List<Notification> filteredNotificationList;
    private boolean showingAllNotifications = true;
    private String currentEmployerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notifications); // Using the same layout

        initializeViews();
        initializeFirebase();
        setupToolbar();
        setupRecyclerView();
        setupButtonListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAuthenticationAndLoadNotifications();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        btnAllNotifications = findViewById(R.id.btnAllNotifications);
        btnUnreadNotifications = findViewById(R.id.btnUnreadNotifications);
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        notificationList = new ArrayList<>();
        filteredNotificationList = new ArrayList<>();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void checkAuthenticationAndLoadNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not authenticated, handle accordingly
            finish();
            return;
        }

        currentEmployerId = currentUser.getUid();
        fetchEmployerNotifications();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Notifications");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        notificationsAdapter = new NotificationsAdapter(filteredNotificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(notificationsAdapter);
    }

    private void fetchEmployerNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        mDatabase.child("notifications")
                .orderByChild("employerId")
                .equalTo(currentEmployerId)
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
}