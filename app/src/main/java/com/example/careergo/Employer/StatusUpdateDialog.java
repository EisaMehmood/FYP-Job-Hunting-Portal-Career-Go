package com.example.careergo.Employer;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.careergo.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class StatusUpdateDialog extends Dialog {

    private Context context;
    private String applicationId;
    private String currentStatus;
    private DatabaseReference mDatabase;
    private OnStatusUpdateListener listener;

    private RadioGroup radioGroupStatus;
    private RadioButton rbPending, rbShortlisted, rbApproved, rbRejected;
    private Button btnUpdate, btnCancel;

    public interface OnStatusUpdateListener {
        void onStatusUpdated(String newStatus);
    }

    public StatusUpdateDialog(Context context, String applicationId, String currentStatus, OnStatusUpdateListener listener) {
        super(context);
        this.context = context;
        this.applicationId = applicationId;
        this.currentStatus = currentStatus;
        this.listener = listener;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_update_dialog);

        initializeViews();
        setupCurrentStatus();
        setupClickListeners();
    }

    private void initializeViews() {
        radioGroupStatus = findViewById(R.id.radioGroupStatus);
        rbPending = findViewById(R.id.rbPending);
        rbShortlisted = findViewById(R.id.rbShortlisted);
        rbApproved = findViewById(R.id.rbApproved);
        rbRejected = findViewById(R.id.rbRejected);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupCurrentStatus() {
        if (currentStatus != null) {
            switch (currentStatus.toLowerCase()) {
                case "pending":
                    rbPending.setChecked(true);
                    break;
                case "shortlisted":
                    rbShortlisted.setChecked(true);
                    break;
                case "approved":
                    rbApproved.setChecked(true);
                    break;
                case "rejected":
                    rbRejected.setChecked(true);
                    break;
                default:
                    rbPending.setChecked(true);
            }
        }
    }

    private void setupClickListeners() {
        btnUpdate.setOnClickListener(v -> updateStatus());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void updateStatus() {
        int selectedId = radioGroupStatus.getCheckedRadioButtonId();
        String newStatus;

        if (selectedId == R.id.rbPending) {
            newStatus = "pending";
        } else if (selectedId == R.id.rbShortlisted) {
            newStatus = "shortlisted";
        } else if (selectedId == R.id.rbApproved) {
            newStatus = "approved";
        } else if (selectedId == R.id.rbRejected) {
            newStatus = "rejected";
        } else {
            newStatus = "pending";
        }

        // Update status in Firebase
        mDatabase.child("applications").child(applicationId).child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();

                    // Send notification to student
                    sendStatusNotification(applicationId, newStatus);

                    if (listener != null) {
                        listener.onStatusUpdated(newStatus);
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendStatusNotification(String applicationId, String newStatus) {
        // Get application details first to get studentId
        mDatabase.child("applications").child(applicationId).addListenerForSingleValueEvent(
                new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String studentId = dataSnapshot.child("studentId").getValue(String.class);
                            String jobId = dataSnapshot.child("jobId").getValue(String.class);

                            if (studentId != null) {
                                String notificationId = mDatabase.child("notifications").push().getKey();
                                if (notificationId != null) {
                                    Map<String, Object> notification = new HashMap<>();
                                    notification.put("studentId", studentId);
                                    notification.put("applicationId", applicationId);
                                    notification.put("jobId", jobId);
                                    notification.put("message", "Your application status has been updated to: " + newStatus);
                                    notification.put("timestamp", System.currentTimeMillis());
                                    notification.put("read", false);
                                    notification.put("type", "status_update");

                                    mDatabase.child("notifications").child(notificationId).setValue(notification);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        // Silent fail for notification
                    }
                }
        );
    }
}