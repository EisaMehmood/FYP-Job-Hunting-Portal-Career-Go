package com.example.careergo.Auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.careergo.Model.User;
import com.example.careergo.User.UserHome;
import com.example.careergo.databinding.ActivitySignupBinding;
import com.example.careergo.databinding.LoadingDialogBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 2;

    private ActivitySignupBinding binding;
    private Dialog progressDialog;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private boolean approved = false;

    private Uri cvFileUri;
    private String uploadedCvBase64 = "";
    private String selectedRole = "Job Seeker";

    // Supabase Configuration
    private static final String SUPABASE_URL = "https://fyqhxinzpzndrxombsuu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ5cWh4aW56cHpuZHJ4b21ic3V1Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MDg2NjM5OSwiZXhwIjoyMDc2NDQyMzk5fQ.0BbHzCG1MMqm4-hKw0vZTPCI-XXOVC73dDQ_oonYO1Y";
    private static final String SUPABASE_STORAGE_BUCKET = "CareerGO";

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    cvFileUri = result.getData().getData();
                    String fileName = getFileName(cvFileUri);
                    binding.tvCvFileName.setText(fileName);
                    Toast.makeText(this, "File selected: " + fileName, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupProgressDialog();

        binding.btnSignup.setOnClickListener(v -> attemptSignup());
        binding.tvLogin.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));

        binding.btnUploadCv.setOnClickListener(v -> checkPermissionAndOpenFilePicker());
    }

    private void checkPermissionAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot upload CV.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select your CV"));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) result = uri.getLastPathSegment();
        return result != null ? result : "selected_file.pdf";
    }

    // ------------------- Signup Process ------------------- //
    private void attemptSignup() {
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String mobileNo = binding.etMobileNo.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String country = binding.etCountry.getText().toString().trim(); // Add this line
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

        if (TextUtils.isEmpty(mobileNo) || mobileNo.length() < 10) {
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

        // Start the signup process
        if (cvFileUri != null) {
            uploadResumeToSupabase();
        } else {
            performFirebaseSignup(""); // No resume URL
        }
    }

    private void uploadResumeToSupabase() {
        showProgressDialog();

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(cvFileUri);
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);
                inputStream.close();

                String fileName = "resume_" + System.currentTimeMillis() + ".pdf";
                String filePath = "resumes/" + fileName;

                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = RequestBody.create(fileBytes, okhttp3.MediaType.parse("application/pdf"));

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_STORAGE_BUCKET + "/" + filePath)
                        .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .header("apikey", SUPABASE_ANON_KEY)
                        .header("Content-Type", "application/pdf")
                        .put(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        String resumeUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_STORAGE_BUCKET + "/" + filePath;
                        Toast.makeText(SignupActivity.this, "CV uploaded successfully!", Toast.LENGTH_SHORT).show();
                        performFirebaseSignup(resumeUrl);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(SignupActivity.this,
                                "CV upload failed: " + response.code() + " - " + response.message(),
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(SignupActivity.this,
                            "Error uploading CV: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void performFirebaseSignup(String resumeUrl) {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        showProgressDialog();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user, resumeUrl);
                        }
                    } else {
                        hideProgressDialog();
                        Toast.makeText(SignupActivity.this,
                                "Signup failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(FirebaseUser firebaseUser, String resumeUrl) {
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String mobileNo = binding.etMobileNo.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String country = binding.etCountry.getText().toString().trim(); // Add this line
        String city = binding.etCity.getText().toString().trim();

        // Convert CV to Base64 if file is selected but upload failed
        if (cvFileUri != null && resumeUrl.isEmpty()) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(cvFileUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();
                uploadedCvBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
            } catch (Exception e) {
                uploadedCvBase64 = "";
            }
        }

        // Create User object with country field
        User user = new User(firstName, lastName, mobileNo, email, address, country, city, selectedRole, uploadedCvBase64, approved);

        // Add resume URL if available
        if (!resumeUrl.isEmpty()) {
            user.setResumeUrl(resumeUrl);
        }

        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(SignupActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupProgressDialog() {
        progressDialog = new Dialog(this);
        progressDialog.setCancelable(false);
        LoadingDialogBinding loadingBinding = LoadingDialogBinding.inflate(LayoutInflater.from(this));
        progressDialog.setContentView(loadingBinding.getRoot());
    }

    private void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing())
            progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
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