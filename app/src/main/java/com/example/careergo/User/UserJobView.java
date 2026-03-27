package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.Model.Job;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class UserJobView extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private ImageView ivPopOut, ivCompanyLogo, ivStudentLogo;
    private TextView tvRole, tvCompanyLocation, tvStudentApplied, tvStudentCount,
            tvJobDescription, tvSalary, tvResponsibility, tvWorkType, tvCategory,
            tvAgeRequirement, tvGenderPreference;
    private MaterialButton btnApply, btnSaved;
    private ChipGroup requiredSkillSetChipGroup;

    // Data
    private String jobId;
    private Job currentJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_job_view);

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
        ivCompanyLogo = findViewById(R.id.ivCompanyLogo);
        ivStudentLogo = findViewById(R.id.ivStudentLogo);

        // TextViews
        tvRole = findViewById(R.id.tvRole);
        tvCompanyLocation = findViewById(R.id.tvCompanyLocation);
        tvStudentApplied = findViewById(R.id.tvStudentApplied);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvJobDescription = findViewById(R.id.tvJobDescription);
        tvSalary = findViewById(R.id.tvSalary);
        tvResponsibility = findViewById(R.id.tvResponsibility);

        // New TextViews for additional fields
        tvWorkType = findViewById(R.id.tvWorkType);
        tvCategory = findViewById(R.id.tvCategory);
        tvAgeRequirement = findViewById(R.id.tvAgeRequirement);
        tvGenderPreference = findViewById(R.id.tvGenderPreference);

        // Buttons
        btnApply = findViewById(R.id.btnApply);
        btnSaved = findViewById(R.id.btnSaved);

        // ChipGroup
        requiredSkillSetChipGroup = findViewById(R.id.requiredSkillSetChipGroup);
    }

    private void setupClickListeners() {
        // Back button
        ivPopOut.setOnClickListener(v -> finish());

        // Apply button
        btnApply.setOnClickListener(v -> applyForJob());

        // Save button
        btnSaved.setOnClickListener(v -> saveForLater());
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
                        checkIfAlreadyApplied(); // Check if current user has already applied
                        checkIfJobIsSaved(); // Check if job is saved
                    } else {
                        Toast.makeText(UserJobView.this, "Failed to load job details", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(UserJobView.this, "Job not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserJobView.this, "Failed to load job: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
        } else {
            ivCompanyLogo.setImageResource(R.drawable.ic_apple_logo);
        }

        // Set job role
        tvRole.setText(job.getJobTitle());

        // Build company location string
        String companyLocation = "• " + job.getCompanyName() + " • " + job.getCity() + " •";
        tvCompanyLocation.setText(companyLocation);

        // Set work type (using getCategory() instead of getJobCategory())
        if (job.getWorkType() != null && !job.getWorkType().isEmpty()) {
            tvWorkType.setText("Work Type: " + job.getWorkType());
        } else {
            tvWorkType.setText("Work Type: Not specified");
        }

        // Set job category (using getCategory() instead of getJobCategory())
        if (job.getDesignation() != null && !job.getDesignation().isEmpty()) {
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
            tvSalary.setText("PKR : " + job.getSalary() + "/Mo");
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
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(getResources().getColor(R.color.text_color));
                chip.setClickable(false);
                chip.setChipStrokeWidth(1);
                chip.setChipStrokeColorResource(R.color.chip_stroke_color);
                requiredSkillSetChipGroup.addView(chip);
            }
        } else {
            Chip chip = new Chip(this);
            chip.setText("No specific skills required");
            chip.setChipBackgroundColorResource(R.color.chip_background);
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
                        tvStudentCount.setText(String.valueOf(applicationsCount));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        tvStudentCount.setText("0");
                    }
                });
    }

    private void checkIfAlreadyApplied() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Check if user has already applied for this job
        mDatabase.child("applications")
                .orderByChild("studentId_jobId")
                .equalTo(userId + "_" + jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // User has already applied
                            updateApplyButton(true);
                        } else {
                            // User hasn't applied yet
                            updateApplyButton(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Silent fail
                    }
                });
    }

    private void updateApplyButton(boolean hasApplied) {
        if (hasApplied) {
            btnApply.setText("Already Applied");
            btnApply.setEnabled(false);
            btnApply.setBackgroundColor(getResources().getColor(R.color.disabled_button_color));
        } else {
            btnApply.setText("Apply Now");
            btnApply.setEnabled(true);
            btnApply.setBackgroundColor(getResources().getColor(R.color.button_color));
        }
    }

    private void applyForJob() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to apply for jobs", Toast.LENGTH_SHORT).show();
            // Redirect to login page
            Intent intent = new Intent(UserJobView.this, LoginActivity.class);
            startActivity(intent);
            return;
        }

        String userId = currentUser.getUid();

        // Check if already applied (double check)
        mDatabase.child("applications")
                .orderByChild("studentId_jobId")
                .equalTo(userId + "_" + jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Toast.makeText(UserJobView.this, "You have already applied for this job", Toast.LENGTH_SHORT).show();
                            updateApplyButton(true);
                        } else {
                            // Create job application
                            createJobApplication(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(UserJobView.this, "Failed to check application status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createJobApplication(String userId) {
        String applicationId = mDatabase.child("applications").push().getKey();
        if (applicationId == null) {
            Toast.makeText(this, "Failed to create application", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user data for application
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        // Create application object
                        saveApplicationToFirebase(applicationId, userId, user);
                    } else {
                        Toast.makeText(UserJobView.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(UserJobView.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserJobView.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveApplicationToFirebase(String applicationId, String userId, User user) {
        // Create application data
        mDatabase.child("applications").child(applicationId).child("applicationId").setValue(applicationId);
        mDatabase.child("applications").child(applicationId).child("jobId").setValue(jobId);
        mDatabase.child("applications").child(applicationId).child("studentId").setValue(userId);
        mDatabase.child("applications").child(applicationId).child("studentName").setValue(user.getFirstName() + " " + user.getLastName());
        mDatabase.child("applications").child(applicationId).child("studentEmail").setValue(user.getEmail());
        mDatabase.child("applications").child(applicationId).child("studentPhone").setValue(user.getMobileNo());
        mDatabase.child("applications").child(applicationId).child("appliedDate").setValue(System.currentTimeMillis());
        mDatabase.child("applications").child(applicationId).child("status").setValue("pending");

        // Store composite field for querying
        mDatabase.child("applications").child(applicationId).child("studentId_jobId").setValue(userId + "_" + jobId);

        // Update UI
        updateApplyButton(true);

        // Refresh applications count
        loadApplicationsCount();

        Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show();

        // Send notification to employer
        sendNotificationToEmployer(user.getFirstName() + " " + user.getLastName());
    }

    private void sendNotificationToEmployer(String studentName) {
        if (currentJob == null) return;

        String employerId = currentJob.getEmployerId();
        String notificationId = mDatabase.child("notifications").push().getKey();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser.getUid();

        if (notificationId != null) {
            mDatabase.child("notifications").child(notificationId).child("employerId").setValue(employerId);
            mDatabase.child("notifications").child(notificationId).child("userId").setValue(userId);
            mDatabase.child("notifications").child(notificationId).child("jobId").setValue(jobId);
            mDatabase.child("notifications").child(notificationId).child("jobTitle").setValue(currentJob.getJobTitle());
            mDatabase.child("notifications").child(notificationId).child("message").setValue(studentName + " applied for " + currentJob.getJobTitle());
            mDatabase.child("notifications").child(notificationId).child("timestamp").setValue(System.currentTimeMillis());
            mDatabase.child("notifications").child(notificationId).child("read").setValue(false);
            mDatabase.child("notifications").child(notificationId).child("type").setValue("new_application");
        }
    }

    private void saveForLater() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save jobs", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserJobView.this, LoginActivity.class);
            startActivity(intent);
            return;
        }

        String userId = currentUser.getUid();

        // Check if already saved
        mDatabase.child("savedJobs")
                .orderByChild("studentId_jobId")
                .equalTo(userId + "_" + jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Job already saved, remove it
                            removeFromSavedJobs(userId);
                        } else {
                            // Save the job
                            addToSavedJobs(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(UserJobView.this, "Failed to check saved status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToSavedJobs(String userId) {
        String savedJobId = mDatabase.child("savedJobs").push().getKey();
        if (savedJobId == null) {
            Toast.makeText(this, "Failed to save job", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create saved job entry
        mDatabase.child("savedJobs").child(savedJobId).child("savedJobId").setValue(savedJobId);
        mDatabase.child("savedJobs").child(savedJobId).child("jobId").setValue(jobId);
        mDatabase.child("savedJobs").child(savedJobId).child("studentId").setValue(userId);
        mDatabase.child("savedJobs").child(savedJobId).child("savedDate").setValue(System.currentTimeMillis());

        // Store composite field for querying
        mDatabase.child("savedJobs").child(savedJobId).child("studentId_jobId").setValue(userId + "_" + jobId);

        // Update button UI
        updateSaveButton(true);
        Toast.makeText(this, "Job saved for later", Toast.LENGTH_SHORT).show();
    }

    private void removeFromSavedJobs(String userId) {
        mDatabase.child("savedJobs")
                .orderByChild("studentId_jobId")
                .equalTo(userId + "_" + jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                        updateSaveButton(false);
                        Toast.makeText(UserJobView.this, "Job removed from saved", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(UserJobView.this, "Failed to remove saved job", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSaveButton(boolean isSaved) {
        if (isSaved) {
            btnSaved.setText("Saved");
            btnSaved.setBackgroundColor(getResources().getColor(R.color.saved_button_color));
        } else {
            btnSaved.setText("Save For Later");
            btnSaved.setBackgroundColor(getResources().getColor(R.color.button_color));
        }
    }

    private void checkIfJobIsSaved() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            updateSaveButton(false);
            return;
        }

        String userId = currentUser.getUid();

        mDatabase.child("savedJobs")
                .orderByChild("studentId_jobId")
                .equalTo(userId + "_" + jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            updateSaveButton(true);
                        } else {
                            updateSaveButton(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Silent fail
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (jobId != null) {
            checkIfAlreadyApplied();
            checkIfJobIsSaved();
        }
    }
}