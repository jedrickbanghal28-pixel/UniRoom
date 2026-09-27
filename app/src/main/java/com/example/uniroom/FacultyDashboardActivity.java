package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView welcomeText;
    private Button scheduleButton;
    private Button roomButton;
    private Button notificationButton;
    private Button profileButton;
    private Button logoutButton;

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        welcomeText = findViewById(R.id.welcomeText);

        scheduleButton = findViewById(R.id.scheduleButton);
        roomButton = findViewById(R.id.roomButton);
        notificationButton = findViewById(R.id.notificationButton);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        userId = getIntent().getIntExtra("user_id", 0);

        String firstName = getIntent().getStringExtra("first_name");
        String lastName = getIntent().getStringExtra("last_name");

        if (firstName != null) {
            welcomeText.setText("Welcome, " + firstName + "!");
        }

        // My Schedule
        scheduleButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FacultyDashboardActivity.this,
                    ScheduleActivity.class
            );

            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        // Room Management / CRUD
        roomButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FacultyDashboardActivity.this,
                    RoomCrudActivity.class
            );

            intent.putExtra(
                    "user_id",
                    userId
            );

            startActivity(intent);
        });

        // Send Student Notification
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FacultyDashboardActivity.this,
                    FacultyNotificationActivity.class
            );

            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        // My Profile
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FacultyDashboardActivity.this,
                    ProfileActivity.class
            );

            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FacultyDashboardActivity.this,
                    MainActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }
}