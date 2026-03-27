package com.example.careergo.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.careergo.Admin.AdminEmployerJobsActivity;
import com.example.careergo.Admin.AdminEmployerProfileActivity;
import com.example.careergo.Model.Employer;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class EmployersAdapter extends RecyclerView.Adapter<EmployersAdapter.EmployerViewHolder> {

    private List<Employer> employerList;
    private DatabaseReference mDatabase;
    private Context context;

    public EmployersAdapter(List<Employer> employerList, Context context) {
        this.employerList = employerList;
        this.context = context;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public EmployerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employer, parent, false);
        return new EmployerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployerViewHolder holder, int position) {
        Employer employer = employerList.get(position);
        holder.bind(employer);
    }

    @Override
    public int getItemCount() {
        return employerList.size();
    }

    public void updateList(List<Employer> newList) {
        employerList = newList;
        notifyDataSetChanged();
    }

    class EmployerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCompanyName, tvContactPerson, tvIndustry;
        private Chip chipStatus, chipJobs;
        private ImageButton ibMore;
        private ImageView ivCompanyLogo;

        public EmployerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvContactPerson = itemView.findViewById(R.id.tvContactPerson);
            tvIndustry = itemView.findViewById(R.id.tvIndustry);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipJobs = itemView.findViewById(R.id.chipJobs);
            ibMore = itemView.findViewById(R.id.ibMore);
            ivCompanyLogo = itemView.findViewById(R.id.ivCompanyLogo);
        }

        public void bind(Employer employer) {
            // Set company name or individual name if company name not available
            if (employer.getCompanyName() != null && !employer.getCompanyName().isEmpty()) {
                tvCompanyName.setText(employer.getCompanyName());
            } else {
                tvCompanyName.setText(employer.getFirstName() + " " + employer.getLastName());
            }

            String contactPerson = employer.getFirstName() + " " + employer.getLastName();
            tvContactPerson.setText(contactPerson);

            if (employer.getIndustry() != null && !employer.getIndustry().isEmpty()) {
                tvIndustry.setText(employer.getIndustry());
            } else {
                tvIndustry.setText("Industry not specified");
            }

            // Load profile image
            loadProfileImage(employer.getId());

            // Set status chip
            updateStatusChip(employer.isApproved());

            // Fetch job count
            fetchJobCount(employer.getId());

            ibMore.setOnClickListener(v -> showOptionsMenu(employer));

            // Add click listener to the entire item
            itemView.setOnClickListener(v -> {
                // View Profile when item is clicked
                viewEmployerProfile(employer);
            });
        }

        private void loadProfileImage(String employerId) {
            mDatabase.child("users").child(employerId).child("profileImageUrl")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String imageUrl = dataSnapshot.getValue(String.class);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    // Load image with Glide
                                    Glide.with(context)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_company_placeholder) // Create this drawable
                                            .error(R.drawable.ic_company_placeholder)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(ivCompanyLogo);
                                } else {
                                    // Use default image if URL is empty
                                    ivCompanyLogo.setImageResource(R.drawable.ic_company_placeholder);
                                }
                            } else {
                                // Use default image if no profile image URL found
                                ivCompanyLogo.setImageResource(R.drawable.ic_company_placeholder);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Use default image on error
                            ivCompanyLogo.setImageResource(R.drawable.ic_company_placeholder);
                        }
                    });
        }

        private void updateStatusChip(boolean isApproved) {
            if (isApproved) {
                chipStatus.setText("Verified");
                chipStatus.setChipBackgroundColorResource(R.color.light_green);
            } else {
                chipStatus.setText("Pending");
                chipStatus.setChipBackgroundColorResource(R.color.light_orange);
            }
        }

        private void fetchJobCount(String employerId) {
            mDatabase.child("jobs").orderByChild("employerId").equalTo(employerId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            long jobCount = snapshot.getChildrenCount();
                            chipJobs.setText(jobCount + " Jobs");
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            chipJobs.setText("0 Jobs");
                        }
                    });
        }

        private void showOptionsMenu(Employer employer) {
            String verifyText = employer.isApproved() ? "Unverify Company" : "Verify Company";
            String[] options = {"View Profile", "View Jobs", verifyText, "Delete Employer"};

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
            builder.setTitle("Employer Options");
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        viewEmployerProfile(employer);
                        break;
                    case 1:
                        viewEmployerJobs(employer);
                        break;
                    case 2:
                        toggleEmployerVerification(employer);
                        break;
                    case 3:
                        showDeleteConfirmation(employer);
                        break;
                }
            });
            builder.show();
        }

        private void viewEmployerProfile(Employer employer) {
            Intent intent = new Intent(itemView.getContext(), AdminEmployerProfileActivity.class);
            intent.putExtra("employerId", employer.getId());
            intent.putExtra("employerName", employer.getCompanyName() != null ?
                    employer.getCompanyName() : employer.getFirstName() + " " + employer.getLastName());
            itemView.getContext().startActivity(intent);
        }

        private void viewEmployerJobs(Employer employer) {
            Intent intent = new Intent(itemView.getContext(), AdminEmployerJobsActivity.class);
            intent.putExtra("employerId", employer.getId());
            intent.putExtra("employerName", employer.getCompanyName() != null ?
                    employer.getCompanyName() : employer.getFirstName() + " " + employer.getLastName());
            itemView.getContext().startActivity(intent);
        }

        private void toggleEmployerVerification(Employer employer) {
            boolean newApprovalStatus = !employer.isApproved();

            mDatabase.child("users").child(employer.getId()).child("approved")
                    .setValue(newApprovalStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Update local list and UI
                        employer.setApproved(newApprovalStatus);
                        updateStatusChip(newApprovalStatus);

                        String message = newApprovalStatus ?
                                "Employer verified successfully" : "Employer unverified successfully";
                        Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();

                        // Notify adapter to refresh the specific item
                        notifyItemChanged(getAdapterPosition());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(),
                                "Failed to update verification status", Toast.LENGTH_SHORT).show();
                    });
        }

        private void showDeleteConfirmation(Employer employer) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
            builder.setTitle("Delete Employer");
            builder.setMessage("Are you sure you want to delete this employer? This action cannot be undone and will also delete all associated jobs.");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                deleteEmployer(employer);
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void deleteEmployer(Employer employer) {
            String employerId = employer.getId();

            // First, delete all jobs associated with this employer
            mDatabase.child("jobs").orderByChild("employerId").equalTo(employerId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot jobSnapshot : snapshot.getChildren()) {
                                jobSnapshot.getRef().removeValue();
                            }

                            // Then delete the employer from users
                            mDatabase.child("users").child(employerId).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // Remove from local list and update UI
                                        int position = getAdapterPosition();
                                        if (position != RecyclerView.NO_POSITION) {
                                            employerList.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        Toast.makeText(itemView.getContext(),
                                                "Employer deleted successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(itemView.getContext(),
                                                "Failed to delete employer", Toast.LENGTH_SHORT).show();
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(itemView.getContext(),
                                    "Failed to delete employer jobs", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}