package com.example.careergo.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.JobApplication;
import com.example.careergo.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppliedJobsAdapter extends RecyclerView.Adapter<AppliedJobsAdapter.AppliedJobViewHolder> {

    private List<JobApplication> appliedJobsList;
    private OnAppliedJobClickListener listener;

    public interface OnAppliedJobClickListener {
        void onAppliedJobClick(JobApplication application);
        void onStatusClick(JobApplication application);
    }

    public AppliedJobsAdapter(List<JobApplication> appliedJobsList, OnAppliedJobClickListener listener) {
        this.appliedJobsList = appliedJobsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppliedJobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_applied_job, parent, false);
        return new AppliedJobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppliedJobViewHolder holder, int position) {
        JobApplication application = appliedJobsList.get(position);
        holder.bind(application, listener);
    }

    @Override
    public int getItemCount() {
        return appliedJobsList.size();
    }

    public void updateList(List<JobApplication> newList) {
        appliedJobsList = newList;
        notifyDataSetChanged();
    }

    static class AppliedJobViewHolder extends RecyclerView.ViewHolder {
        private CardView cvJobCard;
        private ImageView ivCompanyLogo;
        private TextView tvJobTitle, tvCompanyName, tvLocation, tvAppliedDate, tvStatus, tvSalary;

        public AppliedJobViewHolder(@NonNull View itemView) {
            super(itemView);
            cvJobCard = itemView.findViewById(R.id.cvJobCard);
            ivCompanyLogo = itemView.findViewById(R.id.ivCompanyLogo);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSalary = itemView.findViewById(R.id.tvSalary);
        }

        public void bind(JobApplication application, OnAppliedJobClickListener listener) {
            // Set company logo
            if (application.getCompanyImageUrl() != null && !application.getCompanyImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(application.getCompanyImageUrl())
                        .placeholder(R.drawable.ic_company_placeholder)
                        .error(R.drawable.ic_company_placeholder)
                        .into(ivCompanyLogo);
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_company_placeholder);
            }

            // Set job title
            tvJobTitle.setText(application.getJobTitle() != null ? application.getJobTitle() : "Job Title");

            // Set company name
            tvCompanyName.setText(application.getCompanyName() != null ? application.getCompanyName() : "Company");

            // Set location
            tvLocation.setText(application.getLocation() != null ? application.getLocation() : "Location not specified");

            // Set salary
            if (application.getSalary() != null && !application.getSalary().isEmpty()) {
                tvSalary.setText("PKR : " + application.getSalary() + "/month");
                tvSalary.setVisibility(View.VISIBLE);
            } else {
                tvSalary.setVisibility(View.GONE);
            }

            // Set applied date
            if (application.getAppliedDate() > 0) {
                String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(new Date(application.getAppliedDate()));
                tvAppliedDate.setText("Applied on " + date);
            } else {
                tvAppliedDate.setText("Applied recently");
            }

            // Set status with color coding
            setStatusUI(application.getStatus());

            // Click listeners
            cvJobCard.setOnClickListener(v -> listener.onAppliedJobClick(application));
            tvStatus.setOnClickListener(v -> listener.onStatusClick(application));
        }

        private void setStatusUI(String status) {
            if (status == null) {
                status = "pending";
            }

            switch (status.toLowerCase()) {
                case "approved":
                    tvStatus.setText("Approved");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_accepted);
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.status_accepted));
                    break;
                case "shortlisted":
                    tvStatus.setText("Shortlisted");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_shortlisted);
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.status_shortlisted));
                    break;
                case "rejected":
                    tvStatus.setText("Not Selected");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.status_rejected));
                    break;
                case "pending":
                default:
                    tvStatus.setText("Under Review");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.status_pending));
                    break;
            }
        }
    }
}