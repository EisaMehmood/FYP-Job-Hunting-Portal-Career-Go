package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.careergo.Adapters.JobAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JobActivity extends AppCompatActivity implements JobAdapter.OnJobClickListener {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private TextInputEditText etSearch;
    private RecyclerView rvJobs;
    private JobAdapter jobAdapter;

    // Data
    private List<Job> jobList = new ArrayList<>();
    private List<Job> filteredJobList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupSearchListener();
        loadJobs();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        rvJobs = findViewById(R.id.rvJobs);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Add job button
        findViewById(R.id.ivAddJob).setOnClickListener(v -> {
            Intent intent = new Intent(JobActivity.this, JobCreation.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        jobAdapter = new JobAdapter(filteredJobList, this);
        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(jobAdapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterJobs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadJobs() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String employerId = currentUser.getUid();

        // Query to get only jobs posted by current employer
        Query jobsQuery = mDatabase.child("jobs")
                .orderByChild("employerId")
                .equalTo(employerId);

        jobsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                jobList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Job job = snapshot.getValue(Job.class);
                    if (job != null && job.isActive()) {
                        jobList.add(job);
                    }
                }
                filteredJobList.clear();
                filteredJobList.addAll(jobList);
                jobAdapter.notifyDataSetChanged();

                // Update record count
                updateRecordCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(JobActivity.this, "Failed to load jobs: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterJobs(String searchText) {
        filteredJobList.clear();

        if (searchText.isEmpty()) {
            filteredJobList.addAll(jobList);
        } else {
            String query = searchText.toLowerCase().trim();
            for (Job job : jobList) {
                if (job.getJobTitle().toLowerCase().contains(query) ||
                        job.getCompanyName().toLowerCase().contains(query) ||
                        job.getCity().toLowerCase().contains(query) ||
                        (job.getDesignation() != null && job.getDesignation().toLowerCase().contains(query))) {
                    filteredJobList.add(job);
                }
            }
        }
        jobAdapter.notifyDataSetChanged();
        updateRecordCount();
    }

    private void updateRecordCount() {
         TextView tvRecordHeading = findViewById(R.id.tvRecordHeading);
        String recordText = getResources().getString(R.string.fragment_jobs_record_heading) +
                " (" + filteredJobList.size() + ")";
        tvRecordHeading.setText(recordText);
    }

    @Override
    public void onJobClick(Job job) {
        // Open job details activity
        Intent intent = new Intent(JobActivity.this, JobViewEmployer.class);
        intent.putExtra("jobId", job.getJobId());
        startActivity(intent);
    }

    @Override
    public void onJobDeleteClick(Job job) {
        // Delete job from Firebase
        deleteJob(job);
    }

    @Override
    public void onJobMoreClick(Job job, View anchorView) {

    }

    private void deleteJob(Job job) {
        // Instead of actually deleting, we can mark it as inactive
        mDatabase.child("jobs").child(job.getJobId()).child("active").setValue(false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Job deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from other activities
        loadJobs();
    }
}