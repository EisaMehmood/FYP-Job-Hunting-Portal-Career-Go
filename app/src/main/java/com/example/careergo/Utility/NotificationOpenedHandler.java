package com.example.careergo.Utility;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.careergo.User.UserJobView;
import com.onesignal.OSNotificationOpenedResult;
import com.onesignal.OneSignal;

public class NotificationOpenedHandler implements OneSignal.OSNotificationOpenedHandler {

    private final Context context;

    public NotificationOpenedHandler(Context context) {
        this.context = context;
    }

    @Override
    public void notificationOpened(OSNotificationOpenedResult result) {
        try {
            String jobId = result.getNotification().getAdditionalData().optString("jobId", null);

            Log.d("OneSignal", "Notification clicked, jobId = " + jobId);

            if (jobId != null) {
                Intent intent = new Intent(context, UserJobView.class);
                intent.putExtra("jobId", jobId);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            }

        } catch (Exception e) {
            Log.e("OneSignal", "Error on notification click", e);
        }
    }
}
