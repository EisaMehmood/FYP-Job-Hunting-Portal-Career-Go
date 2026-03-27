package com.example.careergo.Admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.careergo.Adapters.JobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminJobsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialButton btnActiveJobs, btnInactiveJobs;
    private EditText etSearchJob;
    private RecyclerView rvJobs;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private DatabaseReference mDatabase;
    private JobsAdapter jobsAdapter;
    private List<Job> jobList;
    private List<Job> filteredJobList;
    private boolean showingActiveJobs = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_jobs);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchJobs();
        setupButtonListeners();
        setupSearch();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        btnActiveJobs = findViewById(R.id.btnActiveJobs);
        btnInactiveJobs = findViewById(R.id.btnInactiveJobs);
        etSearchJob = findViewById(R.id.etSearchJob);
        rvJobs = findViewById(R.id.rvJobs);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        jobList = new ArrayList<>();
        filteredJobList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        jobsAdapter = new JobsAdapter(filteredJobList,this);
        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(jobsAdapter);
    }

    private void fetchJobs() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("jobs").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                jobList.clear();
                for (DataSnapshot jobSnapshot : snapshot.getChildren()) {
                    Job job = jobSnapshot.getValue(Job.class);
                    if (job != null) {
                        job.setJobId(jobSnapshot.getKey());
                        jobList.add(job);
                    }
                }
                filterJobsByStatus();
                progressBar.setVisibility(View.GONE);
                updateEmptyState();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setText("Error loading jobs");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtonListeners() {
        btnActiveJobs.setOnClickListener(v -> {
            showingActiveJobs = true;
            updateButtonStates();
            filterJobsByStatus();
        });

        btnInactiveJobs.setOnClickListener(v -> {
            showingActiveJobs = false;
            updateButtonStates();
            filterJobsByStatus();
        });
    }

    private void updateButtonStates() {
        if (showingActiveJobs) {
            btnActiveJobs.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btnActiveJobs.setTextColor(getResources().getColor(R.color.white));
            btnInactiveJobs.setBackgroundColor(getResources().getColor(R.color.transparent));
            btnInactiveJobs.setTextColor(getResources().getColor(R.color.primary_color));
        } else {
            btnInactiveJobs.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btnInactiveJobs.setTextColor(getResources().getColor(R.color.white));
            btnActiveJobs.setBackgroundColor(getResources().getColor(R.color.transparent));
            btnActiveJobs.setTextColor(getResources().getColor(R.color.primary_color));
        }
    }

    private void setupSearch() {
        etSearchJob.addTextChangedListener(new TextWatcher() {
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

    private void filterJobsByStatus() {
        filteredJobList.clear();
        for (Job job : jobList) {
            if (showingActiveJobs && job.isActive()) {
                filteredJobList.add(job);
            } else if (!showingActiveJobs && !job.isActive()) {
                filteredJobList.add(job);
            }
        }
        jobsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void filterJobs(String query) {
        List<Job> tempList = new ArrayList<>();
        for (Job job : jobList) {
            if ((showingActiveJobs && job.isActive()) || (!showingActiveJobs && !job.isActive())) {
                if (query.isEmpty() ||
                        job.getJobTitle().toLowerCase().contains(query.toLowerCase()) ||
                        job.getCompanyName().toLowerCase().contains(query.toLowerCase()) ||
                        job.getCity().toLowerCase().contains(query.toLowerCase())) {
                    tempList.add(job);
                }
            }
        }
        filteredJobList.clear();
        filteredJobList.addAll(tempList);
        jobsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredJobList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvJobs.setVisibility(View.GONE);
            tvEmptyState.setText(showingActiveJobs ? "No active jobs found" : "No inactive jobs found");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvJobs.setVisibility(View.VISIBLE);
        }
    }
}