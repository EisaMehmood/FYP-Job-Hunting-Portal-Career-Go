package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JobViewEmployer extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private ImageView ivPopOut, ivEditJob, ivDeleteJob, ivCompanyLogo, ivStudentLogo, ivStudentApplied;
    private TextView tvRole, tvCompanyLocation, tvStudentApplied, tvJobDescription,
            tvSalary, tvResponsibility, tvJobDescriptionHeader, tvSalaryHeader,
            tvResponsibilityHeader, tvRequiredSkillSet, tvWorkType, tvCategory,
            tvAgeRequirement, tvGenderPreference;
    private MaterialCardView cvStudentApplied;
    private ChipGroup requiredSkillSetChipGroup;

    // Data
    private String jobId;
    private Job currentJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_view_employer);

        // Get job ID from intent
        jobId = getIntent().getStringExtra("jobId");
        if (jobId == null) {
            Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        loadJobDetails();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // ImageViews
        ivPopOut = findViewById(R.id.ivPopOut);
        ivEditJob = findViewById(R.id.ivEditJob);
        ivDeleteJob = findViewById(R.id.ivDeleteJob);
        ivCompanyLogo = findViewById(R.id.ivCompanyLogo);
        ivStudentLogo = findViewById(R.id.ivStudentLogo);
        ivStudentApplied = findViewById(R.id.ivStudentApplied);

        // TextViews
        tvRole = findViewById(R.id.tvRole);
        tvCompanyLocation = findViewById(R.id.tvCompanyLocation);
        tvStudentApplied = findViewById(R.id.tvStudentApplied);
        tvJobDescription = findViewById(R.id.tvJobDescription);
        tvSalary = findViewById(R.id.tvSalary);
        tvResponsibility = findViewById(R.id.tvResponsibility);
        tvJobDescriptionHeader = findViewById(R.id.tvJobDescriptionHeader);
        tvSalaryHeader = findViewById(R.id.tvSalaryHeader);
        tvResponsibilityHeader = findViewById(R.id.tvResponsibilityHeader);
        tvRequiredSkillSet = findViewById(R.id.tvRequiredSkillSet);

        // New TextViews for additional fields
        tvWorkType = findViewById(R.id.tvWorkType);
        tvCategory = findViewById(R.id.tvCategory);
        tvAgeRequirement = findViewById(R.id.tvAgeRequirement);
        tvGenderPreference = findViewById(R.id.tvGenderPreference);

        // Other Views
        cvStudentApplied = findViewById(R.id.cvStudentApplied);
        requiredSkillSetChipGroup = findViewById(R.id.requiredSkillSetChipGroup);
    }

    private void setupClickListeners() {
        // Back button
        ivPopOut.setOnClickListener(v -> finish());

        // Edit job button
        ivEditJob.setOnClickListener(v -> {
            Intent intent = new Intent(JobViewEmployer.this, JobEdit.class);
            intent.putExtra("jobId", jobId);
            startActivity(intent);
        });

        // Delete job button
        ivDeleteJob.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Student applied card - navigate to applications list
        cvStudentApplied.setOnClickListener(v -> {
            if (currentJob != null) {
                Intent intent = new Intent(JobViewEmployer.this, ApplicantView.class);
                intent.putExtra("jobId", jobId);
                intent.putExtra("jobTitle", currentJob.getJobTitle());
                startActivity(intent);
            }
        });
    }

    private void loadJobDetails() {
        mDatabase.child("jobs").child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentJob = dataSnapshot.getValue(Job.class);
                    if (currentJob != null) {
                        displayJobDetails(currentJob);
                        loadApplicationsCount(); // Load student applications count
                    } else {
                        Toast.makeText(JobViewEmployer.this, "Failed to load job details", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(JobViewEmployer.this, "Job not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(JobViewEmployer.this, "Failed to load job: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayJobDetails(Job job) {
        // Load company logo
        if (job.getCompanyImageUrl() != null && !job.getCompanyImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(job.getCompanyImageUrl())
                    .placeholder(R.drawable.ic_apple_logo)
                    .error(R.drawable.ic_apple_logo)
                    .into(ivCompanyLogo);
        }

        // Set job role and company info
        tvRole.setText(job.getJobTitle());

        // Build company location string
        String companyLocation = "• " + job.getCompanyName() + " • " + job.getCity() + " •";
        tvCompanyLocation.setText(companyLocation);

        // Set work type
        if (job.getWorkType() != null && !job.getWorkType().isEmpty()) {
            tvWorkType.setText("Work Type: " + job.getWorkType());
        } else {
            tvWorkType.setText("Work Type: Not specified");
        }

        // Set job category
        if (job.getDesignation()!= null && !job.getDesignation().isEmpty()) {
            tvCategory.setText("Category: " + job.getDesignation());
        } else {
            tvCategory.setText("Category: Not specified");
        }

        // Set age requirement
        if (job.getAgeRequirement() != null && !job.getAgeRequirement().isEmpty()) {
            tvAgeRequirement.setText("Age Requirement: " + job.getAgeRequirement());
        } else {
            tvAgeRequirement.setText("Age Requirement: Not specified");
        }

        // Set gender preference
        if (job.getGenderPreference() != null && !job.getGenderPreference().isEmpty()) {
            tvGenderPreference.setText("Gender Preference: " + job.getGenderPreference());
        } else {
            tvGenderPreference.setText("Gender Preference: Not specified");
        }

        // Set job description
        if (job.getJobDescription() != null && !job.getJobDescription().isEmpty()) {
            tvJobDescription.setText(job.getJobDescription());
        } else {
            tvJobDescription.setText("No job description provided.");
        }

        // Set salary
        if (job.getSalary() != null && !job.getSalary().isEmpty()) {
            tvSalary.setText(job.getSalary() + "/Mo");
        } else {
            tvSalary.setText("Not specified");
        }

        // Set job responsibilities
        if (job.getJobResponsibilities() != null && !job.getJobResponsibilities().isEmpty()) {
            tvResponsibility.setText(job.getJobResponsibilities());
        } else {
            tvResponsibility.setText("No responsibilities specified.");
        }

        // Set required skills
        requiredSkillSetChipGroup.removeAllViews();
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            for (String skill : job.getRequiredSkills()) {
                Chip chip = new Chip(this);
                chip.setText(skill);
                chip.setChipBackgroundColorResource(R.color.light_blue);
                chip.setTextColor(getResources().getColor(R.color.text_color));
                chip.setClickable(false);
                requiredSkillSetChipGroup.addView(chip);
            }
        } else {
            Chip chip = new Chip(this);
            chip.setText("No specific skills required");
            chip.setChipBackgroundColorResource(R.color.purple_200);
            chip.setTextColor(getResources().getColor(R.color.text_color));
            chip.setClickable(false);
            requiredSkillSetChipGroup.addView(chip);
        }
    }

    private void loadApplicationsCount() {
        if (currentJob == null) return;

        // Count applications for this job
        mDatabase.child("applications")
                .orderByChild("jobId")
                .equalTo(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long applicationsCount = dataSnapshot.getChildrenCount();
                        updateApplicationsCount(applicationsCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        updateApplicationsCount(0);
                    }
                });
    }

    private void updateApplicationsCount(long count) {
        String applicationsText = getResources().getString(R.string.heading_student_applied) +
                " (" + count + ")";
        tvStudentApplied.setText(applicationsText);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Job")
                .setMessage("Are you sure you want to delete this job posting? This action cannot be undone.")
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Job");
        builder.setMessage("Are you sure you want to delete this job posting? All applications for this job will also be deleted. This action cannot be undone.");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteJobAndApplications();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteJobAndApplications() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting job and applications...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // First, get all applications for this job
        mDatabase.child("applications")
                .orderByChild("jobId")
                .equalTo(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Create a list of application IDs to delete
                        List<String> applicationIds = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String applicationId = snapshot.getKey();
                            if (applicationId != null) {
                                applicationIds.add(applicationId);
                            }
                        }

                        // Delete all applications first
                        deleteApplications(applicationIds, progressDialog);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressDialog.dismiss();
                        Toast.makeText(JobViewEmployer.this,
                                "Failed to load applications: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteApplications(List<String> applicationIds, ProgressDialog progressDialog) {
        if (applicationIds.isEmpty()) {
            // No applications to delete, just delete the job
            deleteSingleJob(progressDialog);
            return;
        }

        // Use a counter to track completion
        final int[] completedDeletions = {0};
        int totalApplications = applicationIds.size();

        for (String applicationId : applicationIds) {
            mDatabase.child("applications").child(applicationId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        completedDeletions[0]++;

                        // Check if all applications have been deleted
                        if (completedDeletions[0] == totalApplications) {
                            // Now delete the job
                            deleteSingleJob(progressDialog);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedDeletions[0]++;
                        Log.e("JobViewEmployer", "Failed to delete application " + applicationId + ": " + e.getMessage());

                        // Continue with next deletion even if one fails
                        if (completedDeletions[0] == totalApplications) {
                            deleteSingleJob(progressDialog);
                        }
                    });
        }
    }

    private void deleteSingleJob(ProgressDialog progressDialog) {
        // Delete the job
        mDatabase.child("jobs").child(jobId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(JobViewEmployer.this,
                            "Job and all applications deleted successfully",
                            Toast.LENGTH_SHORT).show();

                    // Navigate back to job list
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(JobViewEmployer.this,
                            "Failed to delete job: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from edit activity
        loadJobDetails();
    }
}