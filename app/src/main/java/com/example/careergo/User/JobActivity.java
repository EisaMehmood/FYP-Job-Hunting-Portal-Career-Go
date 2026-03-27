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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Adapters.RecentJobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class JobActivity extends AppCompatActivity implements RecentJobsAdapter.OnJobClickListener {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    // UI Components
    private TextView tvJobsHeading, tvJobsCount;
    private RecyclerView rvJobs;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout llCategoryFilter;
    private MaterialButton btnAllCategories;

    // Adapter
    private RecentJobsAdapter jobsAdapter;
    private List<Job> jobsList = new ArrayList<>();
    private List<Job> allJobsList = new ArrayList<>();

    // Profile completion check
    private boolean isProfileComplete = false;

    // Categories
    private String[] jobCategories;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job2);

        initializeFirebase();
        initializeViews();
        setupBottomNavigation();
        setupClickListeners();
        setupRecyclerView();
        loadCategories();
        checkProfileCompletion();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeViews() {
        // Text Views
        tvJobsHeading = findViewById(R.id.tvJobsHeading);
        tvJobsCount = findViewById(R.id.tvJobsCount);

        // RecyclerView
        rvJobs = findViewById(R.id.rvJobs);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Category Filter
        llCategoryFilter = findViewById(R.id.llCategoryFilter);
        btnAllCategories = findViewById(R.id.btnAllCategories);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_jobs) {
                // Already on Jobs, do nothing
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                // Open Notifications Activity
                Intent notificationsIntent = new Intent(JobActivity.this, NotificationsActivity.class);
                startActivity(notificationsIntent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_home) {
                // Open Home Activity
                Intent homeIntent = new Intent(JobActivity.this, UserHome.class);
                startActivity(homeIntent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Open Profile Activity
                Intent profileIntent = new Intent(JobActivity.this, UserProfile.class);
                startActivity(profileIntent);
                return true;
            }
            return false;
        });

        // Set the jobs item as selected by default
        bottomNavigationView.setSelectedItemId(R.id.navigation_jobs);
    }

    private void setupClickListeners() {
        // All Categories Button Click
        btnAllCategories.setOnClickListener(v -> {
            showCategoryFilterDialog();
        });
    }

    private void setupRecyclerView() {
        jobsAdapter = new RecentJobsAdapter(jobsList, this);
        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(jobsAdapter);
    }

    private void loadCategories() {
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> categoriesList = new ArrayList<>();
                categoriesList.add("All"); // Add "All" as default

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    if (category != null) {
                        categoriesList.add(category);
                    }
                }

                // If no categories found in Firebase, use defaults
                if (categoriesList.size() == 1) { // Only "All" is present
                    loadDefaultCategories();
                } else {
                    jobCategories = categoriesList.toArray(new String[0]);
                    setupCategoryFilter();
                    Log.d("CategoryFilter", "Loaded " + jobCategories.length + " categories from Firebase");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // If Firebase fails, use string array
                Log.e("CategoryFilter", "Failed to load categories from Firebase: " + databaseError.getMessage());
                loadDefaultCategories();
            }
        });
    }

    private void loadDefaultCategories() {
        // Get categories from string array resource
        String[] defaultCategories = getResources().getStringArray(R.array.job_categories);

        // Make sure "All" is the first option
        List<String> categoriesList = new ArrayList<>();
        categoriesList.add("All");
        categoriesList.addAll(Arrays.asList(defaultCategories));
        jobCategories = categoriesList.toArray(new String[0]);

        setupCategoryFilter();
        Log.d("CategoryFilter", "Loaded " + jobCategories.length + " default categories");
    }

    private void setupCategoryFilter() {
        // Set initial text
        btnAllCategories.setText(selectedCategory);
        btnAllCategories.setEnabled(true);
    }

    private void showCategoryFilterDialog() {
        if (jobCategories == null || jobCategories.length == 0) {
            Toast.makeText(this, "Categories not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Category")
                .setItems(jobCategories, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedCategory = jobCategories[which];
                        btnAllCategories.setText(selectedCategory);
                        filterJobsByCategory();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterJobsByCategory() {
        if (selectedCategory.equals("All")) {
            // Show all jobs
            jobsList.clear();
            jobsList.addAll(allJobsList);
            Log.d("CategoryFilter", "Showing all jobs: " + jobsList.size());
        } else {
            // Filter by selected category
            jobsList.clear();
            for (Job job : allJobsList) {
                if (job.getDesignation() != null && job.getDesignation().equals(selectedCategory)) {
                    jobsList.add(job);
                }
            }
            Log.d("CategoryFilter", "Filtered by '" + selectedCategory + "': " + jobsList.size() + " jobs found");
        }

        jobsAdapter.notifyDataSetChanged();
        updateJobsCount();

        // Show message if no jobs found for selected category
        if (jobsList.isEmpty() && !selectedCategory.equals("All")) {
            Toast.makeText(this, "No jobs found in " + selectedCategory + " category", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkProfileCompletion() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        DataSnapshot personalInfo = dataSnapshot.child("personalInfo");
                        DataSnapshot academicInfo = dataSnapshot.child("academicInfo");

                        boolean hasPersonalInfo = personalInfo.exists() &&
                                personalInfo.child("profileCompleted").exists() &&
                                personalInfo.child("profileCompleted").getValue(Boolean.class);

                        boolean hasAcademicInfo = academicInfo.exists() &&
                                academicInfo.child("profileCompleted").exists() &&
                                academicInfo.child("profileCompleted").getValue(Boolean.class);

                        isProfileComplete = hasPersonalInfo && hasAcademicInfo;

                        if (isProfileComplete) {
                            loadAllJobs();
                            enableJobFeatures();
                        } else {
                            showProfileIncompleteDialog();
                            disableJobFeatures();
                        }
                    } else {
                        showProfileIncompleteDialog();
                        disableJobFeatures();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(JobActivity.this, "Failed to check profile status", Toast.LENGTH_SHORT).show();
                    isProfileComplete = false;
                    disableJobFeatures();
                }
            });
        }
    }

    private void loadAllJobs() {
        if (!isProfileComplete) {
            jobsList.clear();
            jobsAdapter.notifyDataSetChanged();
            tvJobsHeading.setText("Complete your profile to view jobs");
            return;
        }

        Query jobsQuery = mDatabase.child("jobs")
                .orderByChild("active")
                .equalTo(true);

        jobsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allJobsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Job job = snapshot.getValue(Job.class);
                    if (job != null && job.isActive()) {
                        job.setJobId(snapshot.getKey()); // Make sure job ID is set
                        allJobsList.add(job);
                    }
                }

                Log.d("JobActivity", "Loaded " + allJobsList.size() + " jobs from Firebase");

                // Apply current filter
                filterJobsByCategory();
                updateJobsCount();

                // Show message if no jobs at all
                if (allJobsList.isEmpty()) {
                    tvJobsCount.setText("No jobs available");
                    Toast.makeText(JobActivity.this, "No jobs available at the moment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(JobActivity.this, "Failed to load jobs", Toast.LENGTH_SHORT).show();
                Log.e("JobActivity", "Error loading jobs: " + databaseError.getMessage());
            }
        });
    }

    private void updateJobsCount() {
        String jobsText = "Available Jobs (" + jobsList.size() + ")";
        tvJobsCount.setText(jobsText);
    }

    private void showProfileIncompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Incomplete")
                .setMessage("Please complete your profile to view and apply for jobs. You need to fill in your personal and academic details.")
                .setPositiveButton("Complete Profile", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(JobActivity.this, UserProfile.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(JobActivity.this, "Please complete your profile soon to access jobs", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void disableJobFeatures() {
        rvJobs.setAlpha(0.5f);
        llCategoryFilter.setAlpha(0.5f);
        btnAllCategories.setEnabled(false);
        tvJobsCount.setText("Complete profile to view jobs");

        jobsList.clear();
        jobsAdapter.notifyDataSetChanged();
    }

    private void enableJobFeatures() {
        rvJobs.setAlpha(1.0f);
        llCategoryFilter.setAlpha(1.0f);
        btnAllCategories.setEnabled(true);
    }

    @Override
    public void onJobClick(Job job) {
        if (isProfileComplete) {
            Intent intent = new Intent(JobActivity.this, UserJobView.class);
            intent.putExtra("jobId", job.getJobId());
            startActivity(intent);
        } else {
            showProfileIncompleteDialog();
        }
    }

    @Override
    public void onJobApplyClick(Job job) {
        if (isProfileComplete) {
            Intent intent = new Intent(JobActivity.this, AppliedJobs.class);
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
        checkProfileCompletion();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_jobs);
        }
    }
}