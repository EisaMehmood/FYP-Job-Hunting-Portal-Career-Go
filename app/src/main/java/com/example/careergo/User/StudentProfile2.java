package com.example.careergo.User;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentProfile2 extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etUniversity, etDegree, etMajor, etGpa, etGraduationYear, etExpectedSalary, etExperience, etLinkedIn, etSkills;
    private ChipGroup skillsChipGroup;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    private List<String> skillsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile2);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        initializeViews();
        loadExistingData();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        etUniversity = findViewById(R.id.etUniversity);
        etDegree = findViewById(R.id.etDegree);
        etMajor = findViewById(R.id.etMajor);
        etGpa = findViewById(R.id.etGpa);
        etGraduationYear = findViewById(R.id.etGraduationYear);
        etExpectedSalary = findViewById(R.id.etExpectedSalary);
        etExperience = findViewById(R.id.etExperience);
        etLinkedIn = findViewById(R.id.etLinkedIn);
        etSkills = findViewById(R.id.etSkills);
        skillsChipGroup = findViewById(R.id.skillsChipGroup);
        btnSave = findViewById(R.id.btnSave);

        // Back button click
        ivBack.setOnClickListener(v -> finish());

        // Skills input handling - Add skill when user presses enter
        etSkills.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                addSkillFromInput();
                return true;
            }
            return false;
        });

        // Save button click
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void addSkillFromInput() {
        String skill = etSkills.getText().toString().trim();

        if (!TextUtils.isEmpty(skill)) {
            addSkillToChipGroup(skill);
            etSkills.setText("");
        }
    }

    private void addSkillToChipGroup(String skill) {
        // Normalize the skill for comparison
        String normalizedSkill = skill.toLowerCase().trim();

        // Check if skill already exists (case insensitive)
        boolean skillExists = false;
        for (String existingSkill : skillsList) {
            if (existingSkill.equalsIgnoreCase(skill)) {
                skillExists = true;
                break;
            }
        }

        if (skillExists) {
            Toast.makeText(this, "Skill already added", Toast.LENGTH_SHORT).show();
            return;
        }

        skillsList.add(skill); // Store the original case

        Chip chip = new Chip(this);
        chip.setText(skill);
        chip.setCloseIconVisible(true);
        chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.text_color_secondary)));
        chip.setTextColor(getResources().getColor(R.color.text_color));

        // Set chip background color - make sure this color exists in your colors.xml
        try {
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.skill_chip_color)));
        } catch (Exception e) {
            // Fallback color if skill_chip_color doesn't exist
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
        }

        chip.setOnCloseIconClickListener(v -> {
            skillsChipGroup.removeView(chip);
            skillsList.remove(skill);
            Log.d("ChipDebug", "Removed skill: " + skill + ", Remaining: " + skillsList.size());
        });

        skillsChipGroup.addView(chip);
        Log.d("ChipDebug", "Added skill: " + skill + ", Total: " + skillsList.size());
    }

    private void validateAndSave() {
        String university = etUniversity.getText().toString().trim();
        String degree = etDegree.getText().toString().trim();
        String major = etMajor.getText().toString().trim();
        String gpa = etGpa.getText().toString().trim();
        String graduationYear = etGraduationYear.getText().toString().trim();
        String expectedSalary = etExpectedSalary.getText().toString().trim();
        String experience = etExperience.getText().toString().trim();
        String linkedIn = etLinkedIn.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(university)) {
            etUniversity.setError("University/College is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(degree)) {
            etDegree.setError("Degree is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(major)) {
            etMajor.setError("Major/Field is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(gpa)) {
            etGpa.setError("GPA is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(graduationYear)) {
            etGraduationYear.setError("Graduation year is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(expectedSalary)) {
            etExpectedSalary.setError("Expected salary is required");
            hasError = true;
        } else {
            // Validate salary is a positive number
            try {
                double salary = Double.parseDouble(expectedSalary);
                if (salary <= 0) {
                    etExpectedSalary.setError("Salary must be greater than 0");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                etExpectedSalary.setError("Please enter a valid number");
                hasError = true;
            }
        }

        if (skillsList.isEmpty()) {
            Toast.makeText(this, "Please add at least one skill", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // Validate URLs
        if (!TextUtils.isEmpty(linkedIn) && !linkedIn.startsWith("http")) {
            linkedIn = "https://" + linkedIn;
        }

        saveAcademicAndSkillsInfo(university, degree, major, gpa, graduationYear, expectedSalary, experience, linkedIn);
    }

    private void saveAcademicAndSkillsInfo(String university, String degree, String major, String gpa,
                                           String graduationYear, String expectedSalary, String experience, String linkedIn) {
        Map<String, Object> academicInfo = new HashMap<>();
        academicInfo.put("university", university);
        academicInfo.put("degree", degree);
        academicInfo.put("major", major);
        academicInfo.put("gpa", gpa);
        academicInfo.put("graduationYear", graduationYear);
        academicInfo.put("expectedSalary", expectedSalary);
        academicInfo.put("workExperience", experience);
        academicInfo.put("linkedIn", linkedIn);
        academicInfo.put("skills", skillsList);
        academicInfo.put("profileCompleted", true);

        mDatabase.child("users").child(currentUser.getUid()).child("academicInfo")
                .setValue(academicInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile completed successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StudentProfile2.this, UserHome.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save academic information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingData() {
        mDatabase.child("users").child(currentUser.getUid()).child("academicInfo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Debug: Print the entire data snapshot
                            Log.d("FirebaseData", "DataSnapshot: " + dataSnapshot.toString());

                            String university = dataSnapshot.child("university").getValue(String.class);
                            String degree = dataSnapshot.child("degree").getValue(String.class);
                            String major = dataSnapshot.child("major").getValue(String.class);
                            String gpa = dataSnapshot.child("gpa").getValue(String.class);
                            String graduationYear = dataSnapshot.child("graduationYear").getValue(String.class);
                            String expectedSalary = dataSnapshot.child("expectedSalary").getValue(String.class);
                            String experience = dataSnapshot.child("workExperience").getValue(String.class);
                            String linkedIn = dataSnapshot.child("linkedIn").getValue(String.class);

                            // Set text fields
                            runOnUiThread(() -> {
                                if (university != null) etUniversity.setText(university);
                                if (degree != null) etDegree.setText(degree);
                                if (major != null) etMajor.setText(major);
                                if (gpa != null) etGpa.setText(gpa);
                                if (graduationYear != null) etGraduationYear.setText(graduationYear);
                                if (expectedSalary != null) etExpectedSalary.setText(expectedSalary);
                                if (experience != null) etExperience.setText(experience);
                                if (linkedIn != null) etLinkedIn.setText(linkedIn);
                            });

                            // Load skills
                            loadSkillsFromSnapshot(dataSnapshot);
                        } else {
                            Log.d("FirebaseData", "No academicInfo data found");
                            runOnUiThread(() ->
                                    Toast.makeText(StudentProfile2.this, "No existing profile data found", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("FirebaseData", "Error loading data: " + databaseError.getMessage());
                        runOnUiThread(() ->
                                Toast.makeText(StudentProfile2.this, "Failed to load profile data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void loadSkillsFromSnapshot(DataSnapshot dataSnapshot) {
        DataSnapshot skillsSnapshot = dataSnapshot.child("skills");
        Log.d("SkillsDebug", "Skills snapshot exists: " + skillsSnapshot.exists());

        List<String> existingSkills = new ArrayList<>();

        if (skillsSnapshot.exists()) {
            // Method 1: Try as List
            if (skillsSnapshot.getValue() instanceof List) {
                List<?> skillsListObj = (List<?>) skillsSnapshot.getValue();
                Log.d("SkillsDebug", "Skills as List, size: " + skillsListObj.size());

                for (Object skillObj : skillsListObj) {
                    if (skillObj instanceof String) {
                        String skill = (String) skillObj;
                        existingSkills.add(skill);
                        Log.d("SkillsDebug", "Added skill from list: " + skill);
                    }
                }
            }
            // Method 2: Try as Map (if skills are stored as key-value pairs)
            else if (skillsSnapshot.getValue() instanceof Map) {
                Map<?, ?> skillsMap = (Map<?, ?>) skillsSnapshot.getValue();
                Log.d("SkillsDebug", "Skills as Map, size: " + skillsMap.size());

                for (Object skillObj : skillsMap.values()) {
                    if (skillObj instanceof String) {
                        String skill = (String) skillObj;
                        existingSkills.add(skill);
                        Log.d("SkillsDebug", "Added skill from map: " + skill);
                    }
                }
            }
            // Method 3: Try to get individual children
            else {
                Log.d("SkillsDebug", "Trying to get individual skill children");
                for (DataSnapshot skillChild : skillsSnapshot.getChildren()) {
                    String skill = skillChild.getValue(String.class);
                    if (skill != null) {
                        existingSkills.add(skill);
                        Log.d("SkillsDebug", "Added skill from child: " + skill);
                    }
                }
            }
        }

        Log.d("SkillsDebug", "Final skills to load: " + existingSkills + ", count: " + existingSkills.size());

        // Update UI on main thread
        runOnUiThread(() -> {
            if (!existingSkills.isEmpty()) {
                skillsList.clear();
                skillsChipGroup.removeAllViews(); // Clear existing chips

                for (String skill : existingSkills) {
                    addSkillToChipGroup(skill);
                }
                Toast.makeText(StudentProfile2.this, "Loaded " + existingSkills.size() + " skills", Toast.LENGTH_SHORT).show();
                Log.d("SkillsDebug", "Skills loaded successfully. Chip count: " + skillsChipGroup.getChildCount());
            } else {
                Log.d("SkillsDebug", "No skills found in Firebase");
                Toast.makeText(StudentProfile2.this, "No existing skills found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}