package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.Model.CompanyProfile;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.example.careergo.Utility.SupabaseStorageService;
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditEmployerProfile extends AppCompatActivity {

    // Firebase (for Auth and Realtime Database)
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Supabase Storage Configuration
    private static final String SUPABASE_URL = "https://fyqhxinzpzndrxombsuu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ5cWh4aW56cHpuZHJ4b21ic3V1Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MDg2NjM5OSwiZXhwIjoyMDc2NDQyMzk5fQ.0BbHzCG1MMqm4-hKw0vZTPCI-XXOVC73dDQ_oonYO1Y";
    private static final String SUPABASE_STORAGE_BUCKET = "CareerGO";

    // UI Components
    private CircleImageView profileImage;
    private TextInputEditText etUsername, etEmail, etMobile, etCompanyName, etCompanyWebsite,
            etCompanyDescription, etCompanyAddress, etCompanyCity, etCompanyState,
            etCompanyPincode, etIndustry, etCompanySize;
    private TextInputLayout etUsernameContainer, etEmailContainer, etMobileContainer,
            etCompanyNameContainer, etCompanyWebsiteContainer, etCompanyDescriptionContainer,
            etCompanyAddressContainer, etCompanyCityContainer, etCompanyStateContainer,
            etCompanyPincodeContainer, etIndustryContainer, etCompanySizeContainer;
    private MaterialButton btnSubmit;

    // Data
    private Uri profileImageUri;
    private String uploadedImageUrl = "";
    private ProgressDialog progressDialog;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "EditEmployerProfile";

    // Current user data
    private User currentUserData;
    private CompanyProfile currentCompanyProfile;

    // Supabase Service
    private SupabaseStorageService supabaseStorageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_employer_profile);

        initializeFirebase();
        initializeSupabase();
        initializeViews();
        setupClickListeners();
        setupProgressDialog();
        loadUserData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeSupabase() {
        // Set up logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        supabaseStorageService = retrofit.create(SupabaseStorageService.class);
    }

    private void initializeViews() {
        // Image View
        profileImage = findViewById(R.id.profileImage);

        // Personal Info Fields
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);

        // Personal Info Containers
        etUsernameContainer = findViewById(R.id.etUsernameContainer);
        etEmailContainer = findViewById(R.id.etEmailContainer);
        etMobileContainer = findViewById(R.id.etMobileContainer);

        // Company Info Fields
        etCompanyName = findViewById(R.id.etCompanyName);
        etCompanyWebsite = findViewById(R.id.etCompanyWebsite);
        etCompanyDescription = findViewById(R.id.etCompanyDescription);
        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        etCompanyCity = findViewById(R.id.etCompanyCity);
        etCompanyState = findViewById(R.id.etCompanyState);
        etCompanyPincode = findViewById(R.id.etCompanyPincode);
        etIndustry = findViewById(R.id.etIndustry);
        etCompanySize = findViewById(R.id.etCompanySize);

        // Company Info Containers
        etCompanyNameContainer = findViewById(R.id.etCompanyNameContainer);
        etCompanyWebsiteContainer = findViewById(R.id.etCompanyWebsiteContainer);
        etCompanyDescriptionContainer = findViewById(R.id.etCompanyDescriptionContainer);
        etCompanyAddressContainer = findViewById(R.id.etCompanyAddressContainer);
        etCompanyCityContainer = findViewById(R.id.etCompanyCityContainer);
        etCompanyStateContainer = findViewById(R.id.etCompanyStateContainer);
        etCompanyPincodeContainer = findViewById(R.id.etCompanyPincodeContainer);
        etIndustryContainer = findViewById(R.id.etIndustryContainer);
        etCompanySizeContainer = findViewById(R.id.etCompanySizeContainer);

        // Button
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Delete account button
        findViewById(R.id.ivDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());

        // Profile image picker
        profileImage.setOnClickListener(v -> openImagePicker());
        findViewById(R.id.tvImagePicker).setOnClickListener(v -> openImagePicker());

        // Submit button
        btnSubmit.setOnClickListener(v -> validateAndUpdateProfile());
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

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            profileImageUri = data.getData();
            Glide.with(this)
                    .load(profileImageUri)
                    .placeholder(R.drawable.ic_image_picker)
                    .into(profileImage);
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showProgressDialog("Loading profile...");

        String userId = currentUser.getUid();

        // Load user data
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentUserData = dataSnapshot.getValue(User.class);
                    if (currentUserData != null) {
                        populateUserData(currentUserData);
                    }
                }
                // Load company profile data
                loadCompanyProfile(userId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                hideProgressDialog();
                Toast.makeText(EditEmployerProfile.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCompanyProfile(String userId) {
        mDatabase.child("companyProfiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                hideProgressDialog();
                if (dataSnapshot.exists()) {
                    currentCompanyProfile = dataSnapshot.getValue(CompanyProfile.class);
                    if (currentCompanyProfile != null) {
                        populateCompanyData(currentCompanyProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                hideProgressDialog();
                Toast.makeText(EditEmployerProfile.this, "Failed to load company profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUserData(User user) {
        // Personal information
        String fullName = user.getFirstName() + " " + user.getLastName();
        etUsername.setText(fullName);
        etEmail.setText(user.getEmail());
        etMobile.setText(user.getMobileNo());

        // Load profile image if exists
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_image_picker)
                    .into(profileImage);
            uploadedImageUrl = user.getProfileImageUrl();
        }
    }

    private void populateCompanyData(CompanyProfile companyProfile) {
        // Company information
        if (companyProfile.getCompanyName() != null) {
            etCompanyName.setText(companyProfile.getCompanyName());
        }
        if (companyProfile.getWebsite() != null) {
            etCompanyWebsite.setText(companyProfile.getWebsite());
        }
        if (companyProfile.getDescription() != null) {
            etCompanyDescription.setText(companyProfile.getDescription());
        }
        if (companyProfile.getAddress() != null) {
            etCompanyAddress.setText(companyProfile.getAddress());
        }
        if (companyProfile.getCity() != null) {
            etCompanyCity.setText(companyProfile.getCity());
        }
        if (companyProfile.getState() != null) {
            etCompanyState.setText(companyProfile.getState());
        }
        if (companyProfile.getPincode() != null) {
            etCompanyPincode.setText(companyProfile.getPincode());
        }
        if (companyProfile.getIndustry() != null) {
            etIndustry.setText(companyProfile.getIndustry());
        }
        if (companyProfile.getCompanySize() != null) {
            etCompanySize.setText(companyProfile.getCompanySize());
        }
    }

    private void validateAndUpdateProfile() {
        // Personal info validation
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        // Company info validation
        String companyName = etCompanyName.getText().toString().trim();
        String companyWebsite = etCompanyWebsite.getText().toString().trim();
        String companyDescription = etCompanyDescription.getText().toString().trim();
        String companyAddress = etCompanyAddress.getText().toString().trim();
        String companyCity = etCompanyCity.getText().toString().trim();
        String companyState = etCompanyState.getText().toString().trim();
        String companyPincode = etCompanyPincode.getText().toString().trim();
        String industry = etIndustry.getText().toString().trim();
        String companySize = etCompanySize.getText().toString().trim();

        boolean isValid = true;

        // Clear previous errors
        clearErrors();

        // Validate personal information
        if (TextUtils.isEmpty(username)) {
            etUsernameContainer.setError("Full name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmailContainer.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailContainer.setError("Please enter a valid email address");
            isValid = false;
        }

        if (TextUtils.isEmpty(mobile)) {
            etMobileContainer.setError("Mobile number is required");
            isValid = false;
        } else if (mobile.length() < 10) {
            etMobileContainer.setError("Please enter a valid mobile number");
            isValid = false;
        }

        // Validate company information
        if (TextUtils.isEmpty(companyName)) {
            etCompanyNameContainer.setError("Company name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(companyAddress)) {
            etCompanyAddressContainer.setError("Company address is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(companyCity)) {
            etCompanyCityContainer.setError("City is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(companyState)) {
            etCompanyStateContainer.setError("State is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(companyPincode)) {
            etCompanyPincodeContainer.setError("Pincode is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(industry)) {
            etIndustryContainer.setError("Industry is required");
            isValid = false;
        }

        if (isValid) {
            if (profileImageUri != null) {
                // Only upload image if a new one is selected
                uploadImageToSupabase(username, email, mobile, companyName, companyWebsite,
                        companyDescription, companyAddress, companyCity, companyState,
                        companyPincode, industry, companySize);
            } else {
                // Update profile without changing image
                updateProfile(username, email, mobile, companyName, companyWebsite, companyDescription,
                        companyAddress, companyCity, companyState, companyPincode, industry,
                        companySize, uploadedImageUrl);
            }
        }
    }

    private void uploadImageToSupabase(String username, String email, String mobile,
                                       String companyName, String companyWebsite, String companyDescription,
                                       String companyAddress, String companyCity, String companyState,
                                       String companyPincode, String industry, String companySize) {
        showProgressDialog("Uploading profile image...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            hideProgressDialog();
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";

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
                            updateProfile(username, email, mobile, companyName, companyWebsite,
                                    companyDescription, companyAddress, companyCity, companyState,
                                    companyPincode, industry, companySize, uploadedImageUrl);
                        });
                    } else {
                        runOnUiThread(() -> {
                            hideProgressDialog();
                            Toast.makeText(EditEmployerProfile.this,
                                    "Upload failed: " + response.code() + " " + response.message(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    response.close();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(EditEmployerProfile.this,
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

    private void updateProfile(String username, String email, String mobile,
                               String companyName, String companyWebsite, String companyDescription,
                               String companyAddress, String companyCity, String companyState,
                               String companyPincode, String industry, String companySize, String imageUrl) {
        showProgressDialog("Updating profile...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            hideProgressDialog();
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Split username into first and last name
        String[] nameParts = username.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Update user data using Map to preserve existing fields
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("firstName", firstName);
        userUpdates.put("lastName", lastName);
        userUpdates.put("mobileNo", mobile);
        userUpdates.put("email", email);

        // Only update profile image URL if we have a new one
        if (imageUrl != null && !imageUrl.isEmpty()) {
            userUpdates.put("profileImageUrl", imageUrl);
        }

        // Update company profile using Map
        Map<String, Object> companyUpdates = new HashMap<>();
        companyUpdates.put("companyName", companyName);
        companyUpdates.put("website", companyWebsite);
        companyUpdates.put("description", companyDescription);
        companyUpdates.put("address", companyAddress);
        companyUpdates.put("city", companyCity);
        companyUpdates.put("state", companyState);
        companyUpdates.put("pincode", companyPincode);
        companyUpdates.put("industry", industry);
        companyUpdates.put("companySize", companySize);

        // Only update company logo if we have a new image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            companyUpdates.put("logoUrl", imageUrl);
        }

        // Update both user and company profile
        mDatabase.child("users").child(userId).updateChildren(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("companyProfiles").child(userId).updateChildren(companyUpdates)
                            .addOnSuccessListener(aVoid1 -> {
                                hideProgressDialog();
                                Toast.makeText(EditEmployerProfile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                hideProgressDialog();
                                Toast.makeText(EditEmployerProfile.this, "Failed to update company profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(EditEmployerProfile.this, "Failed to update user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAccount();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        showProgressDialog("Deleting account...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            hideProgressDialog();
            return;
        }

        String userId = currentUser.getUid();

        // Delete user data from Realtime Database
        mDatabase.child("users").child(userId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("companyProfiles").child(userId).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                // Delete user from Authentication
                                currentUser.delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            hideProgressDialog();
                                            Toast.makeText(EditEmployerProfile.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(EditEmployerProfile.this, LoginActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            hideProgressDialog();
                                            Toast.makeText(EditEmployerProfile.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                hideProgressDialog();
                                Toast.makeText(EditEmployerProfile.this, "Failed to delete company profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(EditEmployerProfile.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearErrors() {
        etUsernameContainer.setError(null);
        etEmailContainer.setError(null);
        etMobileContainer.setError(null);
        etCompanyNameContainer.setError(null);
        etCompanyWebsiteContainer.setError(null);
        etCompanyDescriptionContainer.setError(null);
        etCompanyAddressContainer.setError(null);
        etCompanyCityContainer.setError(null);
        etCompanyStateContainer.setError(null);
        etCompanyPincodeContainer.setError(null);
        etIndustryContainer.setError(null);
        etCompanySizeContainer.setError(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}