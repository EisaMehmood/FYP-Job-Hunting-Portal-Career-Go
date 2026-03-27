package com.example.careergo.Admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Adapters.JobsAdapter;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminEmployerJobsActivity extends AppCompatActivity {

    private RecyclerView rvJobs;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipActive, chipInactive;
    private ProgressBar progressBar;
    private TextView tvTitle, tvJobCount;
    private ImageButton ibBack;
    private LinearLayout llEmptyState;

    private JobsAdapter jobsAdapter;
    private List<Job> jobList = new ArrayList<>();
    private List<Job> filteredJobList = new ArrayList<>();

    private DatabaseReference mDatabase;
    private String employerId;
    private String employerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_employer_jobs);

        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        // Get employer data from intent
        employerId = getIntent().getStringExtra("employerId");
        employerName = getIntent().getStringExtra("employerName");

        if (employerId == null) {
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Set title with employer name
        if (employerName != null) {
            tvTitle.setText(employerName + "'s Jobs");
        }

        loadEmployerJobs();
        setupSearchAndFilter();
    }

    private void initializeViews() {
        rvJobs = findViewById(R.id.rvJobs);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipActive = findViewById(R.id.chipActive);
        chipInactive = findViewById(R.id.chipInactive);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        tvJobCount = findViewById(R.id.tvJobCount);
        ibBack = findViewById(R.id.ibBack);
        llEmptyState = findViewById(R.id.llEmptyState);
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        jobsAdapter = new JobsAdapter(filteredJobList, AdminEmployerJobsActivity.this);
        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(jobsAdapter);
    }

    private void setupSearchAndFilter() {
        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterJobs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter functionality
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterJobs();
        });
    }

    private void loadEmployerJobs() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        llEmptyState.setVisibility(android.view.View.GONE);

        mDatabase.child("jobs").orderByChild("employerId").equalTo(employerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(android.view.View.GONE);
                        jobList.clear();

                        for (DataSnapshot jobSnapshot : snapshot.getChildren()) {
                            Job job = jobSnapshot.getValue(Job.class);
                            if (job != null) {
                                job.setJobId(jobSnapshot.getKey());
                                jobList.add(job);
                            }
                        }

                        updateJobCount();
                        filterJobs();

                        // Show empty state if no jobs
                        if (jobList.isEmpty()) {
                            llEmptyState.setVisibility(android.view.View.VISIBLE);
                            rvJobs.setVisibility(android.view.View.GONE);
                        } else {
                            llEmptyState.setVisibility(android.view.View.GONE);
                            rvJobs.setVisibility(android.view.View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(android.view.View.GONE);
                        llEmptyState.setVisibility(android.view.View.VISIBLE);
                        rvJobs.setVisibility(android.view.View.GONE);
                    }
                });
    }

    private void filterJobs() {
        filteredJobList.clear();

        String searchQuery = etSearch.getText().toString().toLowerCase().trim();
        int selectedChipId = chipGroupFilter.getCheckedChipId();

        for (Job job : jobList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    job.getJobTitle().toLowerCase().contains(searchQuery) ||
                    (job.getJobDescription() != null && job.getJobDescription().toLowerCase().contains(searchQuery));

            boolean matchesFilter = true;
            if (selectedChipId == R.id.chipActive) {
                matchesFilter = job.isActive();
            } else if (selectedChipId == R.id.chipInactive) {
                matchesFilter = !job.isActive();
            }

            if (matchesSearch && matchesFilter) {
                filteredJobList.add(job);
            }
        }

        jobsAdapter.updateList(filteredJobList);
        updateJobCount();
    }

    private void updateJobCount() {
        String countText = filteredJobList.size() + " job" + (filteredJobList.size() != 1 ? "s" : "");
        tvJobCount.setText(countText);
    }
}