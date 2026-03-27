package com.example.careergo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.careergo.Admin.AdminHome;
import com.example.careergo.Auth.SelectRoleActivity;
import com.example.careergo.Employer.EmployerHome;
import com.example.careergo.User.UserHome;
import com.example.careergo.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        setupUI();
    }

    private void setupUI() {
        // Spannable heading
        SpannableString headingText = new SpannableString(getString(R.string.on_boarding_heading));
        int color = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = getColor(R.color.on_boarding_span_text_color);
        }
        headingText.setSpan(new UnderlineSpan(), 10, 20, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        headingText.setSpan(new ForegroundColorSpan(color), 10, 20, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        binding.onBoardingHeading.setText(headingText);

        // FAB click
        binding.ivFab.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // No logged-in user → go to role selection
                startActivity(new Intent(this, SelectRoleActivity.class));
                finish();
            } else {
                // Logged-in user → check role & approval
                checkUserRole(currentUser.getUid());
            }
        });
    }

    private void checkUserRole(String uid) {
        mDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(MainActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    startActivity(new Intent(MainActivity.this, SelectRoleActivity.class));
                    finish();
                    return;
                }

                String role = snapshot.child("role").getValue(String.class);
                Boolean approved = snapshot.child("approved").getValue(Boolean.class);
                Intent intent;

                if ("admin".equalsIgnoreCase(role)) {
                    // Admin → direct to AdminHome
                    intent = new Intent(MainActivity.this, AdminHome.class);
                } else {
                    // Non-admin → check approval
                    if (approved == null || !approved) {
                        Toast.makeText(MainActivity.this, "Your account is not approved yet. Please wait for Admin approval.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        startActivity(new Intent(MainActivity.this, SelectRoleActivity.class));
                        finish();
                        return;
                    }

                    // Approved users → role-based navigation
                    if ("employer".equalsIgnoreCase(role)) {
                        intent = new Intent(MainActivity.this, EmployerHome.class);
                    } else {
                        intent = new Intent(MainActivity.this, UserHome.class);
                    }
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
