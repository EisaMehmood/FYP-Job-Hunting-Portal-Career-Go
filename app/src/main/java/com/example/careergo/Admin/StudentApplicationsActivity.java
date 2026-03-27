package com.example.careergo.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.careergo.Adapters.StudentApplicationsAdapter;
import com.example.careergo.Model.JobApplication;
import com.example.careergo.Model.Student;
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

public class StudentApplicationsActivity extends AppCompatActivity implements StudentApplicationsAdapter.OnApplicationActionListener {

    private Toolbar toolbar;
    private ImageView ivStudentAvatar;
    private TextView tvStudentName, tvStudentEmail, tvTotalApplications, tvEmptyMessage;
    private RecyclerView rvApplications;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipPending, chipReviewed, chipAccepted, chipRejected;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;

    private StudentApplicationsAdapter applicationsAdapter;
    private List<JobApplication> applicationList = new ArrayList<>();
    private List<JobApplication> filteredApplicationList = new ArrayList<>();
    private Map<String, String> jobTitlesMap = new HashMap<>(); // Store job titles by jobId
    private Map<String, String> companyNamesMap = new HashMap<>(); // Store company names by jobId

    private DatabaseReference mDatabase;
    private String studentId;
    private String studentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_applications);

        // Get student data from intent
        studentId = getIntent().getStringExtra("studentId");
        studentName = getIntent().getStringExtra("studentName");

        if (studentId == null) {
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadStudentData();
        loadStudentApplications();
        setupFilterListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivStudentAvatar = findViewById(R.id.ivStudentAvatar);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentEmail = findViewById(R.id.tvStudentEmail);
        tvTotalApplications = findViewById(R.id.tvTotalApplications);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        rvApplications = findViewById(R.id.rvApplications);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipReviewed = findViewById(R.id.chipReviewed);
        chipAccepted = findViewById(R.id.chipAccepted);
        chipRejected = findViewById(R.id.chipRejected);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Student Applications");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        applicationsAdapter = new StudentApplicationsAdapter(filteredApplicationList, jobTitlesMap, companyNamesMap, this);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(applicationsAdapter);
    }

    private void setupFilterListeners() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterApplications();
        });
    }

    private void loadStudentData() {
        mDatabase.child("users").child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Set student name from snapshot
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (firstName != null && lastName != null) {
                        tvStudentName.setText(firstName + " " + lastName);
                    } else {
                        tvStudentName.setText(studentName != null ? studentName : "Unknown Student");
                    }

                    if (email != null) {
                        tvStudentEmail.setText(email);
                    } else {
                        tvStudentEmail.setText("No email");
                    }

                    // Load profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(StudentApplicationsActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivStudentAvatar);
                    }
                } else {
                    // Use data from intent
                    tvStudentName.setText(studentName != null ? studentName : "Unknown Student");
                    tvStudentEmail.setText("No email available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Use intent data as fallback
                tvStudentName.setText(studentName != null ? studentName : "Unknown Student");
                tvStudentEmail.setText("No email available");
            }
        });
    }

    private void loadStudentApplications() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        llEmptyState.setVisibility(android.view.View.GONE);

        mDatabase.child("applications").orderByChild("studentId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        applicationList.clear();
                        jobTitlesMap.clear();
                        companyNamesMap.clear();

                        if (snapshot.exists()) {
                            List<String> jobIds = new ArrayList<>();

                            // First, collect all applications and job IDs
                            for (DataSnapshot applicationSnapshot : snapshot.getChildren()) {
                                JobApplication application = applicationSnapshot.getValue(JobApplication.class);
                                if (application != null) {
                                    application.setApplicationId(applicationSnapshot.getKey());
                                    applicationList.add(application);

                                    // Collect job IDs to fetch job details
                                    if (application.getJobId() != null && !jobIds.contains(application.getJobId())) {
                                        jobIds.add(application.getJobId());
                                    }
                                }
                            }

                            // Update total applications count
                            updateApplicationsCount();

                            // If we have job IDs, fetch their details
                            if (!jobIds.isEmpty()) {
                                fetchJobDetails(jobIds);
                            } else {
                                progressBar.setVisibility(android.view.View.GONE);
                                updateUI();
                            }
                        } else {
                            progressBar.setVisibility(android.view.View.GONE);
                            showNoApplications("This student hasn't applied to any jobs yet.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(android.view.View.GONE);
                        showNoApplications("Failed to load applications: " + error.getMessage());
                        Toast.makeText(StudentApplicationsActivity.this, "Error loading applications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchJobDetails(List<String> jobIds) {
        final int[] jobsLoaded = {0};
        final int totalJobs = jobIds.size();

        if (totalJobs == 0) {
            progressBar.setVisibility(android.view.View.GONE);
            updateUI();
            return;
        }

        for (String jobId : jobIds) {
            mDatabase.child("jobs").child(jobId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Get job title
                                String jobTitle = snapshot.child("jobTitle").getValue(String.class);
                                if (jobTitle != null) {
                                    jobTitlesMap.put(jobId, jobTitle);
                                } else {
                                    jobTitlesMap.put(jobId, "Unknown Job");
                                }

                                // Get company name
                                String companyName = snapshot.child("companyName").getValue(String.class);
                                if (companyName != null) {
                                    companyNamesMap.put(jobId, companyName);
                                } else {
                                    companyNamesMap.put(jobId, "Unknown Company");
                                }
                            } else {
                                jobTitlesMap.put(jobId, "Deleted Job");
                                companyNamesMap.put(jobId, "Unknown Company");
                            }

                            jobsLoaded[0]++;
                            if (jobsLoaded[0] == totalJobs) {
                                progressBar.setVisibility(android.view.View.GONE);
                                updateUI();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            jobTitlesMap.put(jobId, "Error Loading Job");
                            companyNamesMap.put(jobId, "Error Loading Company");

                            jobsLoaded[0]++;
                            if (jobsLoaded[0] == totalJobs) {
                                progressBar.setVisibility(android.view.View.GONE);
                                updateUI();
                            }
                        }
                    });
        }
    }

    private void updateApplicationsCount() {
        String countText = applicationList.size() + " application" + (applicationList.size() != 1 ? "s" : "");
        tvTotalApplications.setText(countText);
    }

    private void updateUI() {
        filterApplications();

        if (filteredApplicationList.isEmpty()) {
            if (applicationList.isEmpty()) {
                showNoApplications("This student hasn't applied to any jobs yet.");
            } else {
                showNoApplications("No applications match the selected filters.");
            }
        } else {
            showApplicationsList();
        }
    }

    private void filterApplications() {
        filteredApplicationList.clear();

        List<String> selectedStatuses = getSelectedStatuses();

        for (JobApplication application : applicationList) {
            if (selectedStatuses.contains(application.getStatus())) {
                filteredApplicationList.add(application);
            }
        }

        applicationsAdapter.updateList(filteredApplicationList);
        updateApplicationsCount();
    }

    private List<String> getSelectedStatuses() {
        List<String> selectedStatuses = new ArrayList<>();

        if (chipAll.isChecked() || (!chipPending.isChecked() && !chipReviewed.isChecked() &&
                !chipAccepted.isChecked() && !chipRejected.isChecked())) {
            selectedStatuses.add("pending");
            selectedStatuses.add("reviewed");
            selectedStatuses.add("accepted");
            selectedStatuses.add("rejected");
        } else {
            if (chipPending.isChecked()) selectedStatuses.add("pending");
            if (chipReviewed.isChecked()) selectedStatuses.add("reviewed");
            if (chipAccepted.isChecked()) selectedStatuses.add("accepted");
            if (chipRejected.isChecked()) selectedStatuses.add("rejected");
        }

        return selectedStatuses;
    }

    private void showApplicationsList() {
        rvApplications.setVisibility(android.view.View.VISIBLE);
        llEmptyState.setVisibility(android.view.View.GONE);
    }

    private void showNoApplications(String message) {
        rvApplications.setVisibility(android.view.View.GONE);
        llEmptyState.setVisibility(android.view.View.VISIBLE);
        tvEmptyMessage.setText(message);
    }

    @Override
    public void onViewJobDetails(JobApplication application) {
        // Navigate to job details
        Intent intent = new Intent(this, StudentApplicationAdmin.class);
        intent.putExtra("jobId", application.getJobId());
        startActivity(intent);
    }

    @Override
    public void onViewApplicationDetails(JobApplication application) {
       
    }

    @Override
    public void onUpdateApplicationStatus(JobApplication application, String newStatus) {
        updateApplicationStatus(application, newStatus);
    }



    private void updateApplicationStatus(JobApplication application, String newStatus) {
        mDatabase.child("applications").child(application.getApplicationId()).child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    application.setStatus(newStatus);
                    filterApplications();
                    Toast.makeText(this, "Application status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update application status", Toast.LENGTH_SHORT).show();
                });
    }
}