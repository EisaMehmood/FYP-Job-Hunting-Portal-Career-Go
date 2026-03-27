package com.example.careergo.Auth;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.careergo.Employer.EmployerHome;
import com.example.careergo.Model.User;
import com.example.careergo.databinding.ActivityEmployerSignupBinding;
import com.example.careergo.databinding.LoadingDialogBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EmployerSignup extends AppCompatActivity {

    private ActivityEmployerSignupBinding binding;
    private Dialog progressDialog;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private  boolean approved = false;
    private final String selectedRole = "Employer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEmployerSignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupProgressDialog();

        binding.btnSignup.setOnClickListener(v -> attemptSignup());
        binding.tvLogin.setOnClickListener(v ->
                startActivity(new Intent(EmployerSignup.this, LoginActivity.class)));
    }

    private void attemptSignup() {
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String mobileNo = binding.etMobileNo.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String country = binding.etCountry.getText().toString().trim(); 
        String city = binding.etCity.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Validation for all fields including country
        if (TextUtils.isEmpty(city)) {
            binding.etCityContainer.setError("City is required.");
            focusView = binding.etCityContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(country)) {
            binding.etCountryContainer.setError("Country is required."); // Add this validation
            focusView = binding.etCountryContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(address)) {
            binding.etAddressContainer.setError("Address is required.");
            focusView = binding.etAddressContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            binding.etPasswordContainer.setError("Password must be at least 6 characters.");
            focusView = binding.etPasswordContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            binding.etEmailContainer.setError("Please enter a valid email address.");
            focusView = binding.etEmailContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(mobileNo) || mobileNo.length() < 11) {
            binding.etMobileNoContainer.setError("Please enter a valid mobile number.");
            focusView = binding.etMobileNoContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(lastName)) {
            binding.etLastNameContainer.setError("Last name is required.");
            focusView = binding.etLastNameContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(firstName)) {
            binding.etFirstNameContainer.setError("First name is required.");
            focusView = binding.etFirstNameContainer;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) focusView.requestFocus();
            return;
        }

        performFirebaseSignup(email, password, firstName, lastName, mobileNo, address, country, city);
    }

    private void performFirebaseSignup(String email, String password, String firstName, String lastName,
                                       String mobileNo, String address, String country, String city) {
        showProgressDialog();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null)
                            saveUserData(user, firstName, lastName, mobileNo, email, address, country, city);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(EmployerSignup.this, "Signup failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(FirebaseUser firebaseUser, String firstName, String lastName,
                              String mobileNo, String email, String address, String country, String city ) {

        // Create User object with country field
        User user = new User(firstName, lastName, mobileNo, email, address, country, city, selectedRole, "", approved);

        // ✅ Save under "users"
        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    Intent intent = new Intent(EmployerSignup.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(EmployerSignup.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(EmployerSignup.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupProgressDialog() {
        progressDialog = new Dialog(this);
        progressDialog.setCancelable(false);
        LoadingDialogBinding loadingBinding = LoadingDialogBinding.inflate(LayoutInflater.from(this));
        progressDialog.setContentView(loadingBinding.getRoot());
    }

    private void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    private boolean isPinValid(String pin) {
        return pin.length() == 4 && TextUtils.isDigitsOnly(pin);
    }
}