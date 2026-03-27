package com.example.careergo.Admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Adapters.CategoriesAdapter;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddCategoryActivity extends AppCompatActivity {

    private MaterialCardView cvAddCategory;
    private RecyclerView rvCategories;
    private TextView tvEmptyState;
    private ImageView ivBack;

    private DatabaseReference mDatabase;
    private CategoriesAdapter categoriesAdapter;
    private List<String> categoriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        categoriesList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadCategoriesFromFirebase();
    }

    private void initializeViews() {
        cvAddCategory = findViewById(R.id.cvAddCategory);
        rvCategories = findViewById(R.id.rvCategories);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ivBack = findViewById(R.id.ivBack);
    }

    private void setupRecyclerView() {
        categoriesAdapter = new CategoriesAdapter(categoriesList, new CategoriesAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(String category) {
                // Optional: Show category details or edit
                showCategoryOptions(category);
            }

            @Override
            public void onCategoryDelete(String category) {
                showDeleteConfirmation(category);
            }
        });

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rvCategories.setLayoutManager(layoutManager);
        rvCategories.setAdapter(categoriesAdapter);
    }

    private void setupClickListeners() {
        cvAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void showAddCategoryDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add New Category");

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint("Enter category name");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(categoryName)) {
                addCategoryToFirebase(categoryName);
            } else {
                Toast.makeText(AddCategoryActivity.this, "Please enter a category name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addCategoryToFirebase(String categoryName) {
        // Check if category already exists
        if (categoriesList.contains(categoryName)) {
            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add category to Firebase
        String categoryId = mDatabase.child("categories").push().getKey();
        if (categoryId != null) {
            mDatabase.child("categories").child(categoryId).setValue(categoryName)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddCategoryActivity.this, "Category added successfully", Toast.LENGTH_SHORT).show();
                        // The list will update automatically due to the ValueEventListener
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddCategoryActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadCategoriesFromFirebase() {
        mDatabase.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                categoriesList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    if (category != null && !categoriesList.contains(category)) {
                        categoriesList.add(category);
                    }
                }
                updateEmptyState();
                categoriesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddCategoryActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (categoriesList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvCategories.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvCategories.setVisibility(View.VISIBLE);
        }
    }

    private void showCategoryOptions(String category) {
        String[] options = {"Edit Category", "Delete Category"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(category)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditCategoryDialog(category);
                            break;
                        case 1:
                            showDeleteConfirmation(category);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog(String oldCategory) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Category");

        final EditText input = new EditText(this);
        input.setText(oldCategory);
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newCategoryName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newCategoryName) && !newCategoryName.equals(oldCategory)) {
                updateCategoryInFirebase(oldCategory, newCategoryName);
            } else if (newCategoryName.equals(oldCategory)) {
                Toast.makeText(AddCategoryActivity.this, "No changes made", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddCategoryActivity.this, "Please enter a category name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void updateCategoryInFirebase(String oldCategory, String newCategory) {
        // Find the category in the database and update it
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    if (category != null && category.equals(oldCategory)) {
                        snapshot.getRef().setValue(newCategory)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AddCategoryActivity.this, "Category updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AddCategoryActivity.this, "Failed to update category", Toast.LENGTH_SHORT).show();
                                });
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddCategoryActivity.this, "Failed to update category", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(String category) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '" + category + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCategoryFromFirebase(category);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCategoryFromFirebase(String category) {
        // Find and delete the category from Firebase
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String cat = snapshot.getValue(String.class);
                    if (cat != null && cat.equals(category)) {
                        snapshot.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AddCategoryActivity.this, "Category deleted successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AddCategoryActivity.this, "Failed to delete category", Toast.LENGTH_SHORT).show();
                                });
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddCategoryActivity.this, "Failed to delete category", Toast.LENGTH_SHORT).show();
            }
        });
    }
}