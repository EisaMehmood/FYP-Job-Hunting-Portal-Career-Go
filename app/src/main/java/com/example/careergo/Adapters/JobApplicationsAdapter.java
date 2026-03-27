package com.example.careergo.Adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.JobApplication;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JobApplicationsAdapter extends RecyclerView.Adapter<JobApplicationsAdapter.ApplicationViewHolder> {

    private List<JobApplication> applicationList;
    private Map<String, User> studentDataMap;
    private OnApplicationActionListener listener;

    public interface OnApplicationActionListener {
        void onViewCandidateProfile(JobApplication application);
        void onUpdateApplicationStatus(JobApplication application, String newStatus);
        void onViewResume(JobApplication application);
    }

    public JobApplicationsAdapter(List<JobApplication> applicationList, Map<String, User> studentDataMap, OnApplicationActionListener listener) {
        this.applicationList = applicationList;
        this.studentDataMap = studentDataMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_jobs_applications, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        JobApplication application = applicationList.get(position);
        holder.bind(application);
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    public void updateList(List<JobApplication> newList) {
        applicationList = newList;
        notifyDataSetChanged();
    }

    class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCandidateAvatar;
        private TextView tvCandidateName, tvCandidateEmail, tvAppliedDate, tvCoverLetter;
        private Chip chipStatus, chipExperience, chipEducation;
        private Button btnViewProfile, btnViewResume, btnMoreActions;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCandidateAvatar = itemView.findViewById(R.id.ivCandidateAvatar);
            tvCandidateName = itemView.findViewById(R.id.tvCandidateName);
            tvCandidateEmail = itemView.findViewById(R.id.tvCandidateEmail);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipExperience = itemView.findViewById(R.id.chipExperience);
            chipEducation = itemView.findViewById(R.id.chipEducation);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnViewResume = itemView.findViewById(R.id.btnViewResume);
            btnMoreActions = itemView.findViewById(R.id.btnMoreActions);
        }

        public void bind(JobApplication application) {
            // Load student details from the map
            User student = studentDataMap.get(application.getStudentId());

            if (student != null) {
                // Set candidate name and email
                String fullName = (student.getFirstName() != null ? student.getFirstName() : "") + " " +
                        (student.getLastName() != null ? student.getLastName() : "");
                tvCandidateName.setText(fullName.trim().isEmpty() ? "Unknown" : fullName.trim());
                tvCandidateEmail.setText(student.getEmail() != null ? student.getEmail() : "No email");

                // Load profile image
                if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(student.getProfileImageUrl())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(ivCandidateAvatar);
                } else {
                    ivCandidateAvatar.setImageResource(R.drawable.ic_person);
                }



                // Update resume button visibility based on resume availability
                updateResumeButtonVisibility(student);

            } else {
                // Student data not loaded, show basic info
                tvCandidateName.setText("Loading...");
                tvCandidateEmail.setText("Loading...");
                tvCoverLetter.setVisibility(View.GONE);
                ivCandidateAvatar.setImageResource(R.drawable.ic_person);

                // Hide resume button if student data not loaded
                btnViewResume.setVisibility(View.GONE);
            }

            // Hide experience and education chips since they're not in your data structure
            chipExperience.setVisibility(View.GONE);
            chipEducation.setVisibility(View.GONE);

            // Set applied date
            long appliedDate = application.getAppliedDate();
            if (appliedDate != 0) {
                String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(new Date(appliedDate));
                tvAppliedDate.setText("Applied " + formattedDate);
            } else {
                tvAppliedDate.setText("Applied recently");
            }

            // Set status chip
            updateStatusChip(application.getStatus());

            setupClickListeners(application, student);
        }

        private void updateResumeButtonVisibility(User student) {
            if (student != null) {
                boolean hasResume = (student.getResumeUrl() != null && !student.getResumeUrl().isEmpty()) ||
                        (student.getCvBase64() != null && !student.getCvBase64().isEmpty());

                btnViewResume.setVisibility(hasResume ? View.VISIBLE : View.GONE);

                // Update button text based on resume type
                if (hasResume) {
                    if (student.getResumeUrl() != null && !student.getResumeUrl().isEmpty()) {
                        btnViewResume.setText("View Resume");
                    } else if (student.getCvBase64() != null && !student.getCvBase64().isEmpty()) {
                        btnViewResume.setText("View CV");
                    }
                }
            } else {
                btnViewResume.setVisibility(View.GONE);
            }
        }

        private void setupClickListeners(JobApplication application, User student) {
            // View Profile button
            btnViewProfile.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewCandidateProfile(application);
                }
            });

            // View Resume button - DIRECT RESUME VIEWING
            btnViewResume.setOnClickListener(v -> {
                // Check if student data is available and has resume
                if (student != null) {
                    boolean hasResume = (student.getResumeUrl() != null && !student.getResumeUrl().isEmpty()) ||
                            (student.getCvBase64() != null && !student.getCvBase64().isEmpty());

                    if (hasResume) {
                        // Direct resume viewing without going through listener
                        openResumeDirectly(student);
                    } else {
                        showNoResumeDialog();
                    }
                } else {
                    showNoResumeDialog();
                }
            });

            // More Actions button
            btnMoreActions.setOnClickListener(v -> showMoreOptions(application));

            // Add click listener to entire item for profile view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewCandidateProfile(application);
                }
            });
        }

        private void openResumeDirectly(User student) {
            try {
                String resumeUrl = student.getResumeUrl();

                if (resumeUrl != null && !resumeUrl.isEmpty()) {
                    // Open PDF URL directly
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(resumeUrl), "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    // Verify that there's an app to handle PDFs
                    if (intent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                        itemView.getContext().startActivity(intent);
                    } else {
                        // No PDF viewer app found, try opening in browser
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(resumeUrl));
                        itemView.getContext().startActivity(browserIntent);
                    }
                } else if (student.getCvBase64() != null && !student.getCvBase64().isEmpty()) {
                    // Handle base64 CV
                    showBase64CvDialog(student.getCvBase64());
                } else {
                    showNoResumeDialog();
                }
            } catch (Exception e) {
                Toast.makeText(itemView.getContext(), "Error opening resume: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private void showBase64CvDialog(String cvBase64) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("CV Available");
            builder.setMessage("This candidate's CV is available in base64 format. Would you like to view it?");
            builder.setPositiveButton("View", (dialog, which) -> {
                // You can implement base64 PDF viewing here
                // For now, show a message
                Toast.makeText(itemView.getContext(), "Base64 CV viewing feature coming soon", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showNoResumeDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("No Resume Available");
            builder.setMessage("This candidate has not uploaded a resume yet.");
            builder.setPositiveButton("OK", null);
            builder.show();
        }

        private void updateStatusChip(String status) {
            if (status == null) status = "pending";

            chipStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            switch (status.toLowerCase()) {
                case "pending":
                    chipStatus.setChipBackgroundColorResource(R.color.light_orange);
                    break;
                case "reviewed":
                    chipStatus.setChipBackgroundColorResource(R.color.light_blue);
                    break;
                case "accepted":
                    chipStatus.setChipBackgroundColorResource(R.color.light_green);
                    break;
                case "rejected":
                    chipStatus.setChipBackgroundColorResource(R.color.light_red);
                    break;
                default:
                    chipStatus.setChipBackgroundColorResource(R.color.light_gray);
            }
        }

        private void showMoreOptions(JobApplication application) {
            String[] options = {"Mark as Reviewed", "Accept Application", "Reject Application"};

            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Application Actions");
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        updateApplicationStatus(application, "reviewed");
                        break;
                    case 1:
                        showAcceptConfirmation(application);
                        break;
                    case 2:
                        showRejectConfirmation(application);
                        break;
                }
            });
            builder.show();
        }

        private void updateApplicationStatus(JobApplication application, String newStatus) {
            if (listener != null) {
                listener.onUpdateApplicationStatus(application, newStatus);
            }
        }

        private void showAcceptConfirmation(JobApplication application) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Accept Application");
            builder.setMessage("Are you sure you want to accept this application?");
            builder.setPositiveButton("Accept", (dialog, which) -> {
                updateApplicationStatus(application, "accepted");
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showRejectConfirmation(JobApplication application) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Reject Application");
            builder.setMessage("Are you sure you want to reject this application?");
            builder.setPositiveButton("Reject", (dialog, which) -> {
                updateApplicationStatus(application, "rejected");
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }
}