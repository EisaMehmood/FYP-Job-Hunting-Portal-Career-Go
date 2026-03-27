package com.example.careergo.Auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.careergo.R;
import com.example.careergo.databinding.ActivityForgotPassBinding;
import com.example.careergo.databinding.LoadingDialogBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassActivity extends AppCompatActivity {

    private ActivityForgotPassBinding binding;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize view binding
        binding = ActivityForgotPassBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize the custom loading dialog
        setupProgressDialog();

        // Setup click listeners
        binding.btnResetPassword.setOnClickListener(v -> attemptPasswordReset());

        binding.btnBackToLogin.setOnClickListener(v -> finish()); // Go back to the previous activity
    }

    /**
     * Handles the password reset process.
     */
    private void attemptPasswordReset() {
        // Clear previous errors
        binding.etEmailContainer.setError(null);

        // Get and trim the email input
        String email = binding.etEmail.getText().toString().trim();

        // Validation check for email
        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            binding.etEmailContainer.setError("Please enter a valid email address.");
            binding.etEmailContainer.requestFocus();
            return;
        }

        // Show the loading dialog before starting the network task
        showProgressDialog();

        // Use Firebase Auth to send the password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Hide the loading dialog when the task is complete
                    hideProgressDialog();

                    if (task.isSuccessful()) {
                        // Password reset email sent successfully
                        Toast.makeText(this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                        // Optionally, navigate back to the login screen
                        // Navigate to EmailActivity
                        Intent intent = new Intent(ForgotPassActivity.this, EmailActivity.class);
                        intent.putExtra("email", email); // optionally pass email to next activity
                        startActivity(intent);
                        finish();
                    } else {
                        // Failed to send password reset email (e.g., email not found)
                        Toast.makeText(this, "Failed to send reset email. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Initializes and configures the custom loading dialog.
     */
    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        // Use a new binding class for the loading dialog layout
        LoadingDialogBinding loadingBinding = LoadingDialogBinding.inflate(LayoutInflater.from(this));
        progressDialog.setView(loadingBinding.getRoot());
    }

    /**
     * Shows the custom loading dialog.
     */
    private void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    /**
     * Hides the custom loading dialog.
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Checks if the email is in a valid format.
     */
    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
