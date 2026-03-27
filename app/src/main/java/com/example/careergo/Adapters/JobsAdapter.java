package com.example.careergo.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Admin.AdminJobDetailsActivity;
import com.example.careergo.Model.Job;
import com.example.careergo.R;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.JobViewHolder> {

    private List<Job> jobList;
    private DatabaseReference mDatabase;
    private Context context;
    private OnJobStatusChangeListener statusChangeListener;

    public interface OnJobStatusChangeListener {
        void onJobStatusChanged(Job job);
    }

    public JobsAdapter(List<Job> jobList, Context context) {
        this.jobList = jobList;
        this.context = context;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public JobsAdapter(List<Job> jobList, Context context, OnJobStatusChangeListener listener) {
        this.jobList = jobList;
        this.context = context;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.statusChangeListener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);
        holder.bind(job);
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    public void updateList(List<Job> newList) {
        jobList = newList;
        notifyDataSetChanged();
    }

    class JobViewHolder extends RecyclerView.ViewHolder {
        private TextView tvJobTitle, tvCompanyName, tvLocation, tvPostedDate;
        private Chip chipStatus, chipWorkType, chipSalary, chipApplications;
        private MaterialButton btnViewDetails, btnToggleStatus;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPostedDate = itemView.findViewById(R.id.tvPostedDate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipWorkType = itemView.findViewById(R.id.chipWorkType);
            chipSalary = itemView.findViewById(R.id.chipSalary);
            chipApplications = itemView.findViewById(R.id.chipApplications);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
        }

        public void bind(Job job) {
            // Set basic job information
            tvJobTitle.setText(job.getJobTitle() != null ? job.getJobTitle() : "No Title");
            tvCompanyName.setText(job.getCompanyName() != null ? job.getCompanyName() : "No Company");
            tvLocation.setText(job.getCity() != null ? job.getCity() : "Location not specified");

            // Format timestamp to readable date
            if (job.getTimestamp() != 0) {
                String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(new Date(job.getTimestamp()));
                tvPostedDate.setText("Posted " + formattedDate);
            } else {
                tvPostedDate.setText("Posted date not available");
            }

            // Set status chip
            updateStatusChip(job.isActive());

            // Set work type chip
            if (job.getWorkType() != null && !job.getWorkType().isEmpty()) {
                chipWorkType.setText(job.getWorkType());
                chipWorkType.setVisibility(View.VISIBLE);
            } else {
                chipWorkType.setVisibility(View.GONE);
            }

            // Set salary chip
            if (job.getSalary() != null && !job.getSalary().isEmpty()) {
                chipSalary.setText(job.getSalary());
                chipSalary.setVisibility(View.VISIBLE);
            } else {
                chipSalary.setVisibility(View.GONE);
            }

            // Fetch and set application count
            fetchApplicationCount(job.getJobId());

            // Set toggle button text
            btnToggleStatus.setText(job.isActive() ? "Deactivate" : "Activate");

            // Set click listeners
            btnToggleStatus.setOnClickListener(v -> showToggleStatusConfirmation(job));
            btnViewDetails.setOnClickListener(v -> viewJobDetails(job));

            // Add click listener to entire item
            itemView.setOnClickListener(v -> viewJobDetails(job));
        }

        private void updateStatusChip(boolean isActive) {
            if (isActive) {
                chipStatus.setText("Active");
                chipStatus.setChipBackgroundColorResource(R.color.light_green);
            } else {
                chipStatus.setText("Inactive");
                chipStatus.setChipBackgroundColorResource(R.color.light_gray);
            }
        }

        private void fetchApplicationCount(String jobId) {
            if (jobId == null) {
                chipApplications.setText("0 Apps");
                return;
            }

            mDatabase.child("applications").orderByChild("jobId").equalTo(jobId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            long applicationCount = snapshot.getChildrenCount();
                            chipApplications.setText(applicationCount + " App" + (applicationCount != 1 ? "s" : ""));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            chipApplications.setText("0 Apps");
                        }
                    });
        }

        private void showToggleStatusConfirmation(Job job) {
            String message = job.isActive() ?
                    "Are you sure you want to deactivate this job? It will no longer be visible to job seekers." :
                    "Are you sure you want to activate this job? It will become visible to job seekers.";

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Confirm Status Change");
            builder.setMessage(message);
            builder.setPositiveButton("Yes", (dialog, which) -> {
                toggleJobStatus(job);
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void toggleJobStatus(Job job) {
            boolean newStatus = !job.isActive();

            mDatabase.child("jobs").child(job.getJobId()).child("active")
                    .setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Update local job object
                        job.setActive(newStatus);

                        // Update UI
                        updateStatusChip(newStatus);
                        btnToggleStatus.setText(newStatus ? "Deactivate" : "Activate");

                        // Show success message
                        String message = newStatus ? "Job activated successfully" : "Job deactivated successfully";
                        Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();

                        // Notify adapter
                        notifyItemChanged(getAdapterPosition());

                        // Notify listener if exists
                        if (statusChangeListener != null) {
                            statusChangeListener.onJobStatusChanged(job);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "Failed to update job status", Toast.LENGTH_SHORT).show();
                    });
        }

        private void viewJobDetails(Job job) {
            Intent intent = new Intent(itemView.getContext(), AdminJobDetailsActivity.class);
            intent.putExtra("jobId", job.getJobId());
            intent.putExtra("jobTitle", job.getJobTitle());
            intent.putExtra("employerId", job.getEmployerId());
            itemView.getContext().startActivity(intent);
        }
    }
}