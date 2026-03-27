package com.example.careergo.Utility;

import android.os.StrictMode;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private static final String TAG = "EmailSender";

    public static void sendApprovalEmail(String userEmail, String userName) {
        // Allow network on main thread (for testing purposes)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final String fromEmail = "teamcareergo@gmail.com"; // replace with your Gmail
        final String password = "jiia nafv wzbs sjud"; // replace with your Gmail App Password

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
            message.setSubject("Account Approved!");
            message.setText("Hello " + userName + ",\n\nYour account has been approved by admin. You can now login.\n\nBest regards,\nCareerGo Team");

            Transport.send(message);

            Log.d(TAG, "Approval email sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
            Log.e(TAG, "Error sending email: " + e.getMessage());
        }
    }
}
