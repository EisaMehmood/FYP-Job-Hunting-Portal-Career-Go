package com.example.careergo.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Model.JobApplication;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentApplicationsAdapter extends RecyclerView.Adapter<StudentApplicationsAdapter.ApplicationViewHolder> {

    private List<JobApplication> applicationList;
    private Map<String, String> jobTitlesMap;
    private Map<String, String> companyNamesMap;
    private OnApplicationActionListener listener;

    public interface OnApplicationActionListener {
        void onViewJobDetails(JobApplication application);
        void onViewApplicationDetails(JobApplication application);
        void onUpdateApplicationStatus(JobApplication application, String newStatus);
    }

    public StudentApplicationsAdapter(List<JobApplication> applicationList,
                                      Map<String, String> jobTitlesMap,
                                      Map<String, String> companyNamesMap,
                                      OnApplicationActionListener listener) {
        this.applicationList = applicationList;
        this.jobTitlesMap = jobTitlesMap;
        this.companyNamesMap = companyNamesMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_applications, parent, false);
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
        private TextView tvJobTitle, tvCompanyName, tvAppliedDate;
        private Chip chipStatus;
        private Button btnViewJob;
        private MaterialButton btnMoreActions;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnViewJob = itemView.findViewById(R.id.btnViewJob);

            btnMoreActions = itemView.findViewById(R.id.btnMoreActions);
        }

        public void bind(JobApplication application) {
            // Set job title and company name
            String jobTitle = jobTitlesMap.get(application.getJobId());
            String companyName = companyNamesMap.get(application.getJobId());

            tvJobTitle.setText(jobTitle != null ? jobTitle : "Unknown Job");
            tvCompanyName.setText(companyName != null ? companyName : "Unknown Company");

            // Set applied date
            setAppliedDate(application);

            // Set status chip
            updateStatusChip(application.getStatus());

            // Set click listeners
            setupClickListeners(application);
        }

        private void setAppliedDate(JobApplication application) {
            try {
                long appliedDate = 0;

                // Try different possible date fields
                if (application.getAppliedDate() != 0) {
                    appliedDate = application.getAppliedDate();
                } else if (application.getAppliedDate() != 0) {
                    appliedDate = application.getAppliedDate();
                }

                if (appliedDate > 0) {
                    // Handle both milliseconds and seconds timestamp
                    if (appliedDate < 10000000000L) {
                        // If it's in seconds, convert to milliseconds
                        appliedDate = appliedDate * 1000;
                    }

                    String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(new Date(appliedDate));
                    tvAppliedDate.setText("Applied " + formattedDate);
                } else {
                    tvAppliedDate.setText("Applied recently");
                }
            } catch (Exception e) {
                // Fallback if date parsing fails
                tvAppliedDate.setText("Applied recently");
            }
        }

        private void setupClickListeners(JobApplication application) {
            btnViewJob.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewJobDetails(application);
                }
            });

            btnMoreActions.setOnClickListener(v -> showMoreOptions(application));

        }

        private void updateStatusChip(String status) {
            if (status == null) status = "pending";

            chipStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            switch (status) {
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

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
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
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
            builder.setTitle("Accept Application");
            builder.setMessage("Are you sure you want to accept this application?");
            builder.setPositiveButton("Accept", (dialog, which) -> {
                updateApplicationStatus(application, "accepted");
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showRejectConfirmation(JobApplication application) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
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