package com.example.careergo.Admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Adapters.JobApplicationsAdapter;
import com.example.careergo.Model.JobApplication;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminJobApplicationsActivity extends AppCompatActivity implements JobApplicationsAdapter.OnApplicationActionListener {

    private static final String TAG = "AdminJobApplications";

    private RecyclerView rvApplications;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipPending, chipReviewed, chipAccepted, chipRejected;
    private ProgressBar progressBar;
    private TextView tvTitle, tvApplicationCount;
    private ImageButton ibBack;
    private LinearLayout llEmptyState;

    private JobApplicationsAdapter applicationsAdapter;
    private List<JobApplication> applicationList = new ArrayList<>();
    private List<JobApplication> filteredApplicationList = new ArrayList<>();
    private Map<String, User> studentDataMap = new HashMap<>();

    private DatabaseReference mDatabase;
    private ValueEventListener applicationsListener;
    private String jobId;
    private String jobTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_job_applications);

        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        // Get job data from intent
        jobId = getIntent().getStringExtra("jobId");
        jobTitle = getIntent().getStringExtra("jobTitle");

        // Debug intent data
        Log.d(TAG, "Received jobId: " + jobId);
        Log.d(TAG, "Received jobTitle: " + jobTitle);

        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job ID not provided", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Job ID is null or empty");
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Set title with job title
        if (jobTitle != null && !jobTitle.isEmpty()) {
            tvTitle.setText(jobTitle + " - Applications");
        } else {
            tvTitle.setText("Job Applications");
        }

        loadJobApplications();
        setupFilterListeners();
    }

    private void initializeViews() {
        rvApplications = findViewById(R.id.rvApplications);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipReviewed = findViewById(R.id.chipReviewed);
        chipAccepted = findViewById(R.id.chipAccepted);
        chipRejected = findViewById(R.id.chipRejected);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        tvApplicationCount = findViewById(R.id.tvApplicationCount);
        ibBack = findViewById(R.id.ibBack);
        llEmptyState = findViewById(R.id.llEmptyState);
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        applicationsAdapter = new JobApplicationsAdapter(filteredApplicationList, studentDataMap, this);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(applicationsAdapter);
    }

    private void setupFilterListeners() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Log.d(TAG, "Filter changed, checked IDs: " + checkedIds);
            filterApplications();
        });

        // Set default: select "All" chip
        chipAll.setChecked(true);
    }

    private void loadJobApplications() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvApplications.setVisibility(View.GONE);

        Log.d(TAG, "Loading applications for jobId: " + jobId);

        // Remove existing listener if any
        if (applicationsListener != null) {
            mDatabase.child("applications").removeEventListener(applicationsListener);
        }

        applicationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applicationList.clear();
                studentDataMap.clear();
                filteredApplicationList.clear();

                Log.d(TAG, "Firebase snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Firebase children count: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    List<String> studentIds = new ArrayList<>();
                    int applicationsFound = 0;

                    // Iterate through all applications
                    for (DataSnapshot applicationSnapshot : snapshot.getChildren()) {
                        JobApplication application = applicationSnapshot.getValue(JobApplication.class);
                        if (application != null) {
                            // Check if this application belongs to our job
                            if (jobId.equals(application.getJobId())) {
                                application.setApplicationId(applicationSnapshot.getKey());
                                applicationList.add(application);
                                applicationsFound++;

                                Log.d(TAG, "Found application: " + application.getApplicationId() +
                                        " for student: " + application.getStudentId() +
                                        " status: " + application.getStatus());

                                // Collect student IDs to fetch their data
                                if (application.getStudentId() != null && !application.getStudentId().isEmpty() &&
                                        !studentIds.contains(application.getStudentId())) {
                                    studentIds.add(application.getStudentId());
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Total applications found for job " + jobId + ": " + applicationsFound);
                    Log.d(TAG, "Unique student IDs to fetch: " + studentIds.size());

                    if (applicationsFound > 0) {
                        if (!studentIds.isEmpty()) {
                            fetchStudentData(studentIds);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            updateUI();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showNoApplications("No applications found for this job");
                        Log.d(TAG, "No applications found for jobId: " + jobId);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    showNoApplications("No applications found in database");
                    Log.d(TAG, "No applications exist in Firebase at all");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                showNoApplications("Failed to load applications");
                Log.e(TAG, "Database error: " + error.getMessage() + " - " + error.getDetails());
                Toast.makeText(AdminJobApplicationsActivity.this,
                        "Failed to load applications: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        // Listen to all applications and filter by jobId in code
        mDatabase.child("applications").addValueEventListener(applicationsListener);
    }

    private void fetchStudentData(List<String> studentIds) {
        Log.d(TAG, "Fetching data for " + studentIds.size() + " students");

        final int[] studentsLoaded = {0};
        final int totalStudents = studentIds.size();

        if (totalStudents == 0) {
            progressBar.setVisibility(View.GONE);
            updateUI();
            return;
        }

        for (String studentId : studentIds) {
            mDatabase.child("users").child(studentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                User student = snapshot.getValue(User.class);
                                if (student != null) {
                                    student.setId(snapshot.getKey());
                                    studentDataMap.put(studentId, student);
                                    Log.d(TAG, "Loaded student data for: " + studentId);
                                } else {
                                    Log.w(TAG, "Student data is null for: " + studentId);
                                }
                            } else {
                                Log.w(TAG, "Student not found in users: " + studentId);
                            }

                            studentsLoaded[0]++;
                            Log.d(TAG, "Students loaded: " + studentsLoaded[0] + "/" + totalStudents);

                            // Check if all student data has been loaded
                            if (studentsLoaded[0] == totalStudents) {
                                progressBar.setVisibility(View.GONE);
                                updateUI();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            studentsLoaded[0]++;
                            Log.e(TAG, "Failed to load student data for: " + studentId + " - " + error.getMessage());

                            // Continue even if some student data fails to load
                            if (studentsLoaded[0] == totalStudents) {
                                progressBar.setVisibility(View.GONE);
                                updateUI();
                            }
                        }
                    });
        }
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI with " + applicationList.size() + " applications");
        filterApplications();

        if (filteredApplicationList.isEmpty()) {
            showNoApplications("No applications match current filters");
        } else {
            showApplicationsList();
        }
    }

    private void filterApplications() {
        filteredApplicationList.clear();

        List<String> selectedStatuses = getSelectedStatuses();
        Log.d(TAG, "Filtering with statuses: " + selectedStatuses);

        for (JobApplication application : applicationList) {
            String applicationStatus = application.getStatus();
            if (applicationStatus == null) {
                applicationStatus = "pending"; // Default status
                application.setStatus(applicationStatus);
            }

            // Normalize status to lowercase for comparison
            String normalizedStatus = applicationStatus.toLowerCase().trim();

            if (selectedStatuses.contains(normalizedStatus)) {
                filteredApplicationList.add(application);
            }
        }

        Log.d(TAG, "Filtered applications: " + filteredApplicationList.size() + " out of " + applicationList.size());

        applicationsAdapter.updateList(filteredApplicationList);
        updateApplicationCount();
    }

    private List<String> getSelectedStatuses() {
        List<String> selectedStatuses = new ArrayList<>();

        // If "All" is checked → add all statuses
        if (chipAll.isChecked()) {
            selectedStatuses.add("pending");
            selectedStatuses.add("reviewed");
            selectedStatuses.add("accepted");
            selectedStatuses.add("rejected");
        } else {
            // If "All" is NOT checked → check individual chips
            if (chipPending.isChecked()) selectedStatuses.add("pending");
            if (chipReviewed.isChecked()) selectedStatuses.add("reviewed");
            if (chipAccepted.isChecked()) selectedStatuses.add("accepted");
            if (chipRejected.isChecked()) selectedStatuses.add("rejected");

            // If user unchecks everything → default to ALL
            if (selectedStatuses.isEmpty()) {
                selectedStatuses.add("pending");
                selectedStatuses.add("reviewed");
                selectedStatuses.add("accepted");
                selectedStatuses.add("rejected");
                chipAll.setChecked(true); // Visually show All is selected
            }
        }

        Log.d(TAG, "Selected statuses: " + selectedStatuses);
        return selectedStatuses;
    }

    private void updateApplicationCount() {
        String countText = filteredApplicationList.size() + " application" + (filteredApplicationList.size() != 1 ? "s" : "");
        tvApplicationCount.setText(countText);
        Log.d(TAG, "Application count updated: " + countText);
    }

    private void showApplicationsList() {
        rvApplications.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        Log.d(TAG, "Showing applications list");
    }

    private void showNoApplications(String message) {
        rvApplications.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);

        // Update empty state message
        TextView tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }

        Log.d(TAG, "Showing empty state: " + message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firebase listeners
        if (applicationsListener != null) {
            mDatabase.child("applications").removeEventListener(applicationsListener);
        }
    }

    // Interface methods implementation
    @Override
    public void onViewCandidateProfile(JobApplication application) {
        Log.d(TAG, "Viewing candidate profile: " + application.getStudentId());
        Intent intent = new Intent(this, com.example.careergo.Employer.StudentView.class);
        intent.putExtra("studentId", application.getStudentId());
        intent.putExtra("applicationId", application.getApplicationId());
        startActivity(intent);
    }

    @Override
    public void onUpdateApplicationStatus(JobApplication application, String newStatus) {
        Log.d(TAG, "Updating application " + application.getApplicationId() + " to status: " + newStatus);
        updateApplicationStatus(application, newStatus);
    }

    @Override
    public void onViewResume(JobApplication application) {
        User student = studentDataMap.get(application.getStudentId());
        if (student != null && student.getResumeUrl() != null && !student.getResumeUrl().isEmpty()) {
            Log.d(TAG, "Opening resume URL: " + student.getResumeUrl());
            openResume(student.getResumeUrl());
        } else if (student != null && student.getCvBase64() != null && !student.getCvBase64().isEmpty()) {
            Toast.makeText(this, "CV available in base64 format", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Resume not available for this student", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No resume available for student: " + application.getStudentId());
        }
    }

    private void openResume(String resumeUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(resumeUrl), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Verify that there's an app to handle PDFs
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening resume: " + e.getMessage());
            Toast.makeText(this, "Error opening resume", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateApplicationStatus(JobApplication application, String newStatus) {
        mDatabase.child("applications").child(application.getApplicationId()).child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    application.setStatus(newStatus);
                    filterApplications(); // Refresh the filtered list
                    Toast.makeText(this, "Application status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successfully updated application status to: " + newStatus);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update application status", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update application status: " + e.getMessage());
                });
    }
}