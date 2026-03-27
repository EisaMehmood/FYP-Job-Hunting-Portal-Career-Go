package com.example.careergo.Utility;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OSNotificationOpenedResult;
import com.onesignal.OneSignal;

public class MyApplication extends Application {

    private static final String ONESIGNAL_APP_ID = "ec1a53d7-fd27-4dc2-981b-70f4ed031f32";
    private static final String TAG = "OneSignalDebug";

    // Firebase database reference
    private DatabaseReference mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        Log.d(TAG, "Initializing OneSignal with App ID: " + ONESIGNAL_APP_ID);

        // Enable verbose logging
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // Initialize OneSignal
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "job_notifications",
                    "Job Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        // Notification opened handler
        OneSignal.setNotificationOpenedHandler((OSNotificationOpenedResult result) -> {
            if (result.getNotification().getAdditionalData() != null) {
                String jobId = result.getNotification().getAdditionalData().optString("jobId", "");
                String type = result.getNotification().getAdditionalData().optString("type", "");
                Log.d(TAG, "Notification clicked - jobId: " + jobId + ", type: " + type);

                if ("new_job".equals(type) && !jobId.isEmpty()) {
                    Log.d(TAG, "Navigate to job details for jobId: " + jobId);

                    Intent intent = new Intent(getApplicationContext(), com.example.careergo.User.UserJobView.class);
                    intent.putExtra("jobId", jobId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    startActivity(intent);
                }
            }
        });

        OneSignal.addSubscriptionObserver(stateChanges -> {
            // Check if the user is now subscribed
            if (stateChanges.getTo().isSubscribed()) {
                String playerId = stateChanges.getTo().getUserId();
                Log.d(TAG, "Player ID available now: " + playerId);

                // Save it to Firebase if user is logged in
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    saveOneSignalIdToUserProfile(currentUserId, playerId);
                }
            }
        });


        // Prompt user for push notifications
        OneSignal.promptForPushNotifications();
    }

    // Save OneSignal Player ID to Firebase
    private void saveOneSignalIdToUserProfile(String userId, String playerId) {
        if (playerId != null && !playerId.isEmpty()) {
            mDatabase.child(userId).child("oneSignalId").setValue(playerId)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "OneSignal ID saved successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving OneSignal ID", e));
        } else {
            Log.e(TAG, "Player ID is null or empty. Cannot save to Firebase.");
        }
    }
}
