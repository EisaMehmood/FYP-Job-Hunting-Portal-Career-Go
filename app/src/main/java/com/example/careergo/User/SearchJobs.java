package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import com.example.careergo.Adapters.RecentJobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchJobs extends AppCompatActivity implements RecentJobsAdapter.OnJobClickListener {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private TextInputEditText etSearch;
    private RecyclerView rvJobs;
    private TextView tvSearchHeading;

    // Adapter and Data
    private RecentJobsAdapter jobsAdapter;
    private List<Job> allJobsList = new ArrayList<>();
    private List<Job> filteredJobsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_jobs);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupSearchListener();
        loadAllJobs();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // Text Input
        etSearch = findViewById(R.id.etSearch);

        // RecyclerView
        rvJobs = findViewById(R.id.rvJobs);

        // Text View
        tvSearchHeading = findViewById(R.id.tvSearchHeading);
    }

    private void setupRecyclerView() {
        jobsAdapter = new RecentJobsAdapter(filteredJobsList, this);
        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(jobsAdapter);
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

    private void loadAllJobs() {
        // Query to get all active jobs
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
                        allJobsList.add(job);
                    }
                }

                // Initially show all jobs
                filteredJobsList.clear();
                filteredJobsList.addAll(allJobsList);
                jobsAdapter.notifyDataSetChanged();

                // Update heading with job count
                updateJobsHeading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SearchJobs.this, "Failed to load jobs: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterJobs(String searchText) {
        filteredJobsList.clear();

        if (searchText.isEmpty()) {
            // Show all jobs if search is empty
            filteredJobsList.addAll(allJobsList);
        } else {
            // Filter jobs based on search text
            String query = searchText.toLowerCase().trim();
            for (Job job : allJobsList) {
                if (matchesSearch(job, query)) {
                    filteredJobsList.add(job);
                }
            }
        }

        jobsAdapter.notifyDataSetChanged();
        updateJobsHeading();
    }

    private boolean matchesSearch(Job job, String query) {
        // Search in job title
        if (job.getJobTitle() != null && job.getJobTitle().toLowerCase().contains(query)) {
            return true;
        }

        // Search in company name
        if (job.getCompanyName() != null && job.getCompanyName().toLowerCase().contains(query)) {
            return true;
        }

        // Search in location/city
        if (job.getCity() != null && job.getCity().toLowerCase().contains(query)) {
            return true;
        }

        // Search in designation
        if (job.getDesignation() != null && job.getDesignation().toLowerCase().contains(query)) {
            return true;
        }

        // Search in work type
        if (job.getWorkType() != null && job.getWorkType().toLowerCase().contains(query)) {
            return true;
        }

        // Search in required skills
        if (job.getRequiredSkills() != null) {
            for (String skill : job.getRequiredSkills()) {
                if (skill.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void updateJobsHeading() {
        String headingText = getResources().getString(R.string.header_all_jobs) +
                " (" + filteredJobsList.size() + ")";
        tvSearchHeading.setText(headingText);
    }

    @Override
    public void onJobClick(Job job) {
        // Open job details activity
        Intent intent = new Intent(SearchJobs.this, UserJobView.class);
        intent.putExtra("jobId", job.getJobId());
        startActivity(intent);
    }

    @Override
    public void onJobApplyClick(Job job) {
        // Open job details for application
        Intent intent = new Intent(SearchJobs.this, UserJobView.class);
        intent.putExtra("jobId", job.getJobId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh jobs list when returning to this activity
        if (jobsAdapter != null) {
            jobsAdapter.notifyDataSetChanged();
        }
    }
}