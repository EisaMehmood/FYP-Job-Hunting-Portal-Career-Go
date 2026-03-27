package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.careergo.Model.CompanyProfile;
import com.example.careergo.Model.Job;
import com.example.careergo.Utility.OneSignalNotificationService; // <<<< ADD THIS IMPORT
import com.example.careergo.R;
import com.example.careergo.databinding.LoadingDialogBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

public class JobCreation2 extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private TextInputEditText etJobResp, etSkills;
    private TextInputLayout etJobRespContainer, etSkillsContainer;
    private ChipGroup skillChipGroup;
    private MaterialButton btnSave;

    // Data
    private List<String> skillsList = new ArrayList<>();
    private android.app.Dialog progressDialog;

    // Received data from first activity
    private String jobTitle, companyName, cityName, age, salary, jobDesc, workType, category, gender, companyImageUrl ,experince;

    // Company profile data
    private CompanyProfile companyProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_creation2);

        initializeFirebase();
        getIntentData();
        initializeViews();
        setupClickListeners();
        setupProgressDialog();
        loadCompanyProfile();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        jobTitle = intent.getStringExtra("jobTitle");
        companyName = intent.getStringExtra("companyName");
        cityName = intent.getStringExtra("cityName");
        age = intent.getStringExtra("age");
        salary = intent.getStringExtra("salary");
        jobDesc = intent.getStringExtra("jobDesc");
        workType = intent.getStringExtra("workType");
        category = intent.getStringExtra("category");
        gender = intent.getStringExtra("gender");
        companyImageUrl = intent.getStringExtra("companyImageUrl");
        experince = intent.getStringExtra("experince");
        Toast.makeText(JobCreation2.this,experince,Toast.LENGTH_SHORT).show();
    }

    private void initializeViews() {
        // Text Inputs
        etJobResp = findViewById(R.id.etJobResp);
        etSkills = findViewById(R.id.etSkills);

        // Text Input Layouts
        etJobRespContainer = findViewById(R.id.etJobRespContainer);
        etSkillsContainer = findViewById(R.id.etSkillsContainer);

        // Chip Group
        skillChipGroup = findViewById(R.id.skillChipGroup);

        // Buttons
        btnSave = findViewById(R.id.btnSave);

        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        // Skills input - add chip on enter or comma
        etSkills.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_COMMA) {
                addSkillChip();
                return true;
            }
            return false;
        });

        // Save button
        btnSave.setOnClickListener(v -> validateAndSaveJob());
    }

    private void loadCompanyProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        mDatabase.child("companyProfiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    companyProfile = dataSnapshot.getValue(CompanyProfile.class);
                    if (companyProfile != null) {
                        // Auto-fill company data if not provided in first activity
                        autoFillCompanyData();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Silent fail - company profile is optional
            }
        });
    }

    private void autoFillCompanyData() {
        // If company name is empty, use company profile name
        if (TextUtils.isEmpty(companyName) && companyProfile.getCompanyName() != null) {
            companyName = companyProfile.getCompanyName();
        }

        // If city is empty, use company profile city
        if (TextUtils.isEmpty(cityName) && companyProfile.getCity() != null) {
            cityName = companyProfile.getCity();
        }

        // If company image is empty, use company profile logo
        if (TextUtils.isEmpty(companyImageUrl) && companyProfile.getLogoUrl() != null) {
            companyImageUrl = companyProfile.getLogoUrl();
        }

        // Add default skills based on company industry
        addDefaultSkills();
    }

    private void addDefaultSkills() {
        if (companyProfile != null && companyProfile.getIndustry() != null) {
            List<String> defaultSkills = getDefaultSkillsForIndustry(companyProfile.getIndustry());

            for (String skill : defaultSkills) {
                if (!skillsList.contains(skill)) {
                    skillsList.add(skill);
                    createSkillChip(skill);
                }
            }

            if (!defaultSkills.isEmpty()) {
                Toast.makeText(this, "Added default skills for " + companyProfile.getIndustry() + " industry", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<String> getDefaultSkillsForIndustry(String industry) {
        List<String> defaultSkills = new ArrayList<>();

        switch (industry.toLowerCase()) {
            case "information technology":
            case "it":
            case "software":
                defaultSkills.add("Java");
                defaultSkills.add("Python");
                defaultSkills.add("SQL");
                defaultSkills.add("Problem Solving");
                break;

            case "finance":
            case "banking":
                defaultSkills.add("Financial Analysis");
                defaultSkills.add("Excel");
                defaultSkills.add("Accounting");
                defaultSkills.add("Risk Management");
                break;

            case "healthcare":
            case "medical":
                defaultSkills.add("Patient Care");
                defaultSkills.add("Medical Knowledge");
                defaultSkills.add("Communication");
                defaultSkills.add("Teamwork");
                break;

            case "education":
                defaultSkills.add("Teaching");
                defaultSkills.add("Communication");
                defaultSkills.add("Curriculum Development");
                defaultSkills.add("Student Assessment");
                break;

            case "marketing":
            case "advertising":
                defaultSkills.add("Digital Marketing");
                defaultSkills.add("SEO");
                defaultSkills.add("Social Media");
                defaultSkills.add("Content Creation");
                break;

            case "retail":
                defaultSkills.add("Customer Service");
                defaultSkills.add("Sales");
                defaultSkills.add("Inventory Management");
                defaultSkills.add("Communication");
                break;

            default:
                // Generic skills for any industry
                defaultSkills.add("Communication");
                defaultSkills.add("Teamwork");
                defaultSkills.add("Problem Solving");
                break;
        }

        return defaultSkills;
    }

    private void addSkillChip() {
        String skill = etSkills.getText().toString().trim();

        if (!TextUtils.isEmpty(skill)) {
            // Remove comma if present
            if (skill.endsWith(",")) {
                skill = skill.substring(0, skill.length() - 1).trim();
            }

            if (!skill.isEmpty() && !skillsList.contains(skill)) {
                skillsList.add(skill);
                createSkillChip(skill);
                etSkills.setText("");
            }
        }
    }

    private void createSkillChip(String skill) {
        Chip chip = new Chip(this);
        chip.setText(skill);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.light_blue);
        chip.setTextColor(getResources().getColor(R.color.text_color));

        chip.setOnCloseIconClickListener(v -> {
            skillChipGroup.removeView(chip);
            skillsList.remove(skill);
        });

        skillChipGroup.addView(chip);
    }

    private void validateAndSaveJob() {
        String jobResp = etJobResp.getText().toString().trim();

        boolean isValid = true;

        // Clear previous errors
        clearErrors();

        // Validate fields
        if (TextUtils.isEmpty(jobResp)) {
            etJobRespContainer.setError("Job responsibilities are required");
            isValid = false;
        } else if (jobResp.length() < 30) {
            etJobRespContainer.setError("Job responsibilities should be at least 30 characters");
            isValid = false;
        }

        if (skillsList.isEmpty()) {
            Toast.makeText(this, "Please add at least one skill", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            saveJobToFirebase(jobResp);
        }
    }

    private void saveJobToFirebase(String jobResp) {
        showProgressDialog();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            hideProgressDialog();
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobId = mDatabase.child("jobs").push().getKey();
        if (jobId == null) {
            hideProgressDialog();
            Toast.makeText(this, "Failed to create job", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use company profile data as fallback
        String finalCompanyName = !TextUtils.isEmpty(companyName) ? companyName :
                (companyProfile != null ? companyProfile.getCompanyName() : "Company");

        String finalCityName = !TextUtils.isEmpty(cityName) ? cityName :
                (companyProfile != null ? companyProfile.getCity() : "City");

        String finalCompanyImageUrl = !TextUtils.isEmpty(companyImageUrl) ? companyImageUrl :
                (companyProfile != null ? companyProfile.getLogoUrl() : "");

        // Use default values if age/gender are null
        String finalAge = age != null ? age : "";
        String finalGender = gender != null ? gender : "Any Gender";

        // Create job object - UPDATED CONSTRUCTOR WITH AGE AND GENDER
        Job job = new Job(
                jobId,
                jobTitle,
                finalCompanyName,
                finalCityName,
                salary,
                jobDesc,
                workType,
                category,
                jobResp,
                skillsList,
                finalCompanyImageUrl,
                currentUser.getUid(),
                System.currentTimeMillis(),
                true, // isActive
                finalAge, // Age requirement
                finalGender // Gender preference
        );

        // Save to Firebase
        mDatabase.child("jobs").child(jobId).setValue(job)
                .addOnSuccessListener(aVoid -> {


                    // Send notifications to matching job seekers
                    sendNotificationsToJobSeekers(jobId, jobTitle, category, finalCityName, skillsList, finalCompanyName, finalAge, finalGender);

                    hideProgressDialog();
                    Toast.makeText(JobCreation2.this, "Job created successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate back to employer home or jobs list
                    Intent intent = new Intent(JobCreation2.this, EmployerHome.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(JobCreation2.this, "Failed to save job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Send notifications to job seekers - UPDATED WITH AGE AND GENDER FILTERING
    private void sendNotificationsToJobSeekers(String jobId, String jobTitle, String category,
                                               String location, List<String> skills, String company,
                                               String ageRequirement, String genderRequirement) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> playerIds = new ArrayList<>();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);
                    String oneSignalId = userSnapshot.child("oneSignalId").getValue(String.class);

                    if ("Job Seeker".equals(role) && oneSignalId != null && !oneSignalId.isEmpty()) {
                        // Check if job matches user preferences
                        if (isJobMatchingUserPreferences(userSnapshot, category, ageRequirement, genderRequirement)) {
                            playerIds.add(oneSignalId);
                        }
                    }
                }

                if (!playerIds.isEmpty()) {
                    sendOneSignalNotification(playerIds, jobId, jobTitle, location, company, ageRequirement, genderRequirement);
                    Log.d("Notification", "Sending notification to " + playerIds.size() + " job seekers");
                } else {
                    Log.d("Notification", "No matching job seekers found for this job");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Notification", "Error finding job seekers", databaseError.toException());
            }
        });
    }

    // Check if user preferences match job requirements including age and gender
    private boolean isJobMatchingUserPreferences(DataSnapshot userSnapshot, String jobCategory,
                                                 String jobAgeRequirement, String jobGenderRequirement) {
        // Check notification preference first
        if (!userSnapshot.hasChild("preferences")) return true;

        DataSnapshot preferences = userSnapshot.child("preferences");

        // Check if notifications are enabled
        Boolean notificationsEnabled = preferences.child("notificationEnabled").getValue(Boolean.class);
        if (notificationsEnabled == null || !notificationsEnabled) return false;

        // Check category preferences
        if (preferences.hasChild("categories")) {
            List<String> userCategories = new ArrayList<>();
            for (DataSnapshot catSnapshot : preferences.child("categories").getChildren()) {
                String cat = catSnapshot.getValue(String.class);
                if (cat != null) userCategories.add(cat.toLowerCase());
            }
            if (!userCategories.isEmpty() && !userCategories.contains(jobCategory.toLowerCase())) {
                return false;
            }
        }

        // Check gender preference if specified in job
        if (!TextUtils.isEmpty(jobGenderRequirement) && !jobGenderRequirement.equals("Any Gender")) {
            String userGender = userSnapshot.child("gender").getValue(String.class);
            if (userGender != null) {
                // If job requires specific gender and user gender doesn't match
                if (jobGenderRequirement.equals("Male Only") && !userGender.equalsIgnoreCase("male")) {
                    return false;
                }
                if (jobGenderRequirement.equals("Female Only") && !userGender.equalsIgnoreCase("female")) {
                    return false;
                }
            }
        }

        // Check age requirement if specified in job
        if (!TextUtils.isEmpty(jobAgeRequirement)) {
            String userAgeStr = userSnapshot.child("age").getValue(String.class);
            if (userAgeStr != null && !userAgeStr.isEmpty()) {
                try {
                    int userAge = Integer.parseInt(userAgeStr);
                    if (!isAgeWithinRequirement(userAge, jobAgeRequirement)) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // If can't parse age, skip age validation
                    Log.d("AgeValidation", "Invalid user age format: " + userAgeStr);
                }
            }
        }

        return true;
    }

    // Helper method to check if user age is within job requirement
    private boolean isAgeWithinRequirement(int userAge, String ageRequirement) {
        if (TextUtils.isEmpty(ageRequirement)) return true;

        String cleanAge = ageRequirement.replace(" ", "").toLowerCase();

        // Check single number (minimum age)
        if (cleanAge.endsWith("+")) {
            try {
                String numberPart = cleanAge.substring(0, cleanAge.length() - 1);
                int minAge = Integer.parseInt(numberPart);
                return userAge >= minAge;
            } catch (NumberFormatException e) {
                return true; // If can't parse, skip validation
            }
        }

        // Check range with hyphen
        if (cleanAge.contains("-")) {
            String[] parts = cleanAge.split("-");
            if (parts.length == 2) {
                try {
                    int minAge = Integer.parseInt(parts[0]);
                    int maxAge = Integer.parseInt(parts[1]);
                    return userAge >= minAge && userAge <= maxAge;
                } catch (NumberFormatException e) {
                    return true; // If can't parse, skip validation
                }
            }
        }

        // Check single specific age
        if (TextUtils.isDigitsOnly(cleanAge)) {
            try {
                int requiredAge = Integer.parseInt(cleanAge);
                return userAge == requiredAge;
            } catch (NumberFormatException e) {
                return true; // If can't parse, skip validation
            }
        }

        return true; // If format not recognized, don't filter by age
    }

    // Call OneSignal service to send notification
    private void sendOneSignalNotification(List<String> playerIds, String jobId,
                                           String jobTitle, String location, String company,
                                           String ageRequirement, String genderRequirement) {
        new Thread(() -> {
            try {
                OneSignalNotificationService notificationService = new OneSignalNotificationService();
                String heading = "New Job Opportunity!";

                // Create notification content with relevant details
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append(jobTitle).append(" in ").append(location);
                if (!TextUtils.isEmpty(company)) {
                    contentBuilder.append(" - ").append(company);
                }

                // Add age and gender requirements if specified
                if (!TextUtils.isEmpty(ageRequirement) ||
                        !TextUtils.isEmpty(genderRequirement) && !genderRequirement.equals("Any Gender")) {
                    contentBuilder.append(" (");
                    if (!TextUtils.isEmpty(ageRequirement)) {
                        contentBuilder.append("Age: ").append(ageRequirement);
                    }
                    if (!TextUtils.isEmpty(genderRequirement) && !genderRequirement.equals("Any Gender")) {
                        if (!TextUtils.isEmpty(ageRequirement)) {
                            contentBuilder.append(", ");
                        }
                        contentBuilder.append("Gender: ").append(genderRequirement);
                    }
                    contentBuilder.append(")");
                }

                String content = contentBuilder.toString();

                boolean success = notificationService.sendNotification(playerIds, heading, content, jobId);

                runOnUiThread(() -> {
                    if (success) {
                        Log.d("Notification", "Notification sent successfully to " + playerIds.size() + " users");
                    } else {
                        Log.e("Notification", "Failed to send notification");
                    }
                });

            } catch (Exception e) {
                Log.e("Notification", "Error sending notification", e);
            }
        }).start();
    }

    private void setupProgressDialog() {
        progressDialog = new android.app.Dialog(this);
        progressDialog.setCancelable(false);
        LoadingDialogBinding loadingBinding = LoadingDialogBinding.inflate(LayoutInflater.from(this));
        progressDialog.setContentView(loadingBinding.getRoot());
    }

    private void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void clearErrors() {
        etJobRespContainer.setError(null);
        etSkillsContainer.setError(null);
    }
}