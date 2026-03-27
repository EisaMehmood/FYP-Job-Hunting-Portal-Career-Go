package com.example.careergo.Employer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.careergo.Auth.LoginActivity;
import com.example.careergo.Auth.SelectRoleActivity;
import com.example.careergo.Model.CompanyProfile;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployerProfile extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Components
    private CircleImageView profileImage;
    private TextView tvUsername, tvUserEmail;
    private MaterialCardView cvManageAccount, cvLogout;
    private ImageView ivManageAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_profile);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        // Image View
        profileImage = findViewById(R.id.profileImage);

        // Text Views
        tvUsername = findViewById(R.id.tvUsername);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        // Card Views
        cvManageAccount = findViewById(R.id.cvManageAccount);
        cvLogout = findViewById(R.id.cvLogout);

        // Icons
        ivManageAccount = findViewById(R.id.ivManageAccount);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Manage Account card
        cvManageAccount.setOnClickListener(v -> {
            Intent intent = new Intent(EmployerProfile.this, EditEmployerProfile.class);
            startActivity(intent);
        });

        // Manage Account arrow icon
        ivManageAccount.setOnClickListener(v -> {
            Intent intent = new Intent(EmployerProfile.this, EditEmployerProfile.class);
            startActivity(intent);
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
                // Load company profile for additional info if needed
                loadCompanyProfile(userId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EmployerProfile.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
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
    }

    private void loadCompanyProfile(String userId) {
        mDatabase.child("companyProfiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    CompanyProfile companyProfile = dataSnapshot.getValue(CompanyProfile.class);
                    if (companyProfile != null) {
                        // You can display company info here if needed
                        // For example, show company name under user email
                        displayCompanyInfo(companyProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Silent fail - company profile is optional
            }
        });
    }

    private void displayCompanyInfo(CompanyProfile companyProfile) {
        // You can add company information to the profile display if desired
        // For example:
        // String companyInfo = companyProfile.getCompanyName() + " • " + companyProfile.getIndustry();
        // tvCompanyInfo.setText(companyInfo);
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
        Intent intent = new Intent(EmployerProfile.this, SelectRoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning from edit profile
        loadUserData();
    }
}