package com.example.careergo.Admin;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.careergo.Employer.JobEdit;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminJobDetailsActivity extends AppCompatActivity {

    private TextView tvJobTitle, tvCompanyName, tvLocation, tvWorkType, tvAgeRequirement,
            tvGenderPreference, tvSalary, tvPostedDate, tvDeadline;
    private TextView tvDescription, tvRequirements, tvApplicationsCount;
    private Chip chipStatus;
    private Button btnViewApplications, btnToggleStatus, btnEditJob, btnDeleteJob;
    private ImageButton ibBack;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private String jobId;
    private Job currentJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_job_details);

        initializeViews();
        setupClickListeners();

        // Get job ID from intent
        jobId = getIntent().getStringExtra("jobId");
        if (jobId == null) {
            Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        loadJobDetails();
    }

    private void initializeViews() {
        tvJobTitle = findViewById(R.id.tvJobTitle);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvLocation = findViewById(R.id.tvLocation);
        tvWorkType = findViewById(R.id.tvWorkType);
        tvAgeRequirement = findViewById(R.id.tvAgeRequirement);
        tvGenderPreference = findViewById(R.id.tvGenderPreference);
        tvSalary = findViewById(R.id.tvSalary);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvDescription = findViewById(R.id.tvDescription);
        tvRequirements = findViewById(R.id.tvRequirements);
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount);
        chipStatus = findViewById(R.id.chipStatus);
        btnViewApplications = findViewById(R.id.btnViewApplications);
        btnToggleStatus = findViewById(R.id.btnToggleStatus);
        btnEditJob = findViewById(R.id.btnEditJob);
        btnDeleteJob = findViewById(R.id.btnDeleteJob);
        ibBack = findViewById(R.id.ibBack);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());

        btnToggleStatus.setOnClickListener(v -> {
            if (currentJob != null) {
                toggleJobStatus();
            }
        });

        btnViewApplications.setOnClickListener(v -> {
            if (currentJob != null) {
                Intent intent = new Intent(this, AdminJobApplicationsActivity.class);
                intent.putExtra("jobId", jobId);
                intent.putExtra("jobTitle", currentJob.getJobTitle());
                startActivity(intent);
            }
        });

        btnEditJob.setOnClickListener(v -> {
            if (currentJob != null) {
                editJob();
            }
        });

        btnDeleteJob.setOnClickListener(v -> {
            if (currentJob != null) {
                showDeleteConfirmationDialog();
            }
        });
    }

    private void loadJobDetails() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        mDatabase.child("jobs").child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(android.view.View.GONE);

                if (snapshot.exists()) {
                    currentJob = snapshot.getValue(Job.class);
                    if (currentJob != null) {
                        currentJob.setJobId(snapshot.getKey());
                        displayJobDetails();
                        loadJobStats();
                    }
                } else {
                    Toast.makeText(AdminJobDetailsActivity.this, "Job not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(AdminJobDetailsActivity.this, "Failed to load job details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayJobDetails() {
        // Set basic job information
        tvJobTitle.setText(currentJob.getJobTitle());
        tvCompanyName.setText(currentJob.getCompanyName());
        tvLocation.setText(currentJob.getCity());
        tvWorkType.setText(currentJob.getWorkType());

        // Set age requirement with default if empty
        if (currentJob.getAgeRequirement() != null && !currentJob.getAgeRequirement().isEmpty()) {
            tvAgeRequirement.setText("Age: " + currentJob.getAgeRequirement());
        } else {
            tvAgeRequirement.setText("Age: Not specified");
        }

        // Set gender preference with default if empty
        if (currentJob.getGenderPreference() != null && !currentJob.getGenderPreference().isEmpty()) {
            tvGenderPreference.setText("Gender: " + currentJob.getGenderPreference());
        } else {
            tvGenderPreference.setText("Gender: Not specified");
        }

        tvSalary.setText(currentJob.getSalary());

        // Format and display posted date
        if (currentJob.getTimestamp() != 0) {
            String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(currentJob.getTimestamp()));
            tvPostedDate.setText("Posted " + formattedDate);
        } else {
            tvPostedDate.setText("Posted date not available");
        }

        // Set description and requirements
        if (currentJob.getJobDescription() != null && !currentJob.getJobDescription().isEmpty()) {
            tvDescription.setText(currentJob.getJobDescription());
        } else {
            tvDescription.setText("No job description provided.");
        }

        if (currentJob.getJobResponsibilities() != null && !currentJob.getJobResponsibilities().isEmpty()) {
            tvRequirements.setText(currentJob.getJobResponsibilities());
        } else {
            tvRequirements.setText("No job responsibilities specified.");
        }

        // Update status
        updateStatusUI(currentJob.isActive());
    }

    private void updateStatusUI(boolean isActive) {
        if (isActive) {
            chipStatus.setText("Active");
            chipStatus.setChipBackgroundColorResource(R.color.light_green);
            btnToggleStatus.setText("Deactivate Job");
        } else {
            chipStatus.setText("Inactive");
            chipStatus.setChipBackgroundColorResource(R.color.light_gray);
            btnToggleStatus.setText("Activate Job");
        }
    }

    private void loadJobStats() {
        // Load application count
        mDatabase.child("applications").orderByChild("jobId").equalTo(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long applicationCount = snapshot.getChildrenCount();
                        tvApplicationsCount.setText(String.valueOf(applicationCount));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvApplicationsCount.setText("0");
                    }
                });
    }

    private void toggleJobStatus() {
        boolean newStatus = !currentJob.isActive();

        mDatabase.child("jobs").child(jobId).child("active")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    currentJob.setActive(newStatus);
                    updateStatusUI(newStatus);

                    String message = newStatus ? "Job activated successfully" : "Job deactivated successfully";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update job status", Toast.LENGTH_SHORT).show();
                });
    }

    private void editJob() {
        // Navigate to JobEdit activity
        Intent intent = new Intent(this, JobEdit.class);
        intent.putExtra("jobId", jobId);
        startActivity(intent);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Job")
                .setMessage("Are you sure you want to delete this job? This action cannot be undone and will also delete all associated applications.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteJob();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteJob() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        // First, delete all applications for this job
        mDatabase.child("applications").orderByChild("jobId").equalTo(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Delete all applications
                        for (DataSnapshot applicationSnapshot : snapshot.getChildren()) {
                            applicationSnapshot.getRef().removeValue();
                        }

                        // Then delete the job itself
                        mDatabase.child("jobs").child(jobId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(android.view.View.GONE);
                                    Toast.makeText(AdminJobDetailsActivity.this, "Job deleted successfully", Toast.LENGTH_SHORT).show();
                                    finish(); // Go back to previous activity
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(android.view.View.GONE);
                                    Toast.makeText(AdminJobDetailsActivity.this, "Failed to delete job", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(AdminJobDetailsActivity.this, "Failed to delete applications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh job details when returning from edit activity
        if (jobId != null) {
            loadJobDetails();
        }
    }
}