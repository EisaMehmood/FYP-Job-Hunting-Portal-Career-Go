package com.example.careergo.User;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.example.careergo.databinding.ActivityEditUserProfileBinding;
import com.example.careergo.databinding.LoadingDialogBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditUserProfile extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 2;
    private static final int PICK_IMAGE_REQUEST = 1;

    // Supabase Storage Configuration
    private static final String SUPABASE_URL = "https://fyqhxinzpzndrxombsuu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ5cWh4aW56cHpuZHJ4b21ic3V1Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MDg2NjM5OSwiZXhwIjoyMDc2NDQyMzk5fQ.0BbHzCG1MMqm4-hKw0vZTPCI-XXOVC73dDQ_oonYO1Y";
    private static final String SUPABASE_STORAGE_BUCKET = "CareerGO";
    private static final String TAG = "EditUserProfile";

    private ActivityEditUserProfileBinding binding;
    private Dialog progressDialog;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    private Uri profileImageUri;
    private String currentUserId;
    private String uploadedImageUrl = "";

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    profileImageUri = result.getData().getData();
                    binding.profileImage.setImageURI(profileImageUri);
                    Toast.makeText(this, "Profile image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserData();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupProgressDialog();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.ivPopOut.setOnClickListener(v -> onBackPressed());

        binding.ivDeleteStudent.setOnClickListener(v -> showDeleteConfirmationDialog());

        binding.tvImagePicker.setOnClickListener(v -> checkPermissionAndOpenImagePicker());

        binding.profileImage.setOnClickListener(v -> checkPermissionAndOpenImagePicker());

        binding.btnSaveChange.setOnClickListener(v -> attemptUpdateProfile());
    }

    private void loadUserData() {
        showProgressDialog();

        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideProgressDialog();
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        populateUserData(user);
                    }
                } else {
                    Toast.makeText(EditUserProfile.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
                Toast.makeText(EditUserProfile.this, "Failed to load user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUserData(User user) {
        binding.etUsername.setText(user.getFirstName() + " " + user.getLastName());
        binding.etEmail.setText(user.getEmail());
        binding.etMobile.setText(user.getMobileNo());

        // Load profile image if exists
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_image_picker)
                    .into(binding.profileImage);
            uploadedImageUrl = user.getProfileImageUrl();
        }
    }

    private void checkPermissionAndOpenImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openImagePicker();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
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
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
    }

    private void attemptUpdateProfile() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String mobile = binding.etMobile.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Clear previous errors
        binding.etUsernameContainer.setError(null);
        binding.etEmailContainer.setError(null);
        binding.etMobileContainer.setError(null);

        // Validation
        if (TextUtils.isEmpty(username)) {
            binding.etUsernameContainer.setError("Username is required.");
            focusView = binding.etUsernameContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            binding.etEmailContainer.setError("Please enter a valid email address.");
            focusView = binding.etEmailContainer;
            cancel = true;
        }

        if (TextUtils.isEmpty(mobile) || mobile.length() < 10) {
            binding.etMobileContainer.setError("Please enter a valid mobile number.");
            focusView = binding.etMobileContainer;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) focusView.requestFocus();
            return;
        }

        if (profileImageUri != null) {
            // Upload image first, then update profile
            uploadImageToSupabase(username, email, mobile);
        } else {
            // Update profile without changing image
            updateUserProfile(username, email, mobile, uploadedImageUrl);
        }
    }

    private void uploadImageToSupabase(String username, String email, String mobile) {
        showProgressDialog("Uploading profile image...");

        String fileName = "profile_" + currentUserId + "_" + System.currentTimeMillis() + ".jpg";

        try {
            InputStream inputStream = getContentResolver().openInputStream(profileImageUri);
            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            inputStream.close();

            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                    fileBytes,
                    okhttp3.MediaType.parse("image/jpeg")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_STORAGE_BUCKET + "/" + fileName)
                    .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Content-Type", "image/jpeg")
                    .put(requestBody)
                    .build();

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

            new Thread(() -> {
                try {
                    okhttp3.Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        uploadedImageUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_STORAGE_BUCKET + "/" + fileName;

                        runOnUiThread(() -> {
                            Log.d(TAG, "Image uploaded: " + uploadedImageUrl);
                            updateUserProfile(username, email, mobile, uploadedImageUrl);
                        });
                    } else {
                        runOnUiThread(() -> {
                            hideProgressDialog();
                            Toast.makeText(EditUserProfile.this,
                                    "Upload failed: " + response.code() + " " + response.message(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    response.close();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(EditUserProfile.this,
                                "Upload error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            hideProgressDialog();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfile(String username, String email, String mobile, String imageUrl) {
        showProgressDialog("Updating profile...");

        // Split username into first and last name
        String[] nameParts = username.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Update user data in Firebase using Map to preserve existing fields
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User existingUser = snapshot.getValue(User.class);
                    if (existingUser != null) {
                        // Create update map to preserve existing fields
                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("firstName", firstName);
                        userUpdates.put("lastName", lastName);
                        userUpdates.put("mobileNo", mobile);
                        userUpdates.put("email", email);

                        // Only update profile image URL if we have a new one
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            userUpdates.put("profileImageUrl", imageUrl);
                        }

                        // Preserve existing fields
                        userUpdates.put("address", existingUser.getAddress());
                        userUpdates.put("city", existingUser.getCity());
                        userUpdates.put("role", existingUser.getRole());
                        userUpdates.put("cvBase64", existingUser.getCvBase64());
                        userUpdates.put("approved", existingUser.isApproved());

                        // Save updated data
                        mDatabase.child("users").child(currentUserId).updateChildren(userUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    hideProgressDialog();
                                    Toast.makeText(EditUserProfile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                                    // Update email in Firebase Auth if changed
                                    if (!email.equals(currentUser.getEmail())) {
                                        updateEmailInAuth(email);
                                    } else {
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hideProgressDialog();
                                    Toast.makeText(EditUserProfile.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
                Toast.makeText(EditUserProfile.this, "Failed to update profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmailInAuth(String newEmail) {
        currentUser.updateEmail(newEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditUserProfile.this, "Email updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditUserProfile.this, "Profile updated but email update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                    finish();
                });
    }

    private void showDeleteConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        showProgressDialog();

        // Delete user data from Realtime Database
        mDatabase.child("users").child(currentUserId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Delete user from Firebase Auth
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                hideProgressDialog();
                                if (task.isSuccessful()) {
                                    Toast.makeText(EditUserProfile.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(EditUserProfile.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(EditUserProfile.this, "Failed to delete account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(EditUserProfile.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showProgressDialog(String message) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            // You might want to update the message in your custom dialog
            // For now, we'll just show the dialog
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}