package com.example.uniroom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FacultyNotificationActivity extends AppCompatActivity {

    private Spinner sectionSpinner;
    private EditText roomFromInput;
    private EditText roomToInput;
    private EditText messageInput;
    private Button sendButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_notification);

        sectionSpinner = findViewById(R.id.sectionSpinner);
        roomFromInput = findViewById(R.id.roomFromInput);
        roomToInput = findViewById(R.id.roomToInput);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);

        // Section choices
        String[] sections = {
                "Select Section",
                "BS-IT A1",
                "BS-IT A2",
                "BS-MedTech A1",
                "BS-MedTech A2"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        sections
                );

        sectionSpinner.setAdapter(adapter);

        // Example message automatically generated
        sendButton.setOnClickListener(v -> {

            String section = sectionSpinner.getSelectedItem().toString();
            String roomFrom = roomFromInput.getText().toString().trim();
            String roomTo = roomToInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();

            if (section.equals("Select Section")) {
                Toast.makeText(
                        this,
                        "Please select a section.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (roomFrom.isEmpty()) {
                roomFromInput.setError("Enter current room");
                roomFromInput.requestFocus();
                return;
            }

            if (roomTo.isEmpty()) {
                roomToInput.setError("Enter new room");
                roomToInput.requestFocus();
                return;
            }

            if (message.isEmpty()) {
                messageInput.setError("Enter notification message");
                messageInput.requestFocus();
                return;
            }

            Toast.makeText(
                    this,
                    "Notification sent to " + section,
                    Toast.LENGTH_LONG
            ).show();

            roomFromInput.setText("");
            roomToInput.setText("");
            messageInput.setText("");
        });

        backButton.setOnClickListener(v -> finish());
    }
}