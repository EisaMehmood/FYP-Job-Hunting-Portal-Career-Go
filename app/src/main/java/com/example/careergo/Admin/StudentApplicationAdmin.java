package com.example.careergo.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

public class StudentApplicationAdmin extends AppCompatActivity {

    private TextView tvJobTitle, tvCompanyName, tvLocation, tvWorkType, tvSalary, tvPostedDate, tvDeadline;
    private TextView tvDescription, tvRequirements, tvApplicationsCount;
    private Chip chipStatus;
    private Button btnViewApplications, btnToggleStatus;
    private ImageButton ibBack;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private String jobId;
    private Job currentJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_application_admin);

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
        tvSalary = findViewById(R.id.tvSalary);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvDescription = findViewById(R.id.tvDescription);
        tvRequirements = findViewById(R.id.tvRequirements);
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount);
        chipStatus = findViewById(R.id.chipStatus);
        ibBack = findViewById(R.id.ibBack);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());

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
                    Toast.makeText(StudentApplicationAdmin.this, "Job not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(StudentApplicationAdmin.this, "Failed to load job details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayJobDetails() {
        tvJobTitle.setText(currentJob.getJobTitle());
        tvCompanyName.setText(currentJob.getCompanyName());
        tvLocation.setText(currentJob.getCity());
        tvWorkType.setText(currentJob.getWorkType());
        tvSalary.setText(currentJob.getSalary());

        // Format and display dates
        if (currentJob.getTimestamp() != 0) {
            String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(currentJob.getTimestamp()));
            tvPostedDate.setText("Posted " + formattedDate);
        }

        // Set description and requirements
        tvDescription.setText(currentJob.getJobDescription() != null ? currentJob.getJobDescription() : "No description provided.");
        tvRequirements.setText(currentJob.getJobResponsibilities() != null ? currentJob.getJobResponsibilities() : "No requirements specified.");

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

}