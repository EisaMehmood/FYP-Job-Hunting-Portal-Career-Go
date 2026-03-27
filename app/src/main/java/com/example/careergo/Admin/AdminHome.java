package com.example.careergo.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminHome extends AppCompatActivity {

    private TextView tvWelcomeHeading, tvStudentCount, tvJobCount, tvPlacementOfficerCount,
            tvNotificationCount, tvPendingApprovalCount, tvCategoryCount;
    private MaterialCardView cvStudent, cvJob, cvMockTest, cvNotification, cvPendingApproval, cvAddCategory;
    private CircleImageView ivProfileImage;
    private ImageView ivLogout;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupWelcomeMessage();
        fetchCounts();
        setupClickListeners();
    }

    private void bindViews() {
        // TextViews
        tvWelcomeHeading = findViewById(R.id.tvWelcomeHeading);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvJobCount = findViewById(R.id.tvJobCount);
        tvPlacementOfficerCount = findViewById(R.id.tvPlacementOfficerCount);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);
        tvPendingApprovalCount = findViewById(R.id.tvPendingApprovalCount);
        tvCategoryCount = findViewById(R.id.tvAddCat); // This should be the TextView inside your category card

        // Cards
        cvStudent = findViewById(R.id.cvStudent);
        cvJob = findViewById(R.id.cvJob);
        cvMockTest = findViewById(R.id.cvMockTest);
        cvNotification = findViewById(R.id.cvNotification);
        cvPendingApproval = findViewById(R.id.cvPendingApproval);
        cvAddCategory = findViewById(R.id.cvAddCat);

        // Images
        ivProfileImage = findViewById(R.id.ivProfileImage);
        ivLogout = findViewById(R.id.ivLogout);
    }

    private void setupWelcomeMessage() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Fetch admin details from database
            mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String welcomeText = "Hello " + (firstName != null ? firstName : "Admin") + "!";
                        tvWelcomeHeading.setText(welcomeText);
                    } else {
                        tvWelcomeHeading.setText("Hello Admin!");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    tvWelcomeHeading.setText("Hello Admin!");
                }
            });
        } else {
            tvWelcomeHeading.setText("Hello Admin!");
        }
    }

    private void fetchCounts() {
        // Fetch student count
        mDatabase.child("users").orderByChild("role").equalTo("Job Seeker")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long studentCount = snapshot.getChildrenCount();
                        tvStudentCount.setText(String.valueOf(studentCount));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvStudentCount.setText("0");
                    }
                });

        // Fetch job count
        mDatabase.child("jobs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long jobCount = snapshot.getChildrenCount();
                tvJobCount.setText(String.valueOf(jobCount));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvJobCount.setText("0");
            }
        });

        // Fetch employer count (placement officers)
        mDatabase.child("users").orderByChild("role").equalTo("Employer")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long employerCount = snapshot.getChildrenCount();
                        tvPlacementOfficerCount.setText(String.valueOf(employerCount));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvPlacementOfficerCount.setText("0");
                    }
                });

        // Fetch notification count
        mDatabase.child("notifications").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long notificationCount = snapshot.getChildrenCount();
                tvNotificationCount.setText(String.valueOf(notificationCount));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvNotificationCount.setText("0");
            }
        });

        // Fetch pending approval count (users where approved = false)
        mDatabase.child("users").orderByChild("approved").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long pendingCount = snapshot.getChildrenCount();
                        tvPendingApprovalCount.setText(String.valueOf(pendingCount));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvPendingApprovalCount.setText("0");
                    }
                });

        // Fetch category count
        fetchCategoryCount();
    }

    private void fetchCategoryCount() {
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long categoryCount = snapshot.getChildrenCount();
                tvCategoryCount.setText(String.valueOf(categoryCount));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvCategoryCount.setText("0");
            }
        });
    }

    private void setupClickListeners() {
        // Student card click
        cvStudent.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHome.this, AdminStudentsActivity.class);
            startActivity(intent);
        });

        // Job card click
        cvJob.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHome.this, AdminJobsActivity.class);
            startActivity(intent);
        });

        // Employer card click
        cvMockTest.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHome.this, AdminEmployersActivity.class);
            startActivity(intent);
        });

        // Notification card click
        cvNotification.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHome.this, AdminNotificationsActivity.class);
            startActivity(intent);
        });

        // Pending Approval card click
        cvPendingApproval.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHome.this, PendingApproval.class);
            startActivity(intent);
        });

        // Add Category card click
        cvAddCategory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHome.this, AddCategoryActivity.class);
            startActivity(intent);
        });

        // Profile image click
        ivProfileImage.setOnClickListener(v -> {
            // Navigate to admin profile
            Intent intent = new Intent(AdminHome.this, AdminProfileActivity.class);
            startActivity(intent);
        });

        // Logout click
        ivLogout.setOnClickListener(v -> {
            logoutUser();
        });
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(AdminHome.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh counts when returning to this activity
        fetchCounts();
    }
}