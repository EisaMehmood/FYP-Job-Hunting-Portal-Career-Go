package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentView extends AppCompatActivity {

    // Firebase
    private DatabaseReference mDatabase;

    // UI Components
    private ImageView ivPopOut;
    private CircleImageView profileImage;
    private TextView tvUsername, tvUserEmail, tvStudentId, tvMobile, tvUniversity, tvDegree, tvMajor,
            tvGpa, tvGraduationYear, tvExperience, tvLinkedIn, tvGender, tvCnic, tvExpectedSalary,
            tvCountry, tvCity;
    private MaterialCardView cvResume, cvSkills;
    private TextView tvSkills;

    // Data
    private String studentId;
    private String applicationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view);

        // Get student ID and application ID from intent
        studentId = getIntent().getStringExtra("studentId");
        applicationId = getIntent().getStringExtra("applicationId");

        if (studentId == null) {
            Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        loadStudentData();
    }

    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // ImageViews
        ivPopOut = findViewById(R.id.ivPopOut);
        profileImage = findViewById(R.id.profileImage);

        // TextViews - Personal Info
        tvUsername = findViewById(R.id.tvUsername);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvMobile = findViewById(R.id.tvMobile);
        tvGender = findViewById(R.id.tvGender);
        tvCnic = findViewById(R.id.tvCnic);
        tvCountry = findViewById(R.id.tvCountry);
        tvCity = findViewById(R.id.tvCity);

        // TextViews - Academic Info (matching your storage structure)
        tvUniversity = findViewById(R.id.tvUniversity);
        tvDegree = findViewById(R.id.tvDegree);
        tvMajor = findViewById(R.id.tvMajor);
        tvGpa = findViewById(R.id.tvGpa);
        tvGraduationYear = findViewById(R.id.tvGraduationYear);
        tvExpectedSalary = findViewById(R.id.tvExpectedSalary);
        tvExperience = findViewById(R.id.tvExperience);
        tvLinkedIn = findViewById(R.id.tvLinkedIn);
        tvSkills = findViewById(R.id.tvSkills);

        // Cards
        cvResume = findViewById(R.id.cvResume);
        cvSkills = findViewById(R.id.cvSkills);
    }

    private void setupClickListeners() {
        // Back button
        ivPopOut.setOnClickListener(v -> finish());

        // LinkedIn click
        tvLinkedIn.setOnClickListener(v -> {
            String linkedInUrl = tvLinkedIn.getText().toString();
            if (!linkedInUrl.equals("Not provided") && !linkedInUrl.isEmpty()) {
                openLinkedInProfile(linkedInUrl);
            }
        });

        // Resume download/View
        cvResume.setOnClickListener(v -> {
            // Check if resume exists and open it
            checkAndOpenResume();
        });
    }

    private void loadStudentData() {
        mDatabase.child("users").child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        displayStudentData(user);
                        loadAcademicInfo();
                        loadPersonalInfo();
                        loadLocationInfo(user);
                    } else {
                        Toast.makeText(StudentView.this, "Failed to load student data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(StudentView.this, "Student data not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(StudentView.this, "Failed to load student: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayStudentData(User user) {
        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_image_picker)
                    .error(R.drawable.ic_image_picker)
                    .into(profileImage);
        }

        // Set basic information
        String fullName = user.getFirstName() + " " + user.getLastName();
        tvUsername.setText(fullName);
        tvUserEmail.setText(user.getEmail());

        // Set mobile number
        if (user.getMobileNo() != null && !user.getMobileNo().isEmpty()) {
            tvMobile.setText(user.getMobileNo());
        } else {
            tvMobile.setText("Not provided");
        }

        // Set student ID (using first 8 chars of Firebase UID)
        tvStudentId.setText(studentId.substring(0, 8).toUpperCase());
    }

    private void loadLocationInfo(User user) {
        // Load country and city from User object
        if (user.getCountry() != null && !user.getCountry().isEmpty()) {
            tvCountry.setText(user.getCountry());
        } else {
            tvCountry.setText("Not provided");
        }

        if (user.getCity() != null && !user.getCity().isEmpty()) {
            tvCity.setText(user.getCity());
        } else {
            tvCity.setText("Not provided");
        }
    }

    private void loadAcademicInfo() {
        mDatabase.child("users").child(studentId).child("academicInfo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // University
                            String university = dataSnapshot.child("university").getValue(String.class);
                            tvUniversity.setText(university != null ? university : "Not provided");

                            // Degree
                            String degree = dataSnapshot.child("degree").getValue(String.class);
                            tvDegree.setText(degree != null ? degree : "Not provided");

                            // Major
                            String major = dataSnapshot.child("major").getValue(String.class);
                            tvMajor.setText(major != null ? major : "Not provided");

                            // GPA
                            String gpa = dataSnapshot.child("gpa").getValue(String.class);
                            tvGpa.setText(gpa != null ? gpa : "Not provided");

                            // Graduation Year
                            String graduationYear = dataSnapshot.child("graduationYear").getValue(String.class);
                            tvGraduationYear.setText(graduationYear != null ? graduationYear : "Not provided");

                            // Expected Salary
                            String expectedSalary = dataSnapshot.child("expectedSalary").getValue(String.class);
                            if (expectedSalary != null && !expectedSalary.isEmpty()) {
                                try {
                                    // Format salary with commas for better readability
                                    double salary = Double.parseDouble(expectedSalary);
                                    NumberFormat formatter = NumberFormat.getInstance(Locale.US);
                                    tvExpectedSalary.setText("PKR " + formatter.format(salary));
                                } catch (NumberFormatException e) {
                                    tvExpectedSalary.setText("PKR " + expectedSalary);
                                }
                            } else {
                                tvExpectedSalary.setText("Not provided");
                            }

                            // Work Experience
                            String experience = dataSnapshot.child("workExperience").getValue(String.class);
                            tvExperience.setText(experience != null ? experience : "Not provided");

                            // LinkedIn
                            String linkedIn = dataSnapshot.child("linkedIn").getValue(String.class);
                            if (linkedIn != null && !linkedIn.isEmpty()) {
                                tvLinkedIn.setText(linkedIn);
                                tvLinkedIn.setTextColor(getResources().getColor(R.color.blue));
                            } else {
                                tvLinkedIn.setText("Not provided");
                            }

                            // Skills
                            loadSkills(dataSnapshot);
                        } else {
                            setDefaultAcademicInfo();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        setDefaultAcademicInfo();
                    }
                });
    }

    private void loadPersonalInfo() {
        mDatabase.child("users").child(studentId).child("personalInfo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Gender
                            String gender = dataSnapshot.child("gender").getValue(String.class);
                            if (gender != null && !gender.isEmpty()) {
                                tvGender.setText(gender);
                            } else {
                                tvGender.setText("Not provided");
                            }

                            // CNIC
                            String cnic = dataSnapshot.child("cnic").getValue(String.class);
                            if (cnic != null && !cnic.isEmpty()) {
                                tvCnic.setText(cnic);
                            } else {
                                tvCnic.setText("Not provided");
                            }

                            // Bio or other personal info can be loaded here if needed
                        } else {
                            setDefaultPersonalInfo();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        setDefaultPersonalInfo();
                    }
                });
    }

    private void loadSkills(DataSnapshot academicSnapshot) {
        DataSnapshot skillsSnapshot = academicSnapshot.child("skills");
        StringBuilder skillsBuilder = new StringBuilder();

        if (skillsSnapshot.exists()) {
            // Try to get skills as List
            if (skillsSnapshot.getValue() instanceof List) {
                List<?> skillsList = (List<?>) skillsSnapshot.getValue();
                for (Object skillObj : skillsList) {
                    if (skillObj instanceof String) {
                        if (skillsBuilder.length() > 0) {
                            skillsBuilder.append(", ");
                        }
                        skillsBuilder.append((String) skillObj);
                    }
                }
            }
            // Try as individual children
            else {
                for (DataSnapshot skillChild : skillsSnapshot.getChildren()) {
                    String skill = skillChild.getValue(String.class);
                    if (skill != null) {
                        if (skillsBuilder.length() > 0) {
                            skillsBuilder.append(", ");
                        }
                        skillsBuilder.append(skill);
                    }
                }
            }
        }

        if (skillsBuilder.length() > 0) {
            tvSkills.setText(skillsBuilder.toString());
        } else {
            tvSkills.setText("No skills listed");
        }
    }

    private void checkAndOpenResume() {
        // Check if resume exists in user data
        mDatabase.child("users").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String resumeUrl = dataSnapshot.child("resumeUrl").getValue(String.class);
                            if (resumeUrl != null && !resumeUrl.isEmpty()) {
                                openResume(resumeUrl);
                            } else {
                                Toast.makeText(StudentView.this, "Resume not available", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(StudentView.this, "No resume uploaded", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(StudentView.this, "Failed to load resume", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setDefaultAcademicInfo() {
        tvUniversity.setText("Not provided");
        tvDegree.setText("Not provided");
        tvMajor.setText("Not provided");
        tvGpa.setText("Not provided");
        tvGraduationYear.setText("Not provided");
        tvExpectedSalary.setText("Not provided");
        tvExperience.setText("Not provided");
        tvLinkedIn.setText("Not provided");
        tvSkills.setText("No skills listed");
    }

    private void setDefaultPersonalInfo() {
        tvGender.setText("Not provided");
        tvCnic.setText("Not provided");
        tvCountry.setText("Not provided");
        tvCity.setText("Not provided");
    }

    private void openLinkedInProfile(String linkedInUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // Ensure the URL has proper protocol
            if (!linkedInUrl.startsWith("http")) {
                linkedInUrl = "https://" + linkedInUrl;
            }
            intent.setData(Uri.parse(linkedInUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open LinkedIn profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void openResume(String resumeUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(resumeUrl), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
        }
    }
}