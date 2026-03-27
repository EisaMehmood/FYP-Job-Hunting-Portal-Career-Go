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
import com.example.careergo.Admin.StudentApplicationsActivity;
import com.example.careergo.Model.Student;
import com.example.careergo.R;
import com.example.careergo.Employer.StudentView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class StudentsAdapter extends RecyclerView.Adapter<StudentsAdapter.StudentViewHolder> {

    private List<Student> studentList;
    private Context context;
    private DatabaseReference mDatabase;

    public StudentsAdapter(List<Student> studentList, Context context) {
        this.studentList = studentList;
        this.context = context;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateList(List<Student> newList) {
        studentList = newList;
        notifyDataSetChanged();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvStudentName, tvStudentEmail, tvStudentCity;
        private Chip chipStatus, chipApplications;
        private ImageButton ibMore;
        private ImageView ivStudentProfile;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvStudentCity = itemView.findViewById(R.id.tvStudentCity);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipApplications = itemView.findViewById(R.id.chipApplications);
            ibMore = itemView.findViewById(R.id.ibMore);
            ivStudentProfile = itemView.findViewById(R.id.ivStudentProfile);
        }

        public void bind(Student student) {
            String fullName = student.getFirstName() + " " + student.getLastName();
            tvStudentName.setText(fullName);
            tvStudentEmail.setText(student.getEmail());

            // Set location with null checks
            String location = "";
            if (student.getCity() != null) {
                location = student.getCity();
            }
            if (student.getState() != null && !student.getState().isEmpty()) {
                if (!location.isEmpty()) location += ", ";
                location += student.getState();
            }
            if (location.isEmpty()) {
                location = "Location not specified";
            }
            tvStudentCity.setText(location);

            // Load profile image
            loadProfileImage(student.getId());

            // Set status chip
            updateStatusChip(student.isApproved());

            // Fetch application count
            fetchApplicationCount(student.getId());

            // Set click listeners
            ibMore.setOnClickListener(v -> showOptionsMenu(student));

            // Add click listener to entire item
            itemView.setOnClickListener(v -> viewStudentProfile(student));
        }

        private void loadProfileImage(String studentId) {
            mDatabase.child("users").child(studentId).child("profileImageUrl")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String imageUrl = dataSnapshot.getValue(String.class);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    // Load image with Glide
                                    Glide.with(context)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_image_picker) // Create this drawable
                                            .error(R.drawable.ic_image_picker)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(ivStudentProfile);
                                } else {
                                    // Use default image if URL is empty
                                    ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                                }
                            } else {
                                // Use default image if no profile image URL found
                                ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Use default image on error
                            ivStudentProfile.setImageResource(R.drawable.ic_image_picker);
                        }
                    });
        }

        private void updateStatusChip(boolean isApproved) {
            if (isApproved) {
                chipStatus.setText("Active");
                chipStatus.setChipBackgroundColorResource(R.color.light_green);
            } else {
                chipStatus.setText("Pending");
                chipStatus.setChipBackgroundColorResource(R.color.light_orange);
            }
        }

        private void fetchApplicationCount(String studentId) {
            mDatabase.child("applications").orderByChild("studentId").equalTo(studentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            long applicationCount = snapshot.getChildrenCount();
                            chipApplications.setText(applicationCount + " Application" + (applicationCount != 1 ? "s" : ""));
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            chipApplications.setText("0 Applications");
                        }
                    });
        }

        private void showOptionsMenu(Student student) {
            String statusText = student.isApproved() ? "Deactivate" : "Activate";
            String[] options = {"View Profile", "View Applications", statusText, "Delete Student"};

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
            builder.setTitle("Student Options");
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        viewStudentProfile(student);
                        break;
                    case 1:
                        viewStudentApplications(student);
                        break;
                    case 2:
                        toggleStudentStatus(student);
                        break;
                    case 3:
                        showDeleteConfirmation(student);
                        break;
                }
            });
            builder.show();
        }

        private void viewStudentProfile(Student student) {
            Intent intent = new Intent(itemView.getContext(), StudentView.class);
            intent.putExtra("studentId", student.getId());
            intent.putExtra("studentName", student.getFirstName() + " " + student.getLastName());
            itemView.getContext().startActivity(intent);
        }

        private void viewStudentApplications(Student student) {
            // Navigate to student applications activity
            Toast.makeText(itemView.getContext(), "View applications for " + student.getFirstName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(itemView.getContext(),StudentApplicationsActivity.class);
            intent.putExtra("studentId", student.getId());
            intent.putExtra("studentName", student.getFirstName() + " " + student.getLastName());
            itemView.getContext().startActivity(intent);
        }

        private void toggleStudentStatus(Student student) {
            boolean newStatus = !student.isApproved();

            mDatabase.child("users").child(student.getId()).child("approved")
                    .setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Update local student object
                        student.setApproved(newStatus);

                        // Update UI
                        updateStatusChip(newStatus);

                        String message = newStatus ? "Student activated successfully" : "Student deactivated successfully";
                        Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();

                        // Notify adapter
                        notifyItemChanged(getAdapterPosition());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "Failed to update student status", Toast.LENGTH_SHORT).show();
                    });
        }

        private void showDeleteConfirmation(Student student) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(itemView.getContext());
            builder.setTitle("Delete Student");
            builder.setMessage("Are you sure you want to delete " + student.getFirstName() + " " + student.getLastName() + "? This action cannot be undone and will also delete all their applications.");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                deleteStudent(student);
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void deleteStudent(Student student) {
            String studentId = student.getId();

            // First, delete all applications by this student
            mDatabase.child("applications").orderByChild("studentId").equalTo(studentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot applicationSnapshot : snapshot.getChildren()) {
                                applicationSnapshot.getRef().removeValue();
                            }

                            // Also delete saved jobs for this student
                            mDatabase.child("savedJobs").orderByChild("studentId").equalTo(studentId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot savedJobsSnapshot) {
                                            for (DataSnapshot savedJobSnapshot : savedJobsSnapshot.getChildren()) {
                                                savedJobSnapshot.getRef().removeValue();
                                            }

                                            // Then delete the student from users
                                            mDatabase.child("users").child(studentId).removeValue()
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Remove from local list and update UI
                                                        int position = getAdapterPosition();
                                                        if (position != RecyclerView.NO_POSITION) {
                                                            studentList.remove(position);
                                                            notifyItemRemoved(position);
                                                        }
                                                        Toast.makeText(itemView.getContext(), "Student deleted successfully", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(itemView.getContext(), "Failed to delete student", Toast.LENGTH_SHORT).show();
                                                    });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(itemView.getContext(), "Failed to delete saved jobs", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(itemView.getContext(), "Failed to delete student applications", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}