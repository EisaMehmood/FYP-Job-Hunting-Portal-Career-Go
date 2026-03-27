package com.example.careergo.Adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
import com.example.careergo.User.UserJobView;
import com.google.android.material.chip.Chip;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecentJobsAdapter extends RecyclerView.Adapter<RecentJobsAdapter.JobViewHolder> {

    private List<Job> jobList;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
        void onJobApplyClick(Job job);
    }

    public RecentJobsAdapter(List<Job> jobList, OnJobClickListener listener) {
        this.jobList = jobList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.job_detail_card, parent, false);
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
        private CircleImageView ivCompanyLogo;
        private TextView tvJobRole, tvCompanyNameLocation, tvSalary;
        private Chip chipDesignation, chipWorkType, btnApply;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            ivCompanyLogo = itemView.findViewById(R.id.ivCompanyLogo);
            tvJobRole = itemView.findViewById(R.id.tvJobRole);
            tvCompanyNameLocation = itemView.findViewById(R.id.tvCompanyNameLocation);
            tvSalary = itemView.findViewById(R.id.tvSalary);
            chipDesignation = itemView.findViewById(R.id.chipDesignation);
            chipWorkType = itemView.findViewById(R.id.chipWorkType);
            btnApply = itemView.findViewById(R.id.btnApply);
        }

        public void bind(Job job, OnJobClickListener listener) {
            // Set job data
            tvJobRole.setText(job.getJobTitle());

            // Set company name and location
            String companyLocation = job.getCompanyName() + " • " + job.getCity();
            tvCompanyNameLocation.setText(companyLocation);

            // Set salary
            tvSalary.setText("PKR : " + job.getSalary() + "/Mo");

            // Set designation chip
            if (job.getDesignation() != null && !job.getDesignation().isEmpty()) {
                chipDesignation.setText(job.getDesignation());
                chipDesignation.setVisibility(View.VISIBLE);
            } else {
                chipDesignation.setVisibility(View.GONE);
            }

            // Set work type chip
            if (job.getWorkType() != null && !job.getWorkType().isEmpty()) {
                chipWorkType.setText(job.getWorkType());
                chipWorkType.setVisibility(View.VISIBLE);
            } else {
                chipWorkType.setVisibility(View.GONE);
            }

            // Load company logo
            if (job.getCompanyImageUrl() != null && !job.getCompanyImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(job.getCompanyImageUrl())
                        .placeholder(R.drawable.ic_apple_logo)
                        .error(R.drawable.ic_apple_logo)
                        .into(ivCompanyLogo);
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_apple_logo);
            }

            // Item click - open job details
            itemView.setOnClickListener(v -> listener.onJobClick(job));

            // Apply button click
            btnApply.setOnClickListener(v -> listener.onJobApplyClick(job));
        }
    }
}