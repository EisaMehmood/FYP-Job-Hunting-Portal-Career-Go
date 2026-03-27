package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.example.careergo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private Switch switchNotifications;
    private Button btnSave;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<String> categoriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        categoriesList = new ArrayList<>();

        initializeViews();
        loadCategoriesFromFirebase();
        loadCurrentPreferences();

        btnSave.setOnClickListener(v -> savePreferences());
    }

    private void initializeViews() {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSave = findViewById(R.id.btnSave);

        // Back button
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
    }

    private void loadCategoriesFromFirebase() {
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                categoriesList.clear();
                categoriesList.add("All Categories"); // Add default option

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    if (category != null && !categoriesList.contains(category)) {
                        categoriesList.add(category);
                    }
                }

                // Set up spinner with loaded categories
                setupCategorySpinner();

                // Load current preferences after categories are loaded
                loadCurrentPreferences();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PreferencesActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                // Set default categories if Firebase fails
                setDefaultCategories();
            }
        });
    }

    private void setDefaultCategories() {
        categoriesList.clear();
        categoriesList.add("All Categories");
        categoriesList.add("Information Technology");
        categoriesList.add("Finance");
        categoriesList.add("Healthcare");
        categoriesList.add("Education");
        categoriesList.add("Marketing");
        categoriesList.add("Retail");
        categoriesList.add("Engineering");
        categoriesList.add("Design");
        categoriesList.add("Sales");
        categoriesList.add("Customer Service");

        setupCategorySpinner();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoriesList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void loadCurrentPreferences() {
        String userId = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(userId).child("preferences")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        // Load notification setting
                        Boolean notificationEnabled = dataSnapshot.child("notificationEnabled").getValue(Boolean.class);
                        if (notificationEnabled != null) {
                            switchNotifications.setChecked(notificationEnabled);
                        }

                        // Load category - wait for spinner to be populated
                        if (spinnerCategory.getAdapter() != null && spinnerCategory.getAdapter().getCount() > 0) {
                            String category = dataSnapshot.child("categories").child("0").getValue(String.class);
                            if (category != null) {
                                setSpinnerSelection(spinnerCategory, category);
                            }
                        }
                    }
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void savePreferences() {
        String userId = mAuth.getCurrentUser().getUid();

        // Get selected values
        String category = spinnerCategory.getSelectedItem().toString();
        boolean notificationEnabled = switchNotifications.isChecked();

        // Create preferences map
        Map<String, Object> preferences = new HashMap<>();

        // Categories list - only store if not "All Categories"
        List<String> categories = new ArrayList<>();
        if (!"All Categories".equals(category)) {
            categories.add(category);
        }
        preferences.put("categories", categories);

        // Notification setting
        preferences.put("notificationEnabled", notificationEnabled);

        // Save to Firebase
        mDatabase.child("users").child(userId).child("preferences")
                .setValue(preferences)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save preferences: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}