package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.Auth.SelectRoleActivity;
import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfile extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    // UI Components
    private CircleImageView profileImage;
    private TextView tvUsername, tvUserEmail;
    private MaterialCardView cvManageAccount, cvUpdateResume, cvLogout , cvProfile , cvpreference;
    private ImageView ivManageAccount, ivUpdateResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeViews() {
        // Image View
        profileImage = findViewById(R.id.profileImage);

        // Text Views
        tvUsername = findViewById(R.id.tvUsername);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        // Card Views
        cvManageAccount = findViewById(R.id.cvManageAccount);
        cvUpdateResume = findViewById(R.id.cvUpdateResume);
        cvProfile = findViewById(R.id.cvManageProfile);
        cvLogout = findViewById(R.id.cvLogout);
        cvpreference= findViewById(R.id.cVpref);

        // Icons
        ivManageAccount = findViewById(R.id.ivManageAccount);
        ivUpdateResume = findViewById(R.id.ivUpdateResume);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Manage Account card
        cvManageAccount.setOnClickListener(v -> {
            openEditProfileActivity();
        });
        cvProfile.setOnClickListener(v -> {
            openProfileActivity();
        });

        // Manage Account arrow icon
        ivManageAccount.setOnClickListener(v -> {
            openEditProfileActivity();
        });

        // Update Resume card
        cvUpdateResume.setOnClickListener(v -> {
            openResumeActivity();
        });
        cvpreference.setOnClickListener(v -> {
            // Create EditUserProfile activity for user profile editing
            Intent intent = new Intent(UserProfile.this, PreferencesActivity.class);
            startActivity(intent);

        });

        // Update Resume arrow icon
        ivUpdateResume.setOnClickListener(v -> {
            openResumeActivity();
        });

        // Logout card
        cvLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // Load user data
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        displayUserData(user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserProfile.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserData(User user) {
        // Set user name
        String fullName = user.getFirstName() + " " + user.getLastName();
        tvUsername.setText(fullName);

        // Set user email
        tvUserEmail.setText(user.getEmail());

        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_image_picker)
                    .error(R.drawable.ic_image_picker)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_image_picker);
        }

        // Update resume card based on CV availability
        updateResumeCard(user);
    }

    private void updateResumeCard(User user) {
        TextView tvUpdateResumeHeading = findViewById(R.id.tvUpdateResumeHeading);
        TextView tvUpdateResumeSubHeading = findViewById(R.id.tvUpdateResumeSubHeading);

        if (user.getCvBase64() != null && !user.getCvBase64().isEmpty()) {
            // User has a resume uploaded
            tvUpdateResumeHeading.setText("View Resume");
            tvUpdateResumeSubHeading.setText("View your uploaded resume");
        } else {
            // User doesn't have a resume
            tvUpdateResumeHeading.setText("Upload Resume");
            tvUpdateResumeSubHeading.setText("Upload your resume for job applications");
        }
    }
    private void openProfileActivity() {
        // Create EditUserProfile activity for user profile editing
        Intent intent = new Intent(UserProfile.this, StudentProfile.class);
        startActivity(intent);
    }


    private void openEditProfileActivity() {
        // Create EditUserProfile activity for user profile editing
        Intent intent = new Intent(UserProfile.this, EditUserProfile.class);
        startActivity(intent);
    }

    private void openResumeActivity() {
        // Check if user has resume uploaded
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        if (user.getCvBase64() != null && !user.getCvBase64().isEmpty()) {
                            // User has resume - open view resume activity
                            openViewResumeActivity();
                        } else {
                            // User doesn't have resume - open upload resume activity
                            openUploadResumeActivity();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Default to upload resume
                openUploadResumeActivity();
            }
        });
    }

    private void openViewResumeActivity() {
        // Create ViewResumeActivity to display uploaded resume
        Intent intent = new Intent(UserProfile.this, UploadResume.class);
        startActivity(intent);

        // For now, show a message
        // Toast.makeText(this, "Opening your resume...", Toast.LENGTH_SHORT).show();
    }

    private void openUploadResumeActivity() {
        // Create UploadResumeActivity for resume upload
        Intent intent = new Intent(UserProfile.this, UploadResume.class);
        startActivity(intent);

        // For now, show a message
        // Toast.makeText(this, "Redirecting to resume upload...", Toast.LENGTH_SHORT).show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logoutUser();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(UserProfile.this, SelectRoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning from edit profile or resume activities
        loadUserData();
    }
}