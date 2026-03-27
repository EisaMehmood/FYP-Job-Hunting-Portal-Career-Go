package com.example.careergo.Admin;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Adapters.PendingApprovalAdapter;
import com.example.careergo.Model.UserModel;
import com.example.careergo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class PendingApproval extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PendingApprovalAdapter adapter;
    private List<UserModel> pendingUsers;
    private DatabaseReference mDatabase;
    private ImageButton btnBack;

    // Your admin email credentials (use a separate email for production)
    private final String ADMIN_EMAIL = "teamcareergo@gmail.com";
    private final String ADMIN_PASSWORD =  "jiia nafv wzbs sjud";;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        // Initialize back button
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerViewPending);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set back button click listener
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // Or use finish() to simply close the activity
        });

        pendingUsers = new ArrayList<>();
        adapter = new PendingApprovalAdapter(pendingUsers, this, this::approveUser);
        recyclerView.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        loadPendingUsers();
    }

    private void loadPendingUsers() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingUsers.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    Boolean approved = userSnap.child("approved").getValue(Boolean.class);
                    // Treat null as pending
                    if (approved == null || !approved) {
                        String name = userSnap.child("firstName").getValue(String.class);
                        String email = userSnap.child("email").getValue(String.class);
                        String role = userSnap.child("role").getValue(String.class);
                        String uid = userSnap.getKey();
                        pendingUsers.add(new UserModel(uid, name, email, role));
                        Log.d("PendingApproval", "User found: " + name + ", approved: " + approved);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PendingApproval.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveUser(UserModel user) {
        if (user.uid == null) return;

        mDatabase.child(user.uid).child("approved").setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(PendingApproval.this, user.fname + " approved", Toast.LENGTH_SHORT).show();
                        pendingUsers.remove(user);
                        adapter.notifyDataSetChanged();
                        sendApprovalEmail(user.email, user.fname);
                    } else {
                        Toast.makeText(PendingApproval.this, "Failed to approve", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendApprovalEmail(String userEmail, String userName) {
        AsyncTask.execute(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(ADMIN_EMAIL, ADMIN_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(ADMIN_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
                message.setSubject("Account Approved");
                message.setText("Hello " + userName + ",\n\nYour account has been approved by the admin. You can now login to the app.\n\nBest regards,\nCareerGo Team");

                Transport.send(message);
                Log.d("PendingApproval", "Email sent to: " + userEmail);
            } catch (MessagingException e) {
                e.printStackTrace();
                Log.e("PendingApproval", "Email failed: " + e.getMessage());
            }
        });
    }
}