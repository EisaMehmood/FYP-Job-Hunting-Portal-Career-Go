package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
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

public class ApplicantView extends AppCompatActivity {

    // Firebase
    private DatabaseReference mDatabase;

    // UI Components
    private ImageView ivBack;
    private TextView tvTitle, tvNoApplications;
    private RecyclerView recyclerViewApplications;

    // Adapter and List
    private ApplicationsAdapter applicationsAdapter;
    private List<JobApplication> applicationsList;
    private Map<String, String> studentProfileImagesMap; // Store student profile images

    // Data
    private String jobId;
    private String jobTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicant_view);

        // Get job ID and title from intent
        jobId = getIntent().getStringExtra("jobId");
        jobTitle = getIntent().getStringExtra("jobTitle");

        if (jobId == null) {
            Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadApplications();
    }

    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvNoApplications = findViewById(R.id.tvNoApplications);
        recyclerViewApplications = findViewById(R.id.recyclerViewApplications);

        // Set title with job title
        if (jobTitle != null && !jobTitle.isEmpty()) {
            tvTitle.setText("Applications - " + jobTitle);
        } else {
            tvTitle.setText("Job Applications");
        }

        studentProfileImagesMap = new HashMap<>();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        applicationsList = new ArrayList<>();
        applicationsAdapter = new ApplicationsAdapter(applicationsList);
        recyclerViewApplications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewApplications.setAdapter(applicationsAdapter);
    }

    private void loadApplications() {
        mDatabase.child("applications")
                .orderByChild("jobId")
                .equalTo(jobId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        applicationsList.clear();

                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                JobApplication application = snapshot.getValue(JobApplication.class);
                                if (application != null) {
                                    application.setApplicationId(snapshot.getKey()); // Set the Firebase key
                                    applicationsList.add(application);

                                    // Load student profile image for this application
                                    loadStudentProfileImage(application.getStudentId());
                                }
                            }

                            if (!applicationsList.isEmpty()) {
                                showApplicationsList();
                            } else {
                                showNoApplications();
                            }
                        } else {
                            showNoApplications();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ApplicantView.this, "Failed to load applications", Toast.LENGTH_SHORT).show();
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

        // Mark as loading to avoid duplicate requests
        studentProfileImagesMap.put(studentId, "loading");

        // Load from users node - check multiple possible field names
        mDatabase.child("users").child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    String imageUrl = null;

                    // Check all common field names for profile image
                    if (userSnapshot.child("profileImageUrl").exists()) {
                        imageUrl = userSnapshot.child("profileImageUrl").getValue(String.class);
                    } else if (userSnapshot.child("profileImage").exists()) {
                        imageUrl = userSnapshot.child("profileImage").getValue(String.class);
                    } else if (userSnapshot.child("photoUrl").exists()) {
                        imageUrl = userSnapshot.child("photoUrl").getValue(String.class);
                    } else if (userSnapshot.child("imageUrl").exists()) {
                        imageUrl = userSnapshot.child("imageUrl").getValue(String.class);
                    }

                    studentProfileImagesMap.put(studentId, imageUrl != null ? imageUrl : "");
                    // Notify adapter that images are available
                    applicationsAdapter.notifyDataSetChanged();
                } else {
                    studentProfileImagesMap.put(studentId, "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                studentProfileImagesMap.put(studentId, "");
            }
        });
    }

    private void showApplicationsList() {
        recyclerViewApplications.setVisibility(View.VISIBLE);
        tvNoApplications.setVisibility(View.GONE);
        applicationsAdapter.notifyDataSetChanged();
    }

    private void showNoApplications() {
        recyclerViewApplications.setVisibility(View.GONE);
        tvNoApplications.setVisibility(View.VISIBLE);
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_application, parent, false);
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

            private TextView tvStudentName, tvStudentEmail, tvStudentPhone, tvAppliedDate, tvStatus;
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
            }

            public void bind(JobApplication application) {
                // Set student information
                tvStudentName.setText(application.getStudentName());
                tvStudentEmail.setText(application.getStudentEmail());

                if (application.getStudentPhone() != null && !application.getStudentPhone().isEmpty()) {
                    tvStudentPhone.setText(application.getStudentPhone());
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

                    // Skip if still loading
                    if ("loading".equals(imageUrl)) {
                        ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                        return;
                    }

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Load image using Glide with circle transformation
                        Glide.with(itemView.getContext())
                                .load(imageUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .placeholder(R.drawable.ic_image_picker)
                                .error(R.drawable.ic_image_picker)
                                .into(ivStudentProfile);
                    } else {
                        // Use default image if no image URL
                        ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                    }
                } else {
                    // Use default image while loading and trigger image load
                    ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                    // This will trigger the loadStudentProfileImage method in the activity
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