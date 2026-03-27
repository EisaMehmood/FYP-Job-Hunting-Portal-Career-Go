package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.example.careergo.Model.CompanyProfile;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.skydoves.powerspinner.PowerSpinnerView;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class JobCreation extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;

    // UI Components
    private CircleImageView ivCompany;
    private TextInputEditText etJobTitle, etCompanyName, etCityName, etAge, etSalary, etJobDesc ,etexperinece;
    private TextInputLayout etJobTitleContainer, etCompanyNameContainer, etCityNameContainer,
            etAgeContainer, etSalaryContainer, etJobDescContainer ,etexperineceContainer;
    private PowerSpinnerView workTypeSpinner, categorySpinner, genderSpinner;
    private MaterialButton btnNext;

    // Data
    private List<String> categoriesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_creation);

        initializeFirebase();
        initializeViews();
        loadCategoriesFromFirebase(); // Load categories from Firebase
        loadCompanyProfileAndSetDefaults();
        setupClickListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // Image Views
        ivCompany = findViewById(R.id.ivCompany);

        // Text Inputs
        etJobTitle = findViewById(R.id.etJobTitle);
        etCompanyName = findViewById(R.id.etCompanyName);
        etCityName = findViewById(R.id.etCityName);
        etAge = findViewById(R.id.etAge);
        etSalary = findViewById(R.id.etSalary);
        etJobDesc = findViewById(R.id.etJobDesc);
        etexperinece = findViewById(R.id.etexerperince);


        // Text Input Layouts
        etJobTitleContainer = findViewById(R.id.etJobTitleContainer);
        etCompanyNameContainer = findViewById(R.id.etCompanyNameContainer);
        etCityNameContainer = findViewById(R.id.etCityNameContainer);
        etAgeContainer = findViewById(R.id.etAgeContainer);
        etSalaryContainer = findViewById(R.id.etSalaryContainer);
        etJobDescContainer = findViewById(R.id.etJobDescContainer);
        etexperineceContainer = findViewById(R.id.etexeperienceContainer);

        // Spinners
        workTypeSpinner = findViewById(R.id.workTypeSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        genderSpinner = findViewById(R.id.genderSpinner);

        // Buttons
        btnNext = findViewById(R.id.btnNext);

        // Set default selections for spinners
        workTypeSpinner.selectItemByIndex(0);
        genderSpinner.selectItemByIndex(0); // Default to "Any Gender"
    }

    private void loadCategoriesFromFirebase() {
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                categoriesList.clear();
                categoriesList.add("Select Category"); // Add default option

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    if (category != null && !categoriesList.contains(category)) {
                        categoriesList.add(category);
                    }
                }

                // Set the categories to the spinner
                categorySpinner.setItems(categoriesList);

                // Set default selection
                if (!categoriesList.isEmpty()) {
                    categorySpinner.selectItemByIndex(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(JobCreation.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                // Set default categories if Firebase fails
                setDefaultCategories();
            }
        });
    }

    private void setDefaultCategories() {
        // Fallback categories if Firebase is not available
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
        categorySpinner.selectItemByIndex(0);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Next button
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void validateAndProceed() {
        String jobTitle = etJobTitle.getText().toString().trim();
        String companyName = etCompanyName.getText().toString().trim();
        String cityName = etCityName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String salary = etSalary.getText().toString().trim();
        String jobDesc = etJobDesc.getText().toString().trim();
        String workType = workTypeSpinner.getText().toString();
        String category = categorySpinner.getText().toString();
        String gender = genderSpinner.getText().toString();
        String experince = etexperinece.getText().toString();
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
        if (TextUtils.isEmpty(experince)) {
            etCityNameContainer.setError("experice  is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(cityName)) {
            etCityNameContainer.setError("City is required");
            isValid = false;
        }

        // Validate age field (optional but if filled, validate format)
        if (!TextUtils.isEmpty(age)) {
            // Validate age format (e.g., 18-35, 25+, 18 to 35, etc.)
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

        if (TextUtils.isEmpty(workType) || workType.equals("Work Type")) {
            Toast.makeText(this, "Please select work type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate category
        if (TextUtils.isEmpty(category) || category.equals("Select Category")) {
            Toast.makeText(this, "Please select job category", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate gender (optional but validate if selected)
        if (TextUtils.isEmpty(gender) || gender.equals("Gender Preference")) {
            Toast.makeText(this, "Please select gender preference", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            proceedToNextActivity(jobTitle, companyName, experince , cityName, age, salary,
                    jobDesc, workType, category, gender);
        }
    }

    private boolean isValidAgeFormat(String age) {
        // Acceptable formats:
        // - Single number: 18, 25
        // - Range with hyphen: 18-35, 20-45
        // - Range with 'to': 18 to 35, 20 to 45
        // - Plus sign: 18+, 25+
        // - Empty (optional field)

        if (TextUtils.isEmpty(age)) {
            return true; // Age is optional
        }

        // Remove spaces for easier validation
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

    private void proceedToNextActivity(String jobTitle, String companyName, String etexperinece ,String cityName,
                                       String age, String salary, String jobDesc,
                                       String workType, String category, String gender) {
        Intent intent = new Intent(JobCreation.this, JobCreation2.class);


        // Pass data to next activity
        intent.putExtra("jobTitle", jobTitle);
        intent.putExtra("companyName", companyName);
        intent.putExtra("experince", etexperinece);
        intent.putExtra("cityName", cityName);
        intent.putExtra("age", age);
        intent.putExtra("salary", salary);
        intent.putExtra("jobDesc", jobDesc);
        intent.putExtra("workType", workType);
        intent.putExtra("category", category);
        intent.putExtra("gender", gender);

        startActivity(intent);
    }

    private void loadCompanyProfileAndSetDefaults() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        mDatabase.child("companyProfiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    CompanyProfile companyProfile = dataSnapshot.getValue(CompanyProfile.class);
                    if (companyProfile != null) {
                        // Set default values in the form
                        if (TextUtils.isEmpty(etCompanyName.getText().toString())) {
                            etCompanyName.setText(companyProfile.getCompanyName());
                        }
                        if (TextUtils.isEmpty(etCityName.getText().toString())) {
                            etCityName.setText(companyProfile.getCity());
                        }
                        // Set company image if available
                        if (companyProfile.getLogoUrl() != null && !companyProfile.getLogoUrl().isEmpty()) {
                            Glide.with(JobCreation.this)
                                    .load(companyProfile.getLogoUrl())
                                    .placeholder(R.drawable.ic_jobs)
                                    .into(ivCompany);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Silent fail
            }
        });
    }

    private void clearErrors() {
        etJobTitleContainer.setError(null);
        etCompanyNameContainer.setError(null);
        etCityNameContainer.setError(null);
        etAgeContainer.setError(null);
        etSalaryContainer.setError(null);
        etJobDescContainer.setError(null);
    }
}