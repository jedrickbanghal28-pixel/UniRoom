package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models;
import com.example.uniroom.model.Models.ApiResponse;
import com.example.uniroom.network.Client;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView info, nameText, roleText;
    private MaterialButton logout;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        info = findViewById(R.id.info);
        nameText = findViewById(R.id.profileName);
        roleText = findViewById(R.id.profileRole);
        logout = findViewById(R.id.logout);

        Session session = new Session(this);
        userId = session.id();
        if (userId <= 0) userId = getIntent().getIntExtra("user_id", 0);

        if (userId <= 0) {
            Toast.makeText(this, "User session not found.", Toast.LENGTH_SHORT).show();
            session.logout(this);
            return;
        }

        logout.setOnClickListener(v -> session.logout(this));
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        loadProfile();
    }

    private void loadProfile() {
        Client.api().profile(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().user != null) {
                    Models.User user = response.body().user;
                    nameText.setText(user.first_name + " " + user.last_name);
                    roleText.setText("Student • " + cleanProgram(user.program));
                    info.setText(
                            "School ID\n" + value(user.student_id) +
                            "\n\nUsername\n" + value(user.username) +
                            "\n\nEmail\n" + value(user.email) +
                            "\n\nContact Number\n" + value(user.contact_number) +
                            "\n\nProgram\n" + cleanProgram(user.program) +
                            "\n\nSection\n" + value(user.section_name)
                    );
                } else {
                    info.setText("Unable to load profile information.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                info.setText("Could not connect to the server.\n\nPlease try again.");
            }
        });
    }

    private String value(String text) {
        return text == null || text.isEmpty() ? "Not provided" : text;
    }

    private String cleanProgram(String program) {
        if (program == null) return "Program";
        if (program.equalsIgnoreCase("BSMEDTECH")) return "BS-MedTech";
        if (program.equalsIgnoreCase("BSIT")) return "BS-IT";
        return program;
    }
}
