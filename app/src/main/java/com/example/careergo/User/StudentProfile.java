package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.skydoves.powerspinner.PowerSpinnerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentProfile extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView tvImagePicker;
    private ImageView ivBack;
    private EditText etFullName, etEmail, etMobile, etDate, etBio, etCnic;
    private PowerSpinnerView genderSpinner;
    private Button btnNext;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    private Calendar selectedDate;


    private boolean isFormatting = false; // Flag to prevent infinite loop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

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
        profileImage = findViewById(R.id.profileImage);
        tvImagePicker = findViewById(R.id.tvImagePicker);
        ivBack = findViewById(R.id.ivBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etDate = findViewById(R.id.etDate);
        etBio = findViewById(R.id.etBio);
        etCnic = findViewById(R.id.etCnic);
        genderSpinner = findViewById(R.id.genderSpinner);
        btnNext = findViewById(R.id.btnNext);


        selectedDate = Calendar.getInstance();

        // Hide image picker functionality since we're loading from Firebase
        tvImagePicker.setVisibility(View.GONE);
        profileImage.setEnabled(false);

        // Back button click
        ivBack.setOnClickListener(v -> finish());

        // Date picker
        etDate.setOnClickListener(v -> showDatePickerDialog());
        etDate.setFocusable(false);

        // CNIC formatting - Fixed version
        etCnic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) {
                    return;
                }

                isFormatting = true;

                try {
                    String original = s.toString();

                    // Remove all non-digits
                    String digitsOnly = original.replaceAll("[^\\d]", "");

                    // Limit to 13 digits
                    if (digitsOnly.length() > 13) {
                        digitsOnly = digitsOnly.substring(0, 13);
                    }

                    // Format with dashes - correct positions
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < digitsOnly.length(); i++) {
                        // Add dash after 5th digit (position 5)
                        if (i == 5) {
                            formatted.append("-");
                        }
                        // Add dash after 12th digit (position 12)
                        if (i == 12) {
                            formatted.append("-");
                        }
                        formatted.append(digitsOnly.charAt(i));
                    }

                    // Update text only if it's different
                    if (!original.equals(formatted.toString())) {
                        etCnic.setText(formatted.toString());
                        etCnic.setSelection(formatted.length());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isFormatting = false;
                }
            }
        });

        // Next button click
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateInView();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateDateInView() {
        String dateFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void validateAndProceed() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String cnic = etCnic.getText().toString().trim();
        String dob = etDate.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String gender = genderSpinner.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(mobile) || mobile.length() < 10) {
            etMobile.setError("Valid mobile number is required");
            hasError = true;
        }

        // CNIC validation (13 digits after removing dashes)
        String cnicDigits = cnic.replaceAll("[^\\d]", "");
        if (TextUtils.isEmpty(cnic) || cnicDigits.length() != 13) {
            etCnic.setError("Valid 13-digit CNIC is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(dob)) {
            etDate.setError("Date of birth is required");
            hasError = true;
        }

        if (TextUtils.isEmpty(gender) || gender.equals("Gender")) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if (hasError) {
            return;
        }

        savePersonalInfo(fullName, email, mobile, cnic, dob, gender, bio);
    }

    private void savePersonalInfo(String fullName, String email, String mobile, String cnic, String dob, String gender, String bio) {
        Map<String, Object> personalInfo = new HashMap<>();
        personalInfo.put("fullName", fullName);
        personalInfo.put("email", email);
        personalInfo.put("mobile", mobile);
        personalInfo.put("cnic", cnic);
        personalInfo.put("dateOfBirth", dob);
        personalInfo.put("gender", gender);
        personalInfo.put("bio", bio);
        personalInfo.put("profileCompleted", true);

        mDatabase.child("users").child(currentUser.getUid()).child("personalInfo")
                .setValue(personalInfo)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(StudentProfile.this, StudentProfile2.class);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save personal information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingData() {
        // Load user's basic info from Firebase
        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Load profile image from Firebase
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(StudentProfile.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_image_picker)
                                .into(profileImage);
                    }

                    // Load name and mobile from user data
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String mobile = snapshot.child("mobileNo").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    if (firstName != null && lastName != null) {
                        etFullName.setText(firstName + " " + lastName);
                    }

                    if (mobile != null) {
                        etMobile.setText(mobile);
                    }

                    if (email != null) {
                        etEmail.setText(email);
                    } else {
                        etEmail.setText(currentUser.getEmail());
                    }
                } else {
                    // If no user data exists, at least set the email from Firebase Auth
                    etEmail.setText(currentUser.getEmail());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Set email from Firebase Auth as fallback
                etEmail.setText(currentUser.getEmail());
            }
        });

        // Load personal info if exists
        mDatabase.child("users").child(currentUser.getUid()).child("personalInfo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String fullName = dataSnapshot.child("fullName").getValue(String.class);
                            String email = dataSnapshot.child("email").getValue(String.class);
                            String mobile = dataSnapshot.child("mobile").getValue(String.class);
                            String cnic = dataSnapshot.child("cnic").getValue(String.class);
                            String dob = dataSnapshot.child("dateOfBirth").getValue(String.class);
                            String gender = dataSnapshot.child("gender").getValue(String.class);
                            String bio = dataSnapshot.child("bio").getValue(String.class);

                            if (fullName != null) etFullName.setText(fullName);
                            if (email != null) etEmail.setText(email);
                            if (mobile != null) etMobile.setText(mobile);
                            if (cnic != null) etCnic.setText(cnic);
                            if (dob != null) etDate.setText(dob);
                            if (gender != null) genderSpinner.setText(gender);
                            if (bio != null) etBio.setText(bio);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentProfile.this, "Failed to load personal info", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}