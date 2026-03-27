package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.careergo.Model.JobApplication;
import com.example.careergo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployerApplicantsActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private ImageView ivBack;
    private TextView tvTitle, tvNoApplications;
    private RecyclerView recyclerViewApplications;
    private TextInputEditText etSearch;

    // Adapter and List
    private ApplicationsAdapter applicationsAdapter;
    private List<JobApplication> applicationsList;
    private List<JobApplication> filteredApplicationsList; // For search results
    private Map<String, String> jobTitlesMap;
    private Map<String, String> studentProfileImagesMap; // Store student profile images

    // Data
    private String currentEmployerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_applicants); // New layout with search

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupSearchListener();
        checkAuthentication();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvNoApplications = findViewById(R.id.tvNoApplications);
        recyclerViewApplications = findViewById(R.id.recyclerViewApplications);
        etSearch = findViewById(R.id.etSearch);

        tvTitle.setText("All Applicants");

        jobTitlesMap = new HashMap<>();
        studentProfileImagesMap = new HashMap<>();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        applicationsList = new ArrayList<>();
        filteredApplicationsList = new ArrayList<>();
        applicationsAdapter = new ApplicationsAdapter(filteredApplicationsList);
        recyclerViewApplications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewApplications.setAdapter(applicationsAdapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApplications(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterApplications(String searchText) {
        filteredApplicationsList.clear();

        if (searchText.isEmpty()) {
            // Show all applications when search is empty
            filteredApplicationsList.addAll(applicationsList);
        } else {
            // Filter applications based on search text
            String lowerCaseQuery = searchText.toLowerCase();
            for (JobApplication application : applicationsList) {
                // Search in student name, email, phone, job title, and status
                if (application.getStudentName().toLowerCase().contains(lowerCaseQuery) ||
                        application.getStudentEmail().toLowerCase().contains(lowerCaseQuery) ||
                        (application.getStudentPhone() != null && application.getStudentPhone().toLowerCase().contains(lowerCaseQuery)) ||
                        (application.getJobTitle() != null && application.getJobTitle().toLowerCase().contains(lowerCaseQuery)) ||
                        (application.getStatus() != null && application.getStatus().toLowerCase().contains(lowerCaseQuery))) {
                    filteredApplicationsList.add(application);
                }
            }
        }

        applicationsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void checkAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentEmployerId = currentUser.getUid();
        loadAllJobTitles();
    }

    private void loadAllJobTitles() {
        mDatabase.child("jobs")
                .orderByChild("employerId")
                .equalTo(currentEmployerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot jobsSnapshot) {
                        if (jobsSnapshot.exists()) {
                            for (DataSnapshot jobSnapshot : jobsSnapshot.getChildren()) {
                                String jobId = jobSnapshot.getKey();
                                String jobTitle = jobSnapshot.child("jobTitle").getValue(String.class);
                                if (jobTitle != null) {
                                    jobTitlesMap.put(jobId, jobTitle);
                                }
                            }
                            loadAllApplicants();
                        } else {
                            showNoApplications();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(EmployerApplicantsActivity.this,
                                "Failed to load jobs: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showNoApplications();
                    }
                });
    }

    private void loadAllApplicants() {
        mDatabase.child("applications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot applicationsSnapshot) {
                        applicationsList.clear();

                        if (applicationsSnapshot.exists()) {
                            for (DataSnapshot applicationSnapshot : applicationsSnapshot.getChildren()) {
                                JobApplication application = applicationSnapshot.getValue(JobApplication.class);
                                if (application != null && jobTitlesMap.containsKey(application.getJobId())) {
                                    application.setApplicationId(applicationSnapshot.getKey());

                                    // Set job title from our map
                                    String jobTitle = jobTitlesMap.get(application.getJobId());
                                    application.setJobTitle(jobTitle);

                                    applicationsList.add(application);

                                    // Load student profile image for this application
                                    loadStudentProfileImage(application.getStudentId());
                                }
                            }

                            // Apply current search filter (if any)
                            filterApplications(etSearch.getText().toString());

                            if (applicationsList.isEmpty()) {
                                showNoApplications();
                            }
                        } else {
                            showNoApplications();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(EmployerApplicantsActivity.this,
                                "Failed to load applications",
                                Toast.LENGTH_SHORT).show();
                        showNoApplications();
                    }
                });
    }

    private void loadStudentProfileImage(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;

        // Check if we already loaded this student's image
        if (studentProfileImagesMap.containsKey(studentId)) {
            return;
        }

        // FIXED: Load from the correct path - profileImageUrl in users node
        mDatabase.child("users").child(studentId).child("profileImageUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String profileImageUrl = dataSnapshot.getValue(String.class);
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                studentProfileImagesMap.put(studentId, profileImageUrl);
                                // Notify adapter that images are available
                                applicationsAdapter.notifyDataSetChanged();
                            } else {
                                // If profileImageUrl exists but is empty, store null
                                studentProfileImagesMap.put(studentId, null);
                            }
                        } else {
                            // If no profileImageUrl found, store null
                            studentProfileImagesMap.put(studentId, null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Silent fail - we'll use default image
                        studentProfileImagesMap.put(studentId, null);
                    }
                });
    }

    private void updateEmptyState() {
        if (filteredApplicationsList.isEmpty()) {
            recyclerViewApplications.setVisibility(View.GONE);
            tvNoApplications.setVisibility(View.VISIBLE);

            String searchText = etSearch.getText().toString();
            if (!searchText.isEmpty()) {
                tvNoApplications.setText("No applicants found for \"" + searchText + "\"");
            } else {
                tvNoApplications.setText("No applicants found for your jobs");
            }
        } else {
            recyclerViewApplications.setVisibility(View.VISIBLE);
            tvNoApplications.setVisibility(View.GONE);
        }
    }

    private void showNoApplications() {
        recyclerViewApplications.setVisibility(View.GONE);
        tvNoApplications.setVisibility(View.VISIBLE);
        tvNoApplications.setText("No applicants found for your jobs");
    }

    // Adapter Class
    private class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder> {

        private List<JobApplication> applicationsList;

        public ApplicationsAdapter(List<JobApplication> applicationsList) {
            this.applicationsList = applicationsList;
        }

        @NonNull
        @Override
        public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_application_with_job_title, parent, false);
            return new ApplicationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
            JobApplication application = applicationsList.get(position);
            holder.bind(application);
        }

        @Override
        public int getItemCount() {
            return applicationsList.size();
        }

        public class ApplicationViewHolder extends RecyclerView.ViewHolder {

            private TextView tvStudentName, tvStudentEmail, tvStudentPhone, tvAppliedDate, tvStatus, tvJobTitle;
            private ImageView ivStudentProfile;
            private Button btnUpdateStatus;

            public ApplicationViewHolder(@NonNull View itemView) {
                super(itemView);

                tvStudentName = itemView.findViewById(R.id.tvStudentName);
                tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
                tvStudentPhone = itemView.findViewById(R.id.tvStudentPhone);
                tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                ivStudentProfile = itemView.findViewById(R.id.ivStudentProfile);
                btnUpdateStatus = itemView.findViewById(R.id.btnupdatestatus);
                tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            }

            public void bind(JobApplication application) {
                // Set student information
                tvStudentName.setText(application.getStudentName());
                tvStudentEmail.setText(application.getStudentEmail());

                // Set job title
                if (application.getJobTitle() != null && !application.getJobTitle().isEmpty()) {
                    tvJobTitle.setText("Applied for: " + application.getJobTitle());
                    tvJobTitle.setVisibility(View.VISIBLE);
                } else {
                    tvJobTitle.setText("Applied for: Unknown Job");
                    tvJobTitle.setVisibility(View.VISIBLE);
                }

                // Set phone number
                if (application.getStudentPhone() != null && !application.getStudentPhone().isEmpty()) {
                    tvStudentPhone.setText("Phone: " + application.getStudentPhone());
                } else {
                    tvStudentPhone.setText("Phone not provided");
                }

                // Set applied date
                if (application.getAppliedDate() > 0) {
                    String formattedDate = formatDate(application.getAppliedDate());
                    tvAppliedDate.setText("Applied: " + formattedDate);
                } else {
                    tvAppliedDate.setText("Applied: Recently");
                }

                // Set status with color coding
                setStatus(application.getStatus());

                // Load student profile image
                loadStudentProfileImage(application);

                // Set click listener to view application details
                itemView.setOnClickListener(v -> {
                    openStudentDetails(application);
                });

                // Set click listener for status update button
                btnUpdateStatus.setOnClickListener(v -> {
                    showStatusUpdateDialog(application);
                });
            }

            private void loadStudentProfileImage(JobApplication application) {
                String studentId = application.getStudentId();

                if (studentProfileImagesMap.containsKey(studentId)) {
                    String imageUrl = studentProfileImagesMap.get(studentId);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Load image using Glide with circle transformation
                        Glide.with(itemView.getContext())
                                .load(imageUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .placeholder(R.drawable.ic_image_picker) // Default placeholder
                                .error(R.drawable.ic_image_picker) // Error placeholder
                                .into(ivStudentProfile);
                    } else {
                        // Use default image if no image URL
                        ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                    }
                } else {
                    // Use default image while loading
                    ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                }
            }

            private void setStatus(String status) {
                if (status == null) {
                    status = "pending";
                }

                tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

                switch (status.toLowerCase()) {
                    case "approved":
                        tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                        tvStatus.setTextColor(getResources().getColor(R.color.green));
                        break;
                    case "rejected":
                        tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                        tvStatus.setTextColor(getResources().getColor(R.color.red));
                        break;
                    case "shortlisted":
                        tvStatus.setBackgroundResource(R.drawable.bg_status_shortlisted);
                        tvStatus.setTextColor(getResources().getColor(R.color.blue));
                        break;
                    default: // pending
                        tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                        tvStatus.setTextColor(getResources().getColor(R.color.orange));
                        break;
                }
            }

            private String formatDate(long timestamp) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }

            private void showStatusUpdateDialog(JobApplication application) {
                StatusUpdateDialog dialog = new StatusUpdateDialog(
                        itemView.getContext(),
                        application.getApplicationId(),
                        application.getStatus(),
                        new StatusUpdateDialog.OnStatusUpdateListener() {
                            @Override
                            public void onStatusUpdated(String newStatus) {
                                // Update the application status locally
                                application.setStatus(newStatus);

                                // Update the UI
                                setStatus(newStatus);

                                Toast.makeText(itemView.getContext(),
                                        "Status updated to: " + newStatus,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                dialog.show();
            }
        }
    }

    private void openStudentDetails(JobApplication application) {
        Intent intent = new Intent(this, StudentView.class);
        intent.putExtra("studentId", application.getStudentId());
        intent.putExtra("applicationId", application.getApplicationId());
        startActivity(intent);
    }
}