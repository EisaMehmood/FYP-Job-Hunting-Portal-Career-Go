package com.example.careergo.User;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.careergo.Model.User;
import com.example.careergo.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadResume extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Supabase Configuration
    private static final String SUPABASE_URL = "https://fyqhxinzpzndrxombsuu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ5cWh4aW56cHpuZHJ4b21ic3V1Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MDg2NjM5OSwiZXhwIjoyMDc2NDQyMzk5fQ.0BbHzCG1MMqm4-hKw0vZTPCI-XXOVC73dDQ_oonYO1Y";
    private static final String SUPABASE_STORAGE_BUCKET = "CareerGO";

    // UI Components
    private View cvUploadContainer;
    private ImageView ivFile;
    private TextView tvUploadFile, tvFileName, tvUploadDate;
    private MaterialButton btnViewResume, btnDeleteResume, btnUploadNew;

    // Data
    private Uri resumeFileUri;
    private ProgressDialog progressDialog;
    private static final int PICK_PDF_REQUEST = 1;
    private boolean hasExistingResume = false;
    private String currentResumeUrl = "";
    private String currentResumePath = ""; // Store the file path for deletion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_resume);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupProgressDialog();
        checkExistingResume();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // Removed Firebase Storage reference since we're using Supabase
    }

    private void initializeViews() {
        // The upload container is actually an include with id uploadContainer
        cvUploadContainer = findViewById(R.id.uploadContainer);

        // Now find the views within the included layout
        if (cvUploadContainer != null) {
            ivFile = cvUploadContainer.findViewById(R.id.ivFile);
            tvUploadFile = cvUploadContainer.findViewById(R.id.tvUploadFile);
        } else {
            Log.e("UploadResume", "uploadContainer is null");
        }

        // These are in the main layout
        tvFileName = findViewById(R.id.tvFileName);
        tvUploadDate = findViewById(R.id.tvUploadDate);
        btnViewResume = findViewById(R.id.btnViewResume);
        btnDeleteResume = findViewById(R.id.btnDeleteResume);
        btnUploadNew = findViewById(R.id.btnUploadNew);

        // Add null checks
        if (ivFile == null) Log.e("UploadResume", "ivFile is null - check upload_pdf_view layout");
        if (tvUploadFile == null) Log.e("UploadResume", "tvUploadFile is null - check upload_pdf_view layout");
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.ivPopOut).setOnClickListener(v -> finish());

        // Upload container click (only if no resume exists)
        cvUploadContainer.setOnClickListener(v -> {
            if (!hasExistingResume) {
                openFilePicker();
            }
        });

        // Upload file text click
        tvUploadFile.setOnClickListener(v -> {
            if (!hasExistingResume) {
                openFilePicker();
            }
        });

        // File icon click
        ivFile.setOnClickListener(v -> {
            if (!hasExistingResume) {
                openFilePicker();
            }
        });

        // View Resume button
        btnViewResume.setOnClickListener(v -> viewResume());

        // Delete Resume button
        btnDeleteResume.setOnClickListener(v -> deleteResume());

        // Upload New button
        btnUploadNew.setOnClickListener(v -> openFilePicker());
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void checkExistingResume() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        if (user.getResumeUrl() != null && !user.getResumeUrl().isEmpty()) {
                            // User has an existing resume
                            hasExistingResume = true;
                            currentResumeUrl = user.getResumeUrl();

                            // Extract file path from URL for deletion
                            if (currentResumeUrl.contains("/public/" + SUPABASE_STORAGE_BUCKET + "/")) {
                                currentResumePath = currentResumeUrl.split("/public/" + SUPABASE_STORAGE_BUCKET + "/")[1];
                            }

                            updateUIForExistingResume(user);
                        } else {
                            // No resume exists
                            hasExistingResume = false;
                            updateUIForNoResume();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UploadResume.this, "Failed to check resume status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIForExistingResume(User user) {
        // Hide upload container
        if (cvUploadContainer != null) {
            cvUploadContainer.setVisibility(View.GONE);
        }

        // Show resume details card
        View cvResumeDetails = findViewById(R.id.cvResumeDetails);
        if (cvResumeDetails != null) {
            cvResumeDetails.setVisibility(View.VISIBLE);
        }

        // Show resume details and action buttons
        if (tvFileName != null) tvFileName.setVisibility(View.VISIBLE);
        if (tvUploadDate != null) tvUploadDate.setVisibility(View.VISIBLE);
        if (btnViewResume != null) btnViewResume.setVisibility(View.VISIBLE);
        if (btnDeleteResume != null) btnDeleteResume.setVisibility(View.VISIBLE);
        if (btnUploadNew != null) btnUploadNew.setVisibility(View.VISIBLE);

        // Set file name and date
        if (tvFileName != null) tvFileName.setText("Resume.pdf");
        if (tvUploadDate != null) tvUploadDate.setText("Uploaded: Just now");

        // Update main text in upload container (if it exists)
        if (tvUploadFile != null) {
            tvUploadFile.setText("Resume Uploaded");
            tvUploadFile.setTextColor(getResources().getColor(R.color.success_color));
        }
    }

    private void updateUIForNoResume() {
        // Show upload container
        if (cvUploadContainer != null) {
            cvUploadContainer.setVisibility(View.VISIBLE);
        }

        // Hide resume details card
        View cvResumeDetails = findViewById(R.id.cvResumeDetails);
        if (cvResumeDetails != null) {
            cvResumeDetails.setVisibility(View.GONE);
        }

        // Hide resume details and action buttons
        if (tvFileName != null) tvFileName.setVisibility(View.GONE);
        if (tvUploadDate != null) tvUploadDate.setVisibility(View.GONE);
        if (btnViewResume != null) btnViewResume.setVisibility(View.GONE);
        if (btnDeleteResume != null) btnDeleteResume.setVisibility(View.GONE);
        if (btnUploadNew != null) btnUploadNew.setVisibility(View.GONE);

        // Reset main text in upload container
        if (tvUploadFile != null) {
            tvUploadFile.setText("Upload CV/Resume");
            tvUploadFile.setTextColor(getResources().getColor(R.color.text_color));
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select PDF Resume"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            resumeFileUri = data.getData();
            String fileName = getFileName(resumeFileUri);

            // Update UI to show selected file
            tvUploadFile.setText("Selected: " + fileName);
            Toast.makeText(this, "File selected: " + fileName, Toast.LENGTH_SHORT).show();

            // Upload the file to Supabase
            uploadResumeToSupabase();
        }
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
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "resume.pdf";
    }

    private void uploadResumeToSupabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resumeFileUri == null) {
            Toast.makeText(this, "Please select a PDF file first", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Uploading resume...");

        new Thread(() -> {
            try {
                String userId = currentUser.getUid();
                String fileName = "resume_" + userId + "_" + System.currentTimeMillis() + ".pdf";
                String filePath = "resumes/" + fileName;

                InputStream inputStream = getContentResolver().openInputStream(resumeFileUri);
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);
                inputStream.close();

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
                        currentResumePath = filePath; // Store for deletion
                        saveResumeUrlToDatabase(userId, resumeUrl);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(UploadResume.this,
                                "Upload failed: " + response.code() + " - " + response.message(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(UploadResume.this,
                            "Error uploading: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveResumeUrlToDatabase(String userId, String resumeUrl) {
        // Convert PDF to Base64 as backup
        convertPdfToBase64AndSave(userId, resumeUrl);
    }

    private void convertPdfToBase64AndSave(String userId, String resumeUrl) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(resumeFileUri);
            if (inputStream != null) {
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();

                String resumeBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);

                // Save both URL and Base64 to database
                saveResumeDataToDatabase(userId, resumeUrl, resumeBase64);
            }
        } catch (Exception e) {
            // If Base64 conversion fails, just save the URL
            saveResumeDataToDatabase(userId, resumeUrl, "");
        }
    }

    private void saveResumeDataToDatabase(String userId, String resumeUrl, String resumeBase64) {
        // Update user data with resume information
        mDatabase.child("users").child(userId).child("cvBase64").setValue(resumeBase64)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("users").child(userId).child("resumeUrl").setValue(resumeUrl)
                            .addOnSuccessListener(aVoid1 -> {
                                mDatabase.child("users").child(userId).child("hasResume").setValue(true)
                                        .addOnSuccessListener(aVoid2 -> {
                                            hideProgressDialog();
                                            Toast.makeText(UploadResume.this, "Resume uploaded successfully!", Toast.LENGTH_SHORT).show();

                                            // Update UI to show the uploaded resume
                                            checkExistingResume();
                                        })
                                        .addOnFailureListener(e -> {
                                            hideProgressDialog();
                                            Toast.makeText(UploadResume.this, "Failed to update resume status", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                hideProgressDialog();
                                Toast.makeText(UploadResume.this, "Failed to save resume URL", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(UploadResume.this, "Failed to save resume data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void viewResume() {
        if (currentResumeUrl.isEmpty()) {
            Toast.makeText(this, "No resume available to view", Toast.LENGTH_SHORT).show();
            return;
        }

        // For Supabase public URL, we can directly open it
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(currentResumeUrl), "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer app found. Please install a PDF reader.", Toast.LENGTH_SHORT).show();

            // Alternative: Open in browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentResumeUrl));
            startActivity(browserIntent);
        }
    }

    private void deleteResume() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Resume")
                .setMessage("Are you sure you want to delete your resume? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> confirmDeleteResume())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteResume() {
        showProgressDialog("Deleting resume...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Delete from Supabase Storage first
        deleteResumeFromSupabase(userId);
    }

    private void deleteResumeFromSupabase(String userId) {
        if (currentResumePath.isEmpty()) {
            // If we don't have the path, just delete from database
            deleteResumeFromDatabase(userId);
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_STORAGE_BUCKET + "/" + currentResumePath)
                        .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .header("apikey", SUPABASE_ANON_KEY)
                        .delete()
                        .build();

                Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    // Whether Supabase deletion succeeds or fails, delete from database
                    deleteResumeFromDatabase(userId);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e("UploadResume", "Error deleting from Supabase: " + e.getMessage());
                    // Continue with database deletion even if Supabase fails
                    deleteResumeFromDatabase(userId);
                });
            }
        }).start();
    }

    private void deleteResumeFromDatabase(String userId) {
        // Remove resume data from database
        mDatabase.child("users").child(userId).child("cvBase64").removeValue()
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("users").child(userId).child("resumeUrl").removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                mDatabase.child("users").child(userId).child("hasResume").setValue(false)
                                        .addOnSuccessListener(aVoid2 -> {
                                            hideProgressDialog();
                                            Toast.makeText(UploadResume.this, "Resume deleted successfully", Toast.LENGTH_SHORT).show();

                                            // Update UI
                                            hasExistingResume = false;
                                            currentResumeUrl = "";
                                            currentResumePath = "";
                                            updateUIForNoResume();
                                        })
                                        .addOnFailureListener(e -> {
                                            hideProgressDialog();
                                            Toast.makeText(UploadResume.this, "Failed to update resume status", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                hideProgressDialog();
                                Toast.makeText(UploadResume.this, "Failed to delete resume URL", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(UploadResume.this, "Failed to delete resume data", Toast.LENGTH_SHORT).show();
                });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}