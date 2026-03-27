package com.example.careergo.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.SavedJob;
import com.example.careergo.R;
import com.example.careergo.User.UserJobView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavedJobsAdapter extends RecyclerView.Adapter<SavedJobsAdapter.SavedJobViewHolder> {

    private Context context;
    private List<SavedJob> savedJobList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    public SavedJobsAdapter(Context context, List<SavedJob> savedJobList) {
        this.context = context;
        this.savedJobList = savedJobList;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void updateList(List<SavedJob> newList) {
        this.savedJobList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedJobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_job, parent, false);
        return new SavedJobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedJobViewHolder holder, int position) {
        SavedJob savedJob = savedJobList.get(position);
        holder.bind(savedJob);
    }

    @Override
    public int getItemCount() {
        return savedJobList.size();
    }

    public class SavedJobViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivCompanyLogo;
        private TextView tvJobTitle, tvCompanyLocation, tvSalary, tvSavedDate;
        private ChipGroup chipGroupSkills;
        private MaterialButton btnApply, btnRemove;

        public SavedJobViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCompanyLogo = itemView.findViewById(R.id.ivCompanyLogo);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvCompanyLocation = itemView.findViewById(R.id.tvCompanyLocation);
            tvSalary = itemView.findViewById(R.id.tvSalary);
            tvSavedDate = itemView.findViewById(R.id.tvSavedDate);
            chipGroupSkills = itemView.findViewById(R.id.chipGroupSkills);
            btnApply = itemView.findViewById(R.id.btnApply);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        public void bind(SavedJob savedJob) {
            if (savedJob.getJobDetails() == null) {
                // Handle case where job details are not available
                tvJobTitle.setText("Job not available");
                tvCompanyLocation.setText("Unknown company");
                tvSalary.setText("Salary not specified");
                return;
            }

            // Load company logo
            if (savedJob.getJobDetails().getCompanyImageUrl() != null &&
                    !savedJob.getJobDetails().getCompanyImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(savedJob.getJobDetails().getCompanyImageUrl())
                        .placeholder(R.drawable.ic_apple_logo)
                        .error(R.drawable.ic_apple_logo)
                        .into(ivCompanyLogo);
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_apple_logo);
            }

            // Set job title
            tvJobTitle.setText(savedJob.getJobDetails().getJobTitle());

            // Set company location
            String companyLocation = savedJob.getJobDetails().getCompanyName() + " • " +
                    savedJob.getJobDetails().getCity();
            tvCompanyLocation.setText(companyLocation);

            // Set salary
            if (savedJob.getJobDetails().getSalary() != null &&
                    !savedJob.getJobDetails().getSalary().isEmpty()) {
                tvSalary.setText("₹" + savedJob.getJobDetails().getSalary() + "/Mo");
            } else {
                tvSalary.setText("Salary not specified");
            }

            // Set saved date
            tvSavedDate.setText(getTimeAgo(savedJob.getSavedDate()));

            // Set skills chips
            setupSkillsChips(savedJob.getJobDetails().getRequiredSkills());

            // Setup button listeners
            setupButtonListeners(savedJob);
        }

        private void setupSkillsChips(List<String> skills) {
            chipGroupSkills.removeAllViews();

            if (skills != null && !skills.isEmpty()) {
                int maxSkillsToShow = 3;
                for (int i = 0; i < Math.min(skills.size(), maxSkillsToShow); i++) {
                    Chip chip = new Chip(context);
                    chip.setText(skills.get(i));
                    chip.setChipBackgroundColorResource(R.color.chip_background);
                    chip.setTextColor(context.getResources().getColor(R.color.text_color));
                    chip.setClickable(false);
                    chip.setChipStrokeWidth(1);
                    chip.setChipStrokeColorResource(R.color.chip_stroke_color);
                    chip.setTextSize(10f);
                    chipGroupSkills.addView(chip);
                }
            } else {
                Chip chip = new Chip(context);
                chip.setText("No specific skills");
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(context.getResources().getColor(R.color.text_color));
                chip.setClickable(false);
                chip.setTextSize(10f);
                chipGroupSkills.addView(chip);
            }
        }

        private void setupButtonListeners(SavedJob savedJob) {
            // Apply button - navigate to job details
            btnApply.setOnClickListener(v -> {
                Intent intent = new Intent(context, UserJobView.class);
                intent.putExtra("jobId", savedJob.getJobId());
                context.startActivity(intent);
            });

            // Remove button - remove from saved jobs
            btnRemove.setOnClickListener(v -> {
                removeSavedJob(savedJob.getSavedJobId());
            });
        }

        private void removeSavedJob(String savedJobId) {
            if (savedJobId != null && !savedJobId.isEmpty()) {
                mDatabase.child("savedJobs").child(savedJobId).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Job removed from saved", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to remove job", Toast.LENGTH_SHORT).show();
                        });
            }
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return "Saved " + days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                return "Saved " + hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return "Saved " + minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just saved";
            }
        }
    }
}