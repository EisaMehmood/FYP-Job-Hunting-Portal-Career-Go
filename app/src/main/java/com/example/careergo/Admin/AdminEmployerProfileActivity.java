package com.example.careergo.Admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.careergo.Model.Employer;
import com.example.careergo.R;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminEmployerProfileActivity extends AppCompatActivity {

    private TextView tvCompanyName, tvContactPerson, tvIndustry, tvEmail, tvPhone, tvAddress;
    private TextView tvActiveJobs, tvTotalJobs, tvMemberSince;
    private Chip chipStatus;
    private ProgressBar progressBar;
    private Button btnViewJobs, btnContact;
    private ImageButton ibBack;

    private DatabaseReference mDatabase;
    private String employerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_employer_profile);

        initializeViews();
        setupClickListeners();

        // Get employer ID from intent
        employerId = getIntent().getStringExtra("employerId");
        if (employerId == null) {
            Toast.makeText(this, "Employer not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        loadEmployerData();
    }

    private void initializeViews() {
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvContactPerson = findViewById(R.id.tvContactPerson);
        tvIndustry = findViewById(R.id.tvIndustry);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvActiveJobs = findViewById(R.id.tvActiveJobs);
        tvTotalJobs = findViewById(R.id.tvTotalJobs);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        chipStatus = findViewById(R.id.chipStatus);
        progressBar = findViewById(R.id.progressBar);
        btnViewJobs = findViewById(R.id.btnViewJobs);
        btnContact = findViewById(R.id.btnContact);
        ibBack = findViewById(R.id.ibBack);
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());

        btnViewJobs.setOnClickListener(v -> {
            Intent intent = new Intent(AdminEmployerProfileActivity.this, AdminEmployerJobsActivity.class);
            intent.putExtra("employerId", employerId);
            startActivity(intent);
        });

        btnContact.setOnClickListener(v -> showContactOptions());
    }

    private void loadEmployerData() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        mDatabase.child("users").child(employerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(android.view.View.GONE);

                if (snapshot.exists()) {
                    Employer employer = snapshot.getValue(Employer.class);
                    if (employer != null) {
                        displayEmployerData(employer);
                        loadJobStats();
                        loadCompanyProfileInfo(); // Load company profile info separately
                    }
                } else {
                    Toast.makeText(AdminEmployerProfileActivity.this, "Employer data not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(AdminEmployerProfileActivity.this, "Failed to load employer data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayEmployerData(Employer employer) {
        // Set company name or individual name
        if (employer.getCompanyName() != null && !employer.getCompanyName().isEmpty()) {
            tvCompanyName.setText(employer.getCompanyName());
        } else {
            tvCompanyName.setText(employer.getFirstName() + " " + employer.getLastName());
        }

        // Set contact person
        tvContactPerson.setText(employer.getFirstName() + " " + employer.getLastName());

        // Set other fields with null checks
        tvEmail.setText(employer.getEmail() != null ? employer.getEmail() : "Not provided");
        tvPhone.setText(employer.getMobileNo() != null ? employer.getMobileNo() : "Not provided");

        // Set status chip
        if (employer.isApproved()) {
            chipStatus.setText("Verified");
            chipStatus.setChipBackgroundColorResource(R.color.light_green);
        } else {
            chipStatus.setText("Pending Verification");
            chipStatus.setChipBackgroundColorResource(R.color.light_orange);
        }

        // Set member since from user's createdAt timestamp
        setMemberSince(employer.getCreatedAt());
    }

    private void setMemberSince(String createdAt) {
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                // Try to parse as timestamp (long)
                long timestamp = Long.parseLong(createdAt);
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
                String year = sdf.format(date);
                tvMemberSince.setText("Member since " + year);
            } catch (NumberFormatException e) {
                // If it's not a timestamp, try to extract year from string
                try {
                    // Try to parse as date string
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = sdf.parse(createdAt);
                    if (date != null) {
                        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                        String year = yearFormat.format(date);
                        tvMemberSince.setText("Member since " + year);
                    } else {
                        // If parsing fails, just use the first 4 characters as year
                        if (createdAt.length() >= 4) {
                            String year = createdAt.substring(0, 4);
                            tvMemberSince.setText("Member since " + year);
                        } else {
                            tvMemberSince.setText("Member since N/A");
                        }
                    }
                } catch (Exception ex) {
                    // If all parsing fails, show default
                    tvMemberSince.setText("Member since N/A");
                }
            }
        } else {
            tvMemberSince.setText("Member since N/A");
        }
    }

    private void loadJobStats() {
        mDatabase.child("jobs").orderByChild("employerId").equalTo(employerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalJobs = snapshot.getChildrenCount();
                        long activeJobs = 0;

                        for (DataSnapshot jobSnapshot : snapshot.getChildren()) {
                            Boolean isActive = jobSnapshot.child("active").getValue(Boolean.class);
                            if (isActive != null && isActive) {
                                activeJobs++;
                            }
                        }

                        tvTotalJobs.setText(String.valueOf(totalJobs));
                        tvActiveJobs.setText(String.valueOf(activeJobs));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvTotalJobs.setText("0");
                        tvActiveJobs.setText("0");
                    }
                });
    }

    private void loadCompanyProfileInfo() {
        mDatabase.child("companyProfiles").child(employerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // -------- INDUSTRY --------
                            String industry = snapshot.child("industry").getValue(String.class);
                            tvIndustry.setText(industry != null ? industry : "Not specified");

                            // -------- ADDRESS --------
                            String address = snapshot.child("address").getValue(String.class);
                            if (address != null && !address.isEmpty()) {
                                tvAddress.setText(address);
                            } else {
                                // Try to build address from individual components
                                String street = snapshot.child("street").getValue(String.class);
                                String city = snapshot.child("city").getValue(String.class);
                                String state = snapshot.child("state").getValue(String.class);
                                String pincode = snapshot.child("pincode").getValue(String.class);

                                StringBuilder fullAddress = new StringBuilder();
                                if (street != null && !street.isEmpty()) {
                                    fullAddress.append(street);
                                }
                                if (city != null && !city.isEmpty()) {
                                    if (fullAddress.length() > 0) fullAddress.append(", ");
                                    fullAddress.append(city);
                                }
                                if (state != null && !state.isEmpty()) {
                                    if (fullAddress.length() > 0) fullAddress.append(", ");
                                    fullAddress.append(state);
                                }
                                if (pincode != null && !pincode.isEmpty()) {
                                    if (fullAddress.length() > 0) fullAddress.append(" - ");
                                    fullAddress.append(pincode);
                                }

                                if (fullAddress.length() > 0) {
                                    tvAddress.setText(fullAddress.toString());
                                } else {
                                    tvAddress.setText("Not provided");
                                }
                            }

                            // -------- Check if company profile has createdAt and use it if user doesn't --------
                            Long companyCreatedAt = snapshot.child("createdAt").getValue(Long.class);
                            if (companyCreatedAt != null) {
                                Date date = new Date(companyCreatedAt);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
                                String year = sdf.format(date);
                                tvMemberSince.setText("Member since " + year);
                            }
                        } else {
                            // No company profile found
                            tvIndustry.setText("Not specified");
                            tvAddress.setText("Not provided");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvIndustry.setText("Not specified");
                        tvAddress.setText("Not provided");
                    }
                });
    }

    private void showContactOptions() {
        String[] options = {"Send Email", "Make Phone Call", "View Address"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Contact Employer");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    sendEmail();
                    break;
                case 1:
                    makePhoneCall();
                    break;
                case 2:
                    viewAddressOnMap();
                    break;
            }
        });
        builder.show();
    }

    private void sendEmail() {
        String email = tvEmail.getText().toString();
        if (email.equals("Not provided")) {
            Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Job Opportunity");
        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    private void makePhoneCall() {
        String phone = tvPhone.getText().toString();
        if (phone.equals("Not provided")) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void viewAddressOnMap() {
        String address = tvAddress.getText().toString();
        if (address.equals("Not provided")) {
            Toast.makeText(this, "Address not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
}