package com.example.careergo.Auth;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.careergo.R;
import com.example.careergo.databinding.ActivityEmailBinding;

/**
 * An Activity that appears after a password reset email has been sent.
 * It prompts the user to check their email and provides a button to open their email app.
 */
public class EmailActivity extends AppCompatActivity {

    private ActivityEmailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize view binding
        binding = ActivityEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup click listeners
        binding.btnOpenEmail.setOnClickListener(v -> openEmailApp());

        binding.btnBackToLogin.setOnClickListener(v -> finish());

        // This TextView can be used to re-trigger the password reset flow if needed
        binding.tvEmailResend.setOnClickListener(v -> {
            Intent intent = new Intent(EmailActivity.this, ForgotPassActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Attempts to open the user's default email application.
     * Multiple approaches to ensure it works on different devices.
     */
    private void openEmailApp() {
        // Approach 1: Try to open Gmail directly (most common on Android)
        try {
            Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            if (gmailIntent != null) {
                startActivity(gmailIntent);
                return;
            }
        } catch (Exception e) {
        
        }

        // Approach 2: Try generic email intent
        Intent emailIntent = new Intent(Intent.ACTION_MAIN);
        emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);

        // This flag ensures it finds the activity
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Check if any email app can handle this intent
        PackageManager pm = getPackageManager();
        if (emailIntent.resolveActivity(pm) != null) {
            startActivity(emailIntent);
            return;
        }

        // Approach 3: Try to open mailto: protocol (opens email app with compose screen)
        try {
            Intent mailtoIntent = new Intent(Intent.ACTION_VIEW);
            mailtoIntent.setData(Uri.parse("mailto:"));
            if (mailtoIntent.resolveActivity(pm) != null) {
                startActivity(mailtoIntent);
                return;
            }
        } catch (Exception e) {
        
        }

        // Approach 4: Try to open default launcher with EMAIL category
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            startActivity(intent);
            return;
        } catch (Exception e) {
            // Continue to next approach
        }

        // Approach 5: Show dialog with options to install email apps
        showEmailAppOptions();
    }

    /**
     * Shows a dialog suggesting email apps to install if none is found
     */
    private void showEmailAppOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("No Email App Found")
                .setMessage("You don't have an email app installed. Would you like to install one from the Play Store?")
                .setPositiveButton("Install Gmail", (dialog, which) -> {
                    openPlayStoreForApp("com.google.android.gm");
                })
                .setNegativeButton("Install Outlook", (dialog, which) -> {
                    openPlayStoreForApp("com.microsoft.office.outlook");
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    /**
     * Opens Play Store to install the specified email app
     * @param packageName Package name of the email app
     */
    private void openPlayStoreForApp(String packageName) {
        try {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
            playStoreIntent.setData(Uri.parse("market://details?id=" + packageName));
            startActivity(playStoreIntent);
        } catch (android.content.ActivityNotFoundException e) {
            // If Play Store is not installed, open browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(browserIntent);
        }
    }

    /**
     * Alternative simpler method - this is often the most reliable
     */
    private void openEmailAppSimple() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);

        try {
            startActivity(intent);
        } catch (Exception e) {
            // If that fails, try a different approach
            try {
                Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
                if (gmailIntent != null) {
                    startActivity(gmailIntent);
                } else {
                    Toast.makeText(this, "No email app found. Please check your email manually.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception ex) {
                Toast.makeText(this, "No email app found. Please check your email manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Most reliable method that works on most devices
     */
    private void openEmailAppReliable() {
        // Create a chooser intent that shows all available email apps
        Intent emailIntent = new Intent(Intent.ACTION_MAIN);
        emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);

        // Create a chooser dialog
        Intent chooser = Intent.createChooser(emailIntent, "Open email app");

        try {
            startActivity(chooser);
        } catch (Exception e) {
            // Last resort: Try to open Gmail specifically
            try {
                Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
                if (gmailIntent != null) {
                    startActivity(gmailIntent);
                } else {
                    // If nothing works, show a helpful message
                    showManualEmailCheckMessage();
                }
            } catch (Exception ex) {
                showManualEmailCheckMessage();
            }
        }
    }

    private void showManualEmailCheckMessage() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Check Your Email")
                .setMessage("Please check your email app manually for the password reset link. Look for an email from your app's authentication system.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clean up binding to prevent memory leaks
    }
}