package com.example.careergo.Utility;


import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class OneSignalNotificationService {

    private static final String ONESIGNAL_APP_ID = "ec1a53d7-fd27-4dc2-981b-70f4ed031f32";
    private static final String ONESIGNAL_REST_API_KEY = "os_v2_app_5qnfhv75e5g4fga3od2o2ay7gkscvycqgzke6c4disgdzg2wyifqqyws3n2xxjjs2dvgfwplz6d7xybfrtu6ncap3nwrjdpdljlde5q";
    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";

    public boolean sendNotification(List<String> playerIds, String heading, String content, String jobId) {
        try {
            URL url = new URL(ONESIGNAL_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Basic " + ONESIGNAL_REST_API_KEY);
            conn.setDoOutput(true);

            // Create JSON payload
            JSONObject payload = new JSONObject();
            payload.put("app_id", ONESIGNAL_APP_ID);

            // Add player IDs
            JSONArray includePlayerIds = new JSONArray();
            for (String playerId : playerIds) {
                includePlayerIds.put(playerId);
            }
            payload.put("include_player_ids", includePlayerIds);

            // Notification content
            JSONObject headings = new JSONObject();
            headings.put("en", heading);
            payload.put("headings", headings);

            JSONObject contents = new JSONObject();
            contents.put("en", content);
            payload.put("contents", contents);

            // Additional data
            JSONObject data = new JSONObject();
            data.put("jobId", jobId);
            data.put("type", "new_job");
            payload.put("data", data);


            // Send request
            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.close();

            // Get response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                Log.d("OneSignal", "Notification sent: " + response);
                return true;
            } else {
                Log.e("OneSignal", "Failed to send notification. Response code: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            Log.e("OneSignal", "Error sending notification", e);
            return false;
        }
    }
}