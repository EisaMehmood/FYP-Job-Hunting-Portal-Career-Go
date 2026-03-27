package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.Model.CompanyProfile;
import com.example.careergo.Model.Job;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployerHome extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    // UI Components
    private TextView tvWelcomeHeading, tvStudentCount, tvJobCount, tvMockTestCount,
            tvNotificationCount;
    private CircleImageView ivProfileImage;
    private ImageView ivLogout;

    // Profile completion check
    private boolean isCompanyProfileComplete = false;

    // ValueEventListeners for live updates
    private ValueEventListener userDataListener;
    private ValueEventListener profileCompletionListener;
    private ValueEventListener applicationsCountListener;
    private ValueEventListener jobCountListener;
    private ValueEventListener mockTestCountListener;
    private ValueEventListener notificationCountListener;

    // Store employer ID for cleanup
    private String currentEmployerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_home);

        initializeFirebase();
        initializeViews();
        setupClickListeners();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentEmployerId = currentUser.getUid();
            loadUserData();
            checkCompanyProfileCompletion();
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeViews() {
        // TextViews
        tvWelcomeHeading = findViewById(R.id.tvWelcomeHeading);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvJobCount = findViewById(R.id.tvJobCount);
        tvMockTestCount = findViewById(R.id.tvMockTestCount);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);

        // ImageViews
        ivProfileImage = findViewById(R.id.ivProfileImage);
        ivLogout = findViewById(R.id.ivLogout);
    }

    private void setupClickListeners() {
        // Profile Image Click
        ivProfileImage.setOnClickListener(v -> {
            openProfileActivity();
        });

        // Logout Click
        ivLogout.setOnClickListener(v -> {
            logoutUser();
        });

        // Card Click Listeners
        findViewById(R.id.cvStudent).setOnClickListener(v -> {
            if (isCompanyProfileComplete) {
                openStudentsActivity();
            } else {
                showProfileIncompleteDialog();
            }
        });

        findViewById(R.id.cvJob).setOnClickListener(v -> {
            if (isCompanyProfileComplete) {
                openJobsActivity();
            } else {
                showProfileIncompleteDialog();
            }
        });

        findViewById(R.id.cvMockTest).setOnClickListener(v -> {
            if (isCompanyProfileComplete) {
                openMockTestsActivity();
            } else {
                showProfileIncompleteDialog();
            }
        });

        findViewById(R.id.cvNotification).setOnClickListener(v -> {
            if (isCompanyProfileComplete) {
                openNotificationsActivity();
            } else {
                showProfileIncompleteDialog();
            }
        });
    }

    private void loadUserData() {
        if (currentEmployerId == null) return;

        // Remove previous listener if exists
        if (userDataListener != null) {
            mDatabase.child("users").child(currentEmployerId).removeEventListener(userDataListener);
        }

        userDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        // Update welcome message
                        String welcomeText = "Hello \n" + user.getFirstName() + " " + user.getLastName();
                        tvWelcomeHeading.setText(welcomeText);

                        // Load profile image if available
                        loadProfileImage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvWelcomeHeading.setText("Hello \nEmployer");
            }
        };

        mDatabase.child("users").child(currentEmployerId).addValueEventListener(userDataListener);

        // Start loading counts with live updates
        loadCountsWithLiveUpdates();
    }

    private void checkCompanyProfileCompletion() {
        if (currentEmployerId == null) return;

        // Remove previous listener if exists
        if (profileCompletionListener != null) {
            mDatabase.child("companyProfiles").child(currentEmployerId).removeEventListener(profileCompletionListener);
        }

        profileCompletionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    CompanyProfile companyProfile = dataSnapshot.getValue(CompanyProfile.class);
                    if (companyProfile != null && isProfileComplete(companyProfile)) {
                        isCompanyProfileComplete = true;
                    } else {
                        isCompanyProfileComplete = false;
                        // Only show dialog if it's the first time or user hasn't seen it
                        if (!isFinishing() && !isProfileDialogShown) {
                            showProfileIncompleteDialog();
                        }
                    }
                } else {
                    // No company profile found
                    isCompanyProfileComplete = false;
                    if (!isFinishing() && !isProfileDialogShown) {
                        showProfileIncompleteDialog();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                isCompanyProfileComplete = false;
            }
        };

        mDatabase.child("companyProfiles").child(currentEmployerId).addValueEventListener(profileCompletionListener);
    }

    private boolean isProfileComplete(CompanyProfile companyProfile) {
        // Check all required fields
        return companyProfile.getCompanyName() != null && !companyProfile.getCompanyName().isEmpty() &&
                companyProfile.getAddress() != null && !companyProfile.getAddress().isEmpty() &&
                companyProfile.getCity() != null && !companyProfile.getCity().isEmpty() &&
                companyProfile.getState() != null && !companyProfile.getState().isEmpty() &&
                companyProfile.getPincode() != null && !companyProfile.getPincode().isEmpty() &&
                companyProfile.getIndustry() != null && !companyProfile.getIndustry().isEmpty() &&
                companyProfile.getCompanySize() != null && !companyProfile.getCompanySize().isEmpty();
    }

    private boolean isProfileDialogShown = false;

    private void showProfileIncompleteDialog() {
        if (isProfileDialogShown || isFinishing()) return;

        isProfileDialogShown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Incomplete")
                .setMessage("Please complete your company profile to access all features. You need to fill in your company details to post jobs and use other services.")
                .setPositiveButton("Complete Profile", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isProfileDialogShown = false;
                        // Navigate to EditEmployerProfile activity
                        Intent intent = new Intent(EmployerHome.this, EditEmployerProfile.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isProfileDialogShown = false;
                        dialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        isProfileDialogShown = false;
                    }
                })
                .show();
    }

    private void loadProfileImage() {
        if (currentEmployerId == null) return;

        mDatabase.child("users").child(currentEmployerId).child("profileImageUrl")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String imageUrl = dataSnapshot.getValue(String.class);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                // Load image with Glide directly from the URL stored in database
                                Glide.with(EmployerHome.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_image_picker)
                                        .error(R.drawable.ic_image_picker)
                                        .into(ivProfileImage);
                            } else {
                                // Use default image if URL is empty
                                ivProfileImage.setImageResource(R.drawable.ic_image_picker);
                            }
                        } else {
                            // Use default image if no profile image URL found
                            ivProfileImage.setImageResource(R.drawable.ic_image_picker);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Use default image on error
                        ivProfileImage.setImageResource(R.drawable.ic_image_picker);
                    }
                });
    }

    private void loadCountsWithLiveUpdates() {
        if (currentEmployerId == null) return;

        // Load Applications Count for this employer's jobs (live update)
        loadApplicationsCountForEmployer();

        // Load Job Count (jobs posted by this employer) - live update
        loadJobCount();

        // Load Mock Test Count (all mock tests) - live update
        loadMockTestCount();

        // Load Notification Count (unread notifications for this employer) - live update
        loadNotificationCount();
    }

    private void loadApplicationsCountForEmployer() {
        if (currentEmployerId == null) return;

        // Remove previous listener if exists
        if (applicationsCountListener != null) {
            mDatabase.child("applications").removeEventListener(applicationsCountListener);
        }

        // Get all job IDs for this employer first
        mDatabase.child("jobs")
                .orderByChild("employerId")
                .equalTo(currentEmployerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot jobsSnapshot) {
                        Set<String> employerJobIds = new HashSet<>();
                        for (DataSnapshot jobSnapshot : jobsSnapshot.getChildren()) {
                            String jobId = jobSnapshot.getKey();
                            if (jobId != null) {
                                employerJobIds.add(jobId);
                            }
                        }

                        // Now count applications for these job IDs
                        countApplicationsForJobs(employerJobIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        tvStudentCount.setText("0");
                    }
                });
    }

    private void countApplicationsForJobs(Set<String> jobIds) {
        if (jobIds.isEmpty()) {
            tvStudentCount.setText("0");
            return;
        }

        // Remove previous listener if exists
        if (applicationsCountListener != null) {
            mDatabase.child("applications").removeEventListener(applicationsCountListener);
        }

        applicationsCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot applicationsSnapshot) {
                long applicationsCount = 0;
                Set<String> uniqueStudentIds = new HashSet<>(); // To count unique students

                for (DataSnapshot applicationSnapshot : applicationsSnapshot.getChildren()) {
                    String jobId = applicationSnapshot.child("jobId").getValue(String.class);
                    String studentId = applicationSnapshot.child("studentId").getValue(String.class);

                    if (jobId != null && jobIds.contains(jobId) && studentId != null) {
                        applicationsCount++;
                        uniqueStudentIds.add(studentId);
                    }
                }

                // Show either total applications or unique students
                tvStudentCount.setText(String.valueOf(applicationsCount)); // or uniqueStudentIds.size()
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvStudentCount.setText("0");
            }
        };

        mDatabase.child("applications").addValueEventListener(applicationsCountListener);
    }

    private void loadJobCount() {
        if (currentEmployerId == null) return;

        // Remove previous listener if exists
        if (jobCountListener != null) {
            mDatabase.child("jobs").removeEventListener(jobCountListener);
        }

        jobCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long jobCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Job job = snapshot.getValue(Job.class);
                    if (job != null && currentEmployerId.equals(job.getEmployerId()) && job.isActive()) {
                        jobCount++;
                    }
                }
                tvJobCount.setText(String.valueOf(jobCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvJobCount.setText("0");
            }
        };

        mDatabase.child("jobs").addValueEventListener(jobCountListener);
    }

    private void loadMockTestCount() {
        // Remove previous listener if exists
        if (mockTestCountListener != null) {
            mDatabase.child("mockTests").removeEventListener(mockTestCountListener);
        }

        mockTestCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long mockTestCount = dataSnapshot.getChildrenCount();
                tvMockTestCount.setText(String.valueOf(mockTestCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvMockTestCount.setText("0");
            }
        };

        mDatabase.child("mockTests").addValueEventListener(mockTestCountListener);
    }

    private void loadNotificationCount() {
        if (currentEmployerId == null) return;

        // Remove previous listener if exists
        if (notificationCountListener != null) {
            mDatabase.child("notifications").removeEventListener(notificationCountListener);
        }

        notificationCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long unreadCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String employerId = snapshot.child("employerId").getValue(String.class);
                    Boolean read = snapshot.child("read").getValue(Boolean.class);

                    if (currentEmployerId.equals(employerId) && (read == null || !read)) {
                        unreadCount++;
                    }
                }
                tvNotificationCount.setText(String.valueOf(unreadCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvNotificationCount.setText("0");
            }
        };

        mDatabase.child("notifications").addValueEventListener(notificationCountListener);
    }

    // Navigation methods
    private void openProfileActivity() {
        // Always allow profile access even if incomplete
        Intent intent = new Intent(EmployerHome.this, EmployerProfile.class);
        startActivity(intent);
    }

    private void logoutUser() {
        // Remove all listeners before logging out
        removeAllListeners();

        mAuth.signOut();
        Intent intent = new Intent(EmployerHome.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void openStudentsActivity() {
        Intent intent = new Intent(EmployerHome.this, EmployerApplicantsActivity.class);
        startActivity(intent);
    }

    private void openJobsActivity() {
        Intent intent = new Intent(EmployerHome.this, JobActivity.class);
        startActivity(intent);
    }

    private void openMockTestsActivity() {
        Toast.makeText(this, "Mock Tests Activity", Toast.LENGTH_SHORT).show();
    }

    private void openNotificationsActivity() {
        Intent intent = new Intent(EmployerHome.this, EmployerNotificationsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset profile dialog flag
        isProfileDialogShown = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in and is an employer
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Redirect to login if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // Verify user role
            verifyUserRole(currentUser.getUid());
        }
    }

    private void verifyUserRole(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && !"Employer".equals(user.getRole())) {
                        // User is not an employer, redirect to appropriate home
                        Toast.makeText(EmployerHome.this, "Access denied", Toast.LENGTH_SHORT).show();
                        removeAllListeners();
                        mAuth.signOut();
                        startActivity(new Intent(EmployerHome.this, LoginActivity.class));
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EmployerHome.this, "Error verifying user role", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // You can optionally remove listeners here if you want to stop updates when app is in background
        // removeAllListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Optional: Remove listeners to save resources when activity is not visible
        // removeAllListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always remove listeners when activity is destroyed
        removeAllListeners();
    }

    private void removeAllListeners() {
        // Remove all ValueEventListeners to prevent memory leaks
        if (userDataListener != null && currentEmployerId != null) {
            mDatabase.child("users").child(currentEmployerId).removeEventListener(userDataListener);
        }
        if (profileCompletionListener != null && currentEmployerId != null) {
            mDatabase.child("companyProfiles").child(currentEmployerId).removeEventListener(profileCompletionListener);
        }
        if (applicationsCountListener != null) {
            mDatabase.child("applications").removeEventListener(applicationsCountListener);
        }
        if (jobCountListener != null) {
            mDatabase.child("jobs").removeEventListener(jobCountListener);
        }
        if (mockTestCountListener != null) {
            mDatabase.child("mockTests").removeEventListener(mockTestCountListener);
        }
        if (notificationCountListener != null) {
            mDatabase.child("notifications").removeEventListener(notificationCountListener);
        }
    }
}