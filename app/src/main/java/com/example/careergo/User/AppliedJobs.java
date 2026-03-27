package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.careergo.Adapters.AppliedJobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.Model.JobApplication;
import com.example.careergo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppliedJobs extends AppCompatActivity implements AppliedJobsAdapter.OnAppliedJobClickListener {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private ImageView ivBack, ivSearchIcon, ivClearSearch;
    private EditText etSearch;
    private TextView tvTitle, tvNoApplications, tvApplicationsCount;
    private RecyclerView rvAppliedJobs;
    private LinearLayout llSearchContainer, llLoading, llNoApplications;

    // Adapter
    private AppliedJobsAdapter appliedJobsAdapter;
    private List<JobApplication> appliedJobsList = new ArrayList<>();
    private List<JobApplication> filteredJobsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applied_jobs);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupSearchFunctionality();
        loadAppliedJobs();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // ImageViews
        ivBack = findViewById(R.id.ivBack);
        ivSearchIcon = findViewById(R.id.ivSearchIcon);
        ivClearSearch = findViewById(R.id.ivClearSearch);

        // EditTexts
        etSearch = findViewById(R.id.etSearch);

        // TextViews
        tvTitle = findViewById(R.id.tvTitle);
        tvNoApplications = findViewById(R.id.tvNoApplications);
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount);

        // RecyclerView
        rvAppliedJobs = findViewById(R.id.rvAppliedJobs);

        // Layouts
        llSearchContainer = findViewById(R.id.llSearchContainer);
        llLoading = findViewById(R.id.llLoading);
        llNoApplications = findViewById(R.id.llNoApplications);
    }

    private void setupClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Clear search
        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            filterApplications("");
        });
    }

    private void setupRecyclerView() {
        appliedJobsAdapter = new AppliedJobsAdapter(filteredJobsList, this);
        rvAppliedJobs.setLayoutManager(new LinearLayoutManager(this));
        rvAppliedJobs.setAdapter(appliedJobsAdapter);
    }

    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApplications(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterApplications(String searchText) {
        filteredJobsList.clear();

        if (searchText.isEmpty()) {
            filteredJobsList.addAll(appliedJobsList);
        } else {
            String query = searchText.toLowerCase().trim();
            for (JobApplication application : appliedJobsList) {
                if (application.getJobTitle() != null && application.getJobTitle().toLowerCase().contains(query) ||
                        application.getCompanyName() != null && application.getCompanyName().toLowerCase().contains(query) ||
                        application.getStatus() != null && application.getStatus().toLowerCase().contains(query)) {
                    filteredJobsList.add(application);
                }
            }
        }

        updateUI();
        appliedJobsAdapter.notifyDataSetChanged();
    }

    private void loadAppliedJobs() {
        showLoading(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showNoApplications();
            return;
        }

        String userId = currentUser.getUid();

        // Query applications for current user
        Query applicationsQuery = mDatabase.child("applications")
                .orderByChild("studentId")
                .equalTo(userId);

        applicationsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                appliedJobsList.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot applicationSnapshot : dataSnapshot.getChildren()) {
                        JobApplication application = applicationSnapshot.getValue(JobApplication.class);
                        if (application != null) {
                            application.setApplicationId(applicationSnapshot.getKey());
                            appliedJobsList.add(application);
                        }
                    }

                    // Sort by application date (newest first)
                    Collections.sort(appliedJobsList, new Comparator<JobApplication>() {
                        @Override
                        public int compare(JobApplication app1, JobApplication app2) {
                            return Long.compare(app2.getAppliedDate(), app1.getAppliedDate());
                        }
                    });

                    // Load job details for each application
                    loadJobDetailsForApplications();
                } else {
                    showNoApplications();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(AppliedJobs.this, "Failed to load applications", Toast.LENGTH_SHORT).show();
                showNoApplications();
            }
        });
    }

    private void loadJobDetailsForApplications() {
        if (appliedJobsList.isEmpty()) {
            showNoApplications();
            return;
        }

        final int[] loadedCount = {0};
        final int totalApplications = appliedJobsList.size();

        for (int i = 0; i < appliedJobsList.size(); i++) {
            JobApplication application = appliedJobsList.get(i);
            String jobId = application.getJobId();

            if (jobId != null) {
                mDatabase.child("jobs").child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Job job = dataSnapshot.getValue(Job.class);
                            if (job != null) {
                                application.setJobTitle(job.getJobTitle());
                                application.setCompanyName(job.getCompanyName());
                                application.setCompanyImageUrl(job.getCompanyImageUrl());
                                application.setLocation(job.getCity());
                                application.setSalary(job.getSalary());
                            }
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalApplications) {
                            showLoading(false);
                            filteredJobsList.clear();
                            filteredJobsList.addAll(appliedJobsList);
                            updateUI();
                            appliedJobsAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        loadedCount[0]++;
                        if (loadedCount[0] == totalApplications) {
                            showLoading(false);
                            filteredJobsList.clear();
                            filteredJobsList.addAll(appliedJobsList);
                            updateUI();
                            appliedJobsAdapter.notifyDataSetChanged();
                        }
                    }
                });
            } else {
                loadedCount[0]++;
                if (loadedCount[0] == totalApplications) {
                    showLoading(false);
                    filteredJobsList.clear();
                    filteredJobsList.addAll(appliedJobsList);
                    updateUI();
                    appliedJobsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void updateUI() {
        if (filteredJobsList.isEmpty()) {
            llNoApplications.setVisibility(View.VISIBLE);
            rvAppliedJobs.setVisibility(View.GONE);
            tvApplicationsCount.setText("No applications found");
        } else {
            llNoApplications.setVisibility(View.GONE);
            rvAppliedJobs.setVisibility(View.VISIBLE);
            tvApplicationsCount.setText(filteredJobsList.size() + " application" + (filteredJobsList.size() != 1 ? "s" : ""));
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            llLoading.setVisibility(View.VISIBLE);
            rvAppliedJobs.setVisibility(View.GONE);
            llNoApplications.setVisibility(View.GONE);
        } else {
            llLoading.setVisibility(View.GONE);
        }
    }

    private void showNoApplications() {
        showLoading(false);
        llNoApplications.setVisibility(View.VISIBLE);
        rvAppliedJobs.setVisibility(View.GONE);
        tvApplicationsCount.setText("No applications");
    }

    @Override
    public void onAppliedJobClick(JobApplication application) {
        // Open job details view
        if (application.getJobId() != null) {
            Intent intent = new Intent(this, UserJobView.class);
            intent.putExtra("jobId", application.getJobId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Job details not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusClick(JobApplication application) {
        // Show application status details
        showStatusDialog(application);
    }

    private void showStatusDialog(JobApplication application) {
        // Create a custom dialog or bottom sheet to show detailed status
        String statusMessage = getStatusMessage(application.getStatus());
        Toast.makeText(this, "Application Status: " + statusMessage, Toast.LENGTH_LONG).show();
    }

    private String getStatusMessage(String status) {
        if (status == null) return "Pending Review";

        switch (status.toLowerCase()) {
            case "pending":
                return "Your application is under review";
            case "shortlisted":
                return "You've been shortlisted! The employer may contact you soon";
            case "rejected":
                return "Your application was not selected for this position";
            case "accepted":
                return "Congratulations! Your application has been accepted";
            default:
                return "Pending Review";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadAppliedJobs();
    }
}