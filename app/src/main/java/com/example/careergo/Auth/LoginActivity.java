package com.example.careergo.Auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.careergo.Admin.AdminHome;
import com.example.careergo.Employer.EmployerHome;
import com.example.careergo.User.UserHome;
import com.example.careergo.databinding.ActivityLoginBinding;
import com.example.careergo.databinding.LoadingDialogBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal; // <<<< ADD THIS IMPORT

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String roleFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        roleFromIntent = getIntent().getStringExtra("role");

        setupProgressDialog();
        setupUIBasedOnRole();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvSignup.setOnClickListener(v -> {
            if ("employer".equalsIgnoreCase(roleFromIntent)) {
                startActivity(new Intent(this, EmployerSignup.class));
            } else if ("student".equalsIgnoreCase(roleFromIntent)) {
                startActivity(new Intent(this, SignupActivity.class));
            }
        });

        binding.tvForgetPassword.setOnClickListener(v -> {
            if (!"admin".equalsIgnoreCase(roleFromIntent)) {
                startActivity(new Intent(this, ForgotPassActivity.class));
            }
        });
    }

    private void setupUIBasedOnRole() {
        if ("admin".equalsIgnoreCase(roleFromIntent)) {
            binding.tvSignup.setVisibility(android.view.View.GONE);
            binding.tvForgetPassword.setVisibility(android.view.View.GONE);
        } else {
            binding.tvSignup.setVisibility(android.view.View.VISIBLE);
            binding.tvForgetPassword.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Valid email required");
            binding.etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password required");
            binding.etPassword.requestFocus();
            return;
        }


        showProgressDialog();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    hideProgressDialog();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // <<<< ADD THIS LINE - SAVE ONESIGNAL ID >>>>>
                            saveOneSignalIdToUserProfile(user.getUid());
                            checkUserRole(user.getUid());
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // <<<< ADD THIS NEW METHOD >>>>>
    private void saveOneSignalIdToUserProfile(String userId) {
        // Get the current OneSignal player ID
        String playerId = OneSignal.getDeviceState() != null ? OneSignal.getDeviceState().getUserId() : null;

        Log.d("OneSignal", "Retrieved Player ID: " + playerId);

        if (playerId != null && !playerId.isEmpty()) {
            // Save OneSignal player ID to user's profile in Firebase
            mDatabase.child(userId).child("oneSignalId").setValue(playerId)
                    .addOnSuccessListener(aVoid -> Log.d("OneSignal", "OneSignal ID saved successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.e("OneSignal", "Error saving OneSignal ID", e));
        } else {
            Log.e("OneSignal", "Player ID is null or empty. Cannot save to Firebase.");
        }
    }



    private void checkUserRole(String uid) {
        showProgressDialog();

        mDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideProgressDialog();

                if (!snapshot.exists()) {
                    Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    return;
                }

                String role = snapshot.child("role").getValue(String.class);
                Boolean approved = snapshot.child("approved").getValue(Boolean.class);

                if ("admin".equalsIgnoreCase(role)) {
                    // Admin login
                    Intent intent = new Intent(LoginActivity.this, AdminHome.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(LoginActivity.this, "Admin login successful!", Toast.LENGTH_SHORT).show();
                } else {
                    // Non-admin → check approval
                    if (approved == null || !approved) {
                        Toast.makeText(LoginActivity.this, "Your account is not approved yet. Please wait for Admin approval.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

                    // Check PIN

                        Intent intent;
                        if ("employer".equalsIgnoreCase(role)) {
                            intent = new Intent(LoginActivity.this, EmployerHome.class);
                        } else {
                            intent = new Intent(LoginActivity.this, UserHome.class);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    }
                }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        LoadingDialogBinding loadingBinding = LoadingDialogBinding.inflate(getLayoutInflater());
        progressDialog.setView(loadingBinding.getRoot());
    }

    private void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }
}