package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Adapters.RecentJobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserHome extends AppCompatActivity implements RecentJobsAdapter.OnJobClickListener {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    // UI Components
    private CircleImageView ivProfileImage;
    private TextView tvWelcomeHeading, tvSavedJobsCount, tvJobAppliedCount, tvRecentJobList;
    private LinearLayout llSavedJobsCard, llJobCard;
    private RecyclerView rvRecentJobs;
    private BottomNavigationView bottomNavigationView;

    // Adapter
    private RecentJobsAdapter recentJobsAdapter;
    private List<Job> recentJobsList = new ArrayList<>();

    // Profile completion check
    private boolean isProfileComplete = false;

    // ValueEventListeners for live updates
    private ValueEventListener userDataListener;
    private ValueEventListener profileCompletionListener;
    private ValueEventListener savedJobsCountListener;
    private ValueEventListener appliedJobsCountListener;
    private ValueEventListener recentJobsListener;
    private ValueEventListener profileImageListener;

    // Store user ID for cleanup
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeFirebase();
        initializeViews();
        setupBottomNavigation();
        setupClickListeners();
        setupRecyclerView();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserData();
            checkProfileCompletion();
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeViews() {
        // Image View
        ivProfileImage = findViewById(R.id.ivProfileImage);

        // Text Views
        tvWelcomeHeading = findViewById(R.id.tvWelcomeHeading);
        tvSavedJobsCount = findViewById(R.id.tvSavedJobsCount);
        tvJobAppliedCount = findViewById(R.id.tvJobAppliedCount);
        tvRecentJobList = findViewById(R.id.tvRecentJobList);

        // Card Views
        llSavedJobsCard = findViewById(R.id.llSavedJobsCard);
        llJobCard = findViewById(R.id.llJobCard);

        // RecyclerView
        rvRecentJobs = findViewById(R.id.rvRecentJobs);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_jobs) {
                Intent jobsIntent = new Intent(UserHome.this, JobActivity.class);
                startActivity(jobsIntent);
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                Intent notificationsIntent = new Intent(UserHome.this, NotificationsActivity.class);
                startActivity(notificationsIntent);
                return true;
            } else if (itemId == R.id.navigation_home) {
                // Already on Home, refresh data
                refreshHomeData();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent profileIntent = new Intent(UserHome.this, UserProfile.class);
                startActivity(profileIntent);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    private void refreshHomeData() {
        // Force refresh all data
        checkProfileCompletion();
        if (isProfileComplete) {
            loadCountsFromFirebase();
            loadRecentJobs();
        }
    }

    private void setupClickListeners() {
        ivProfileImage.setOnClickListener(v -> {
            openProfileActivity();
        });

        llSavedJobsCard.setOnClickListener(v -> {
            if (isProfileComplete) {
                openSavedJobsActivity();
            } else {
                showProfileIncompleteDialog();
            }
        });

        llJobCard.setOnClickListener(v -> {
            if (isProfileComplete) {
                openAppliedJobsActivity();
            } else {
                showProfileIncompleteDialog();
            }
        });
    }

    private void setupRecyclerView() {
        recentJobsAdapter = new RecentJobsAdapter(recentJobsList, this);
        rvRecentJobs.setLayoutManager(new LinearLayoutManager(this));
        rvRecentJobs.setAdapter(recentJobsAdapter);
    }

    private void checkProfileCompletion() {
        if (currentUserId == null) return;

        // Remove previous listener if exists
        if (profileCompletionListener != null) {
            mDatabase.child("users").child(currentUserId).removeEventListener(profileCompletionListener);
        }

        profileCompletionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Check if both personalInfo and academicInfo exist and are complete
                    DataSnapshot personalInfo = dataSnapshot.child("personalInfo");
                    DataSnapshot academicInfo = dataSnapshot.child("academicInfo");

                    boolean hasPersonalInfo = personalInfo.exists() &&
                            personalInfo.child("profileCompleted").exists() &&
                            personalInfo.child("profileCompleted").getValue(Boolean.class);

                    boolean hasAcademicInfo = academicInfo.exists() &&
                            academicInfo.child("profileCompleted").exists() &&
                            academicInfo.child("profileCompleted").getValue(Boolean.class);

                    boolean newProfileComplete = hasPersonalInfo && hasAcademicInfo;

                    // Only update if status changed
                    if (newProfileComplete != isProfileComplete) {
                        isProfileComplete = newProfileComplete;

                        if (isProfileComplete) {
                            enableJobFeatures();
                            loadCountsFromFirebase();
                            loadRecentJobs();
                        } else {
                            disableJobFeatures();
                            showProfileIncompleteDialog();
                        }
                    }
                } else {
                    // User data doesn't exist at all
                    isProfileComplete = false;
                    disableJobFeatures();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserHome.this, "Failed to check profile status", Toast.LENGTH_SHORT).show();
                isProfileComplete = false;
                disableJobFeatures();
            }
        };

        mDatabase.child("users").child(currentUserId).addValueEventListener(profileCompletionListener);
    }

    private void loadUserData() {
        if (currentUserId == null) return;

        // Remove previous listener if exists
        if (userDataListener != null) {
            mDatabase.child("users").child(currentUserId).removeEventListener(userDataListener);
        }

        userDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        String welcomeText = "Hello \n" + user.getFirstName();
                        tvWelcomeHeading.setText(welcomeText);
                    }
                } else {
                    tvWelcomeHeading.setText("Hello \nUser");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvWelcomeHeading.setText("Hello \nUser");
            }
        };

        mDatabase.child("users").child(currentUserId).addValueEventListener(userDataListener);

        // Load profile image with live updates
        loadProfileImage();
    }

    private void loadProfileImage() {
        if (currentUserId == null) return;

        // Remove previous listener if exists
        if (profileImageListener != null) {
            mDatabase.child("users").child(currentUserId).child("profileImageUrl").removeEventListener(profileImageListener);
        }

        profileImageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String imageUrl = dataSnapshot.getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(UserHome.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_image_picker)
                                .error(R.drawable.ic_image_picker)
                                .into(ivProfileImage);
                    } else {
                        ivProfileImage.setImageResource(R.drawable.ic_image_picker);
                    }
                } else {
                    ivProfileImage.setImageResource(R.drawable.ic_image_picker);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                ivProfileImage.setImageResource(R.drawable.ic_image_picker);
            }
        };

        mDatabase.child("users").child(currentUserId).child("profileImageUrl")
                .addValueEventListener(profileImageListener);
    }

    private void loadCountsFromFirebase() {
        if (!isProfileComplete || currentUserId == null) return;

        // Load Saved Jobs Count - Live updates
        loadSavedJobsCount();

        // Load Applied Jobs Count - Live updates
        loadAppliedJobsCount();
    }

    private void loadSavedJobsCount() {
        if (currentUserId == null) return;

        // Remove previous listener if exists
        if (savedJobsCountListener != null) {
            mDatabase.child("savedJobs").removeEventListener(savedJobsCountListener);
        }

        savedJobsCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long savedJobsCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String studentId = snapshot.child("studentId").getValue(String.class);
                    if (currentUserId.equals(studentId)) {
                        savedJobsCount++;
                    }
                }
                tvSavedJobsCount.setText(String.valueOf(savedJobsCount));
                Log.d("SavedJobsCount", "Live update - Saved jobs count: " + savedJobsCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvSavedJobsCount.setText("0");
                Log.e("SavedJobsCount", "Error loading saved jobs: " + databaseError.getMessage());
            }
        };

        mDatabase.child("savedJobs").addValueEventListener(savedJobsCountListener);
    }

    private void loadAppliedJobsCount() {
        if (currentUserId == null) return;

        // Remove previous listener if exists
        if (appliedJobsCountListener != null) {
            mDatabase.child("applications").removeEventListener(appliedJobsCountListener);
        }

        appliedJobsCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long appliedJobsCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String studentId = snapshot.child("studentId").getValue(String.class);
                    if (currentUserId.equals(studentId)) {
                        appliedJobsCount++;
                    }
                }
                tvJobAppliedCount.setText(String.valueOf(appliedJobsCount));
                Log.d("AppliedJobsCount", "Live update - Applied jobs count: " + appliedJobsCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvJobAppliedCount.setText("0");
            }
        };

        mDatabase.child("applications").addValueEventListener(appliedJobsCountListener);
    }

    private void loadRecentJobs() {
        if (!isProfileComplete) {
            recentJobsList.clear();
            recentJobsAdapter.notifyDataSetChanged();
            tvRecentJobList.setText("Complete your profile to view jobs");
            return;
        }

        // Remove previous listener if exists
        if (recentJobsListener != null) {
            mDatabase.child("jobs").removeEventListener(recentJobsListener);
        }

        recentJobsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Job> tempList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Job job = snapshot.getValue(Job.class);
                    if (job != null && job.isActive()) {
                        tempList.add(job);
                    }
                }

                // Sort by date if you have a timestamp field, or just take latest 10
                // For simplicity, we'll take all active jobs
                recentJobsList.clear();
                if (tempList.size() > 10) {
                    recentJobsList.addAll(tempList.subList(tempList.size() - 10, tempList.size()));
                } else {
                    recentJobsList.addAll(tempList);
                }

                // Reverse to show newest first
                List<Job> reversedList = new ArrayList<>();
                for (int i = recentJobsList.size() - 1; i >= 0; i--) {
                    reversedList.add(recentJobsList.get(i));
                }
                recentJobsList.clear();
                recentJobsList.addAll(reversedList);

                recentJobsAdapter.notifyDataSetChanged();
                updateRecentJobsHeading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserHome.this, "Failed to load recent jobs", Toast.LENGTH_SHORT).show();
            }
        };

        mDatabase.child("jobs").addValueEventListener(recentJobsListener);
    }

    private void updateRecentJobsHeading() {
        String recentJobsText = getResources().getString(R.string.home_fragment_job_list) +
                " (" + recentJobsList.size() + ")";
        tvRecentJobList.setText(recentJobsText);
    }

    private boolean isProfileDialogShown = false;

    private void showProfileIncompleteDialog() {
        if (isProfileDialogShown || isFinishing()) return;

        isProfileDialogShown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Incomplete")
                .setMessage("Please complete your profile to access all features. You need to fill in your personal and academic details to save jobs, apply for jobs, and view job details.")
                .setPositiveButton("Complete Profile", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isProfileDialogShown = false;
                        Intent intent = new Intent(UserHome.this, UserProfile.class);
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

    private void disableJobFeatures() {
        runOnUiThread(() -> {
            llSavedJobsCard.setAlpha(0.5f);
            llJobCard.setAlpha(0.5f);
            rvRecentJobs.setAlpha(0.5f);

            tvSavedJobsCount.setText("-");
            tvJobAppliedCount.setText("-");
            tvRecentJobList.setText("Complete profile to view jobs");

            recentJobsList.clear();
            recentJobsAdapter.notifyDataSetChanged();
        });
    }

    private void enableJobFeatures() {
        runOnUiThread(() -> {
            llSavedJobsCard.setAlpha(1.0f);
            llJobCard.setAlpha(1.0f);
            rvRecentJobs.setAlpha(1.0f);
        });
    }

    // Navigation methods
    private void openProfileActivity() {
        Intent intent = new Intent(UserHome.this, UserProfile.class);
        startActivity(intent);
    }

    private void openSavedJobsActivity() {
        Intent intent = new Intent(UserHome.this, SavedJobs.class);
        startActivity(intent);
    }

    private void openAppliedJobsActivity() {
        Intent intent = new Intent(UserHome.this, AppliedJobs.class);
        startActivity(intent);
    }

    @Override
    public void onJobClick(Job job) {
        if (isProfileComplete) {
            Intent intent = new Intent(UserHome.this, UserJobView.class);
            intent.putExtra("jobId", job.getJobId());
            startActivity(intent);
        } else {
            showProfileIncompleteDialog();
        }
    }

    @Override
    public void onJobApplyClick(Job job) {
        if (isProfileComplete) {
            Intent intent = new Intent(UserHome.this, AppliedJobs.class);
            intent.putExtra("jobId", job.getJobId());
            intent.putExtra("autoOpenApply", true);
            startActivity(intent);
        } else {
            showProfileIncompleteDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset dialog flag
        isProfileDialogShown = false;

        // Reset bottom navigation selection to home
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Optional: Remove listeners to save resources
        // removeAllListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Optional: Remove listeners when activity is not visible
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
        if (userDataListener != null && currentUserId != null) {
            mDatabase.child("users").child(currentUserId).removeEventListener(userDataListener);
        }
        if (profileCompletionListener != null && currentUserId != null) {
            mDatabase.child("users").child(currentUserId).removeEventListener(profileCompletionListener);
        }
        if (savedJobsCountListener != null) {
            mDatabase.child("savedJobs").removeEventListener(savedJobsCountListener);
        }
        if (appliedJobsCountListener != null) {
            mDatabase.child("applications").removeEventListener(appliedJobsCountListener);
        }
        if (recentJobsListener != null) {
            mDatabase.child("jobs").removeEventListener(recentJobsListener);
        }
        if (profileImageListener != null && currentUserId != null) {
            mDatabase.child("users").child(currentUserId).child("profileImageUrl").removeEventListener(profileImageListener);
        }
    }
}