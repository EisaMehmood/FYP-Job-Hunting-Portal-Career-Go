package com.example.careergo.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private List<Job> jobList;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
        void onJobDeleteClick(Job job);
        void onJobMoreClick(Job job, View anchorView);
    }

    public JobAdapter(List<Job> jobList, OnJobClickListener listener) {
        this.jobList = jobList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.job_card_layout, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);
        holder.bind(job, listener);
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    public void updateList(List<Job> newList) {
        jobList = newList;
        notifyDataSetChanged();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cvJob;
        private ImageView ivCompanyLogo;
        private TextView tvJobRole, tvCompanyName, tvSalary, tvExperience, tvApplications, tvPostedDate;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            cvJob = itemView.findViewById(R.id.cvJob);
            ivCompanyLogo = itemView.findViewById(R.id.ivCompanyLogo);
            tvJobRole = itemView.findViewById(R.id.tvJobRole);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvSalary = itemView.findViewById(R.id.tvSalary);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvApplications = itemView.findViewById(R.id.tvApplications);
            tvPostedDate = itemView.findViewById(R.id.tvPostedDate);
        }

        public void bind(Job job, OnJobClickListener listener) {
            // Set basic job data
            tvJobRole.setText(job.getJobTitle());

            // Format company name with location
            String companyInfo = job.getCompanyName() + " • " + job.getCity();
            tvCompanyName.setText(companyInfo);

            // Set salary
            if (job.getSalary() != null && !job.getSalary().isEmpty()) {
                tvSalary.setText("PKR " + job.getSalary() + "/Mo");
                tvSalary.setVisibility(View.VISIBLE);
            } else {
                tvSalary.setVisibility(View.GONE);
            }



            // Set applications count (you'll need to load this from Firebase)
            loadApplicationsCount(job.getJobId());

            // Set posted date
            if (job.getTimestamp() > 0) {
                String postedDate = getTimeAgo(job.getTimestamp());
                tvPostedDate.setText(postedDate);
            }

            // Load company logo
            if (job.getCompanyImageUrl() != null && !job.getCompanyImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(job.getCompanyImageUrl())
                        .placeholder(R.drawable.ic_company_placeholder)
                        .error(R.drawable.ic_company_placeholder)
                        .into(ivCompanyLogo);
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_company_placeholder);
            }

            // Item click - open job details
            cvJob.setOnClickListener(v -> listener.onJobClick(job));
            
        }

        private void loadApplicationsCount(String jobId) {
            // You'll need to implement this to count applications for this job
            // For now, showing placeholder
            tvApplications.setText("0 applicants");

            // Implementation would look like:

            DatabaseReference appsRef = FirebaseDatabase.getInstance().getReference("applications");
            appsRef.orderByChild("jobId").equalTo(jobId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long count = dataSnapshot.getChildrenCount();
                        tvApplications.setText(count + " applicants");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        tvApplications.setText("0 applicants");
                    }
                });

        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just now";
            }
        }
    }
}