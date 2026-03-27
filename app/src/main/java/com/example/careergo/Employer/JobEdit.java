package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Model.Job;
import com.example.careergo.R;
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
import com.skydoves.powerspinner.PowerSpinnerView;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class JobEdit extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private CircleImageView companyImage;
    private TextInputEditText etJobTitle, etCompanyName, etCityName, etAge, etSalary, etJobDesc, etJobResp, etSkills;
    private TextInputLayout etJobTitleContainer, etCompanyNameContainer, etCityNameContainer,
            etAgeContainer, etSalaryContainer, etJobDescContainer, etJobRespContainer, etSkillsContainer;
    private PowerSpinnerView workTypeSpinner, categorySpinner, genderSpinner;
    private ChipGroup skillChipGroup;
    private MaterialButton btnSave;

    // Data
    private List<String> skillsList = new ArrayList<>();
    private ProgressDialog progressDialog;
    private String jobId;
    private Job currentJob;
    private List<String> categoriesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_edit);

        // Get job ID from intent
        jobId = getIntent().getStringExtra("jobId");
        if (jobId == null) {
            Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupProgressDialog();
        loadCategoriesFromFirebase();
        loadJobData(jobId);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // Image Views
        companyImage = findViewById(R.id.companyImage);

        // Text Inputs
        etJobTitle = findViewById(R.id.etJobTitle);
        etCompanyName = findViewById(R.id.etCompanyName);
        etCityName = findViewById(R.id.etCityName);
        etAge = findViewById(R.id.etAge);
        etSalary = findViewById(R.id.etSalary);
        etJobDesc = findViewById(R.id.etJobDesc);
        etJobResp = findViewById(R.id.etJobResp);
        etSkills = findViewById(R.id.etSkills);

        // Text Input Layouts
        etJobTitleContainer = findViewById(R.id.etJobTitleContainer);
        etCompanyNameContainer = findViewById(R.id.etCompanyNameContainer);
        etCityNameContainer = findViewById(R.id.etCityNameContainer);
        etAgeContainer = findViewById(R.id.etAgeContainer);
        etSalaryContainer = findViewById(R.id.etSalaryContainer);
        etJobDescContainer = findViewById(R.id.etJobDescContainer);
        etJobRespContainer = findViewById(R.id.etJobRespContainer);
        etSkillsContainer = findViewById(R.id.etSkillsContainer);

        // Spinners
        workTypeSpinner = findViewById(R.id.workTypeSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        genderSpinner = findViewById(R.id.genderSpinner);

        // Chip Group
        skillChipGroup = findViewById(R.id.skillChipGroup);

        // Buttons
        btnSave = findViewById(R.id.btnSave);
    }

    private void loadCategoriesFromFirebase() {
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                categoriesList.clear();
                categoriesList.add("Select Category");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    if (category != null && !categoriesList.contains(category)) {
                        categoriesList.add(category);
                    }
                }

                categorySpinner.setItems(categoriesList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(JobEdit.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                setDefaultCategories();
            }
        });
    }

    private void setDefaultCategories() {
        String[] defaultCategories = {
                "Select Category",
                "Information Technology",
                "Healthcare",
                "Finance",
                "Education",
                "Marketing",
                "Engineering",
                "Sales",
                "Design",
                "Business",
                "Other"
        };

        categoriesList.clear();
        for (String category : defaultCategories) {
            categoriesList.add(category);
        }
        categorySpinner.setItems(categoriesList);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Skills input - add chip on enter or comma
        etSkills.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_COMMA) {
                addSkillChip();
                return true;
            }
            return false;
        });

        // Save button
        btnSave.setOnClickListener(v -> validateAndUpdateJob());
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void showProgressDialog(String message) {
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadJobData(String jobId) {
        showProgressDialog("Loading job data...");

        mDatabase.child("jobs").child(jobId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                hideProgressDialog();

                if (dataSnapshot.exists()) {
                    currentJob = dataSnapshot.getValue(Job.class);
                    if (currentJob != null) {
                        populateFormWithJobData();
                    } else {
                        Toast.makeText(JobEdit.this, "Failed to load job data", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(JobEdit.this, "Job not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                hideProgressDialog();
                Toast.makeText(JobEdit.this, "Failed to load job: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateFormWithJobData() {
        // Fill form fields with existing job data
        etJobTitle.setText(currentJob.getJobTitle());
        etCompanyName.setText(currentJob.getCompanyName());
        etCityName.setText(currentJob.getCity());
        etAge.setText(currentJob.getAgeRequirement());
        etSalary.setText(currentJob.getSalary());
        etJobDesc.setText(currentJob.getJobDescription());
        etJobResp.setText(currentJob.getJobResponsibilities());

        // Set spinner values
        if (currentJob.getWorkType() != null) {
            workTypeSpinner.setText(currentJob.getWorkType());
        }

        // Set category spinner
        if (currentJob.getDesignation() != null) {
            categorySpinner.setText(currentJob.getDesignation());
        }

        if (currentJob.getGenderPreference() != null) {
            genderSpinner.setText(currentJob.getGenderPreference());
        }

        // Load company image
        if (currentJob.getCompanyImageUrl() != null && !currentJob.getCompanyImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentJob.getCompanyImageUrl())
                    .placeholder(R.drawable.ic_jobs)
                    .into(companyImage);
        }

        // Load skills
        if (currentJob.getRequiredSkills() != null) {
            skillsList.clear();
            skillsList.addAll(currentJob.getRequiredSkills());
            refreshSkillChips();
        }
    }

    private void refreshSkillChips() {
        skillChipGroup.removeAllViews();
        for (String skill : skillsList) {
            createSkillChip(skill);
        }
    }

    private void addSkillChip() {
        String skill = etSkills.getText().toString().trim();

        if (!TextUtils.isEmpty(skill)) {
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

    private void validateAndUpdateJob() {
        String jobTitle = etJobTitle.getText().toString().trim();
        String companyName = etCompanyName.getText().toString().trim();
        String cityName = etCityName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String salary = etSalary.getText().toString().trim();
        String jobDesc = etJobDesc.getText().toString().trim();
        String jobResp = etJobResp.getText().toString().trim();
        String workType = workTypeSpinner.getText().toString();
        String category = categorySpinner.getText().toString();
        String gender = genderSpinner.getText().toString();

        boolean isValid = true;

        // Clear previous errors
        clearErrors();

        // Validate fields
        if (TextUtils.isEmpty(jobTitle)) {
            etJobTitleContainer.setError("Job title is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(companyName)) {
            etCompanyNameContainer.setError("Company name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(cityName)) {
            etCityNameContainer.setError("City is required");
            isValid = false;
        }

        if (!TextUtils.isEmpty(age)) {
            if (!isValidAgeFormat(age)) {
                etAgeContainer.setError("Please enter a valid age format (e.g., 18-35, 25+, 18 to 35)");
                isValid = false;
            }
        }

        if (TextUtils.isEmpty(salary)) {
            etSalaryContainer.setError("Salary is required");
            isValid = false;
        } else if (!TextUtils.isDigitsOnly(salary)) {
            etSalaryContainer.setError("Please enter a valid salary");
            isValid = false;
        }

        if (TextUtils.isEmpty(jobDesc)) {
            etJobDescContainer.setError("Job description is required");
            isValid = false;
        } else if (jobDesc.length() < 50) {
            etJobDescContainer.setError("Job description should be at least 50 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(jobResp)) {
            etJobRespContainer.setError("Job responsibilities are required");
            isValid = false;
        } else if (jobResp.length() < 30) {
            etJobRespContainer.setError("Job responsibilities should be at least 30 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(workType) || workType.equals("Work Type")) {
            Toast.makeText(this, "Please select work type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(category) || category.equals("Select Category")) {
            Toast.makeText(this, "Please select job category", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(gender) || gender.equals("Gender Preference")) {
            Toast.makeText(this, "Please select gender preference", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (skillsList.isEmpty()) {
            Toast.makeText(this, "Please add at least one skill", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            updateJobInFirebase(jobTitle, companyName, cityName, age, salary, jobDesc, jobResp, workType, category, gender);
        }
    }

    private boolean isValidAgeFormat(String age) {
        if (TextUtils.isEmpty(age)) {
            return true;
        }

        String cleanAge = age.replace(" ", "");

        // Check for single number
        if (TextUtils.isDigitsOnly(cleanAge)) {
            try {
                int ageNum = Integer.parseInt(cleanAge);
                return ageNum > 0 && ageNum < 100;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Check for range with hyphen
        if (cleanAge.contains("-")) {
            String[] parts = cleanAge.split("-");
            if (parts.length == 2) {
                try {
                    int minAge = Integer.parseInt(parts[0]);
                    int maxAge = Integer.parseInt(parts[1]);
                    return minAge > 0 && maxAge < 100 && minAge < maxAge;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        // Check for plus sign
        if (cleanAge.endsWith("+")) {
            String numberPart = cleanAge.substring(0, cleanAge.length() - 1);
            if (TextUtils.isDigitsOnly(numberPart)) {
                try {
                    int minAge = Integer.parseInt(numberPart);
                    return minAge > 0 && minAge < 100;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        // Check for 'to' format
        if (age.toLowerCase().contains("to")) {
            String[] parts = age.toLowerCase().split("to");
            if (parts.length == 2) {
                try {
                    int minAge = Integer.parseInt(parts[0].trim());
                    int maxAge = Integer.parseInt(parts[1].trim());
                    return minAge > 0 && maxAge < 100 && minAge < maxAge;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return false;
    }

    private void updateJobInFirebase(String jobTitle, String companyName, String cityName,
                                     String age, String salary, String jobDesc, String jobResp,
                                     String workType, String category, String gender) {
        showProgressDialog("Updating job...");

        // Verify current user is the job owner
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || !currentJob.getEmployerId().equals(currentUser.getUid())) {
            hideProgressDialog();
            Toast.makeText(this, "Unauthorized to edit this job", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update job object
        currentJob.setJobTitle(jobTitle);
        currentJob.setCompanyName(companyName);
        currentJob.setCity(cityName);
        currentJob.setAgeRequirement(age);
        currentJob.setSalary(salary);
        currentJob.setJobDescription(jobDesc);
        currentJob.setJobResponsibilities(jobResp);
        currentJob.setWorkType(workType);
        currentJob.setDesignation(category);
        currentJob.setGenderPreference(gender);
        currentJob.setRequiredSkills(skillsList);
        // Company image URL remains unchanged

        // Update in Firebase
        mDatabase.child("jobs").child(jobId).setValue(currentJob)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    Toast.makeText(JobEdit.this, "Job updated successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(JobEdit.this, "Failed to update job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearErrors() {
        etJobTitleContainer.setError(null);
        etCompanyNameContainer.setError(null);
        etCityNameContainer.setError(null);
        etAgeContainer.setError(null);
        etSalaryContainer.setError(null);
        etJobDescContainer.setError(null);
        etJobRespContainer.setError(null);
        etSkillsContainer.setError(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}