package com.example.careergo.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.careergo.R;

public class SelectRoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        // Setup click listeners for roles
        setupRoleClick(R.id.ivRoleAdmin, LoginActivity.class, "admin");
        setupRoleClick(R.id.ivRoleTpo, LoginActivity.class, "employer");
        setupRoleClick(R.id.ivRoleStudent, LoginActivity.class, "student");
    }

    /**
     * Helper method to setup click listener for a role
     *
     * @param viewId         The view id to attach click listener
     * @param targetActivity The activity to start
     * @param role           Role string to pass in intent
     */
    private void setupRoleClick(int viewId, Class<?> targetActivity, String role) {
        findViewById(viewId).setOnClickListener(v -> {
            // Trigger haptic feedback
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

            // Start the target activity with role info
            Intent intent = new Intent(SelectRoleActivity.this, targetActivity);
            intent.putExtra("role", role);
            startActivity(intent);

            // Optional: smooth transition
            // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }
}
