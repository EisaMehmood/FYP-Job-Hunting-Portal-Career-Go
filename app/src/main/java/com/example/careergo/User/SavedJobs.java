package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.careergo.Adapters.SavedJobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.Model.SavedJob;
import com.example.careergo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedJobs extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String TAG = "SavedJobsActivity";

    // UI Components
    private RecyclerView rvSavedJobs;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private ImageView ivBack;
    private TextView tvTitle;

    // Adapter and List
    private SavedJobsAdapter adapter;
    private List<SavedJob> savedJobList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_jobs);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadSavedJobs();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG, "Firebase initialized");
    }

    private void initializeViews() {
        rvSavedJobs = findViewById(R.id.rvSavedJobs);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);

        savedJobList = new ArrayList<>();
        Log.d(TAG, "Views initialized");
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new SavedJobsAdapter(this, savedJobList);
        rvSavedJobs.setLayoutManager(new LinearLayoutManager(this));
        rvSavedJobs.setAdapter(adapter);
        Log.d(TAG, "RecyclerView setup completed");
    }

    private void loadSavedJobs() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not logged in");
            showEmptyState();
            Toast.makeText(this, "Please login to view saved jobs", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading saved jobs for user: " + userId);

        showLoading();

        mDatabase.child("savedJobs")
                .orderByChild("studentId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());
                        Log.d(TAG, "DataSnapshot children count: " + dataSnapshot.getChildrenCount());

                        savedJobList.clear();

                        if (dataSnapshot.exists()) {
                            List<SavedJob> tempSavedJobs = new ArrayList<>();

                            // First, collect all saved job references
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                SavedJob savedJob = snapshot.getValue(SavedJob.class);
                                if (savedJob != null) {
                                    savedJob.setSavedJobId(snapshot.getKey()); // Set the Firebase key
                                    tempSavedJobs.add(savedJob);
                                    Log.d(TAG, "Found saved job reference: " + savedJob.getJobId());
                                }
                            }

                            if (!tempSavedJobs.isEmpty()) {
                                // Now load job details for each saved job
                                loadJobDetails(tempSavedJobs);
                            } else {
                                showEmptyState();
                            }
                        } else {
                            Log.d(TAG, "No saved jobs found in database");
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Database error: " + databaseError.getMessage());
                        showEmptyState();
                        Toast.makeText(SavedJobs.this, "Failed to load saved jobs", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadJobDetails(List<SavedJob> tempSavedJobs) {
        final int totalJobs = tempSavedJobs.size();
        final List<SavedJob> loadedJobs = new ArrayList<>();

        for (SavedJob savedJob : tempSavedJobs) {
            String jobId = savedJob.getJobId();

            if (jobId != null && !jobId.isEmpty()) {
                mDatabase.child("jobs").child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Job jobDetails = dataSnapshot.getValue(Job.class);
                            if (jobDetails != null) {
                                savedJob.setJobDetails(jobDetails);
                                loadedJobs.add(savedJob);
                                Log.d(TAG, "Loaded job details for: " + jobDetails.getJobTitle());
                            }
                        } else {
                            Log.w(TAG, "Job not found: " + jobId);
                        }

                        // Check if all jobs have been loaded
                        if (loadedJobs.size() == totalJobs) {
                            processLoadedJobs(loadedJobs);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to load job details: " + databaseError.getMessage());
                        loadedJobs.add(savedJob); // Add even without details

                        // Check if all jobs have been processed
                        if (loadedJobs.size() == totalJobs) {
                            processLoadedJobs(loadedJobs);
                        }
                    }
                });
            } else {
                Log.w(TAG, "Invalid job ID in saved job");
                loadedJobs.add(savedJob); // Add even with invalid job ID

                // Check if all jobs have been processed
                if (loadedJobs.size() == totalJobs) {
                    processLoadedJobs(loadedJobs);
                }
            }
        }
    }

    private void processLoadedJobs(List<SavedJob> loadedJobs) {
        // Filter out jobs that couldn't be loaded (optional)
        List<SavedJob> validJobs = new ArrayList<>();
        for (SavedJob savedJob : loadedJobs) {
            if (savedJob.getJobDetails() != null) {
                validJobs.add(savedJob);
            }
        }

        if (!validJobs.isEmpty()) {
            // Sort by saved date (newest first)
            Collections.sort(validJobs, (o1, o2) -> Long.compare(o2.getSavedDate(), o1.getSavedDate()));

            savedJobList.clear();
            savedJobList.addAll(validJobs);

            // Update adapter
            adapter.updateList(savedJobList);
            showJobsList();
            Log.d(TAG, "Showing jobs list with " + savedJobList.size() + " items");
        } else {
            Log.d(TAG, "No valid jobs found after loading details");
            showEmptyState();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvSavedJobs.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void showJobsList() {
        progressBar.setVisibility(View.GONE);
        rvSavedJobs.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        rvSavedJobs.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listeners if needed
    }
}