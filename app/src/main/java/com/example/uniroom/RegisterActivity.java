package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ApiResponse;
import com.example.uniroom.model.Models.RegisterRequest;
import com.example.uniroom.network.Client;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText firstName;
    private TextInputEditText lastName;
    private TextInputEditText studentId;
    private TextInputEditText username;
    private TextInputEditText email;
    private TextInputEditText phone;
    private TextInputEditText password;
    private TextInputEditText confirmPassword;

    private Spinner programSpinner;
    private Spinner sectionSpinner;

    private MaterialButton registerButton;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // Connect Java to XML
        firstName = findViewById(R.id.fn);
        lastName = findViewById(R.id.ln);
        studentId = findViewById(R.id.sid);
        username = findViewById(R.id.user);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        password = findViewById(R.id.pass);
        confirmPassword = findViewById(R.id.confirm);

        programSpinner = findViewById(R.id.program);
        sectionSpinner = findViewById(R.id.section);

        registerButton = findViewById(R.id.btn);
        backButton = findViewById(R.id.back);

        setupProgramSpinner();

        // Register
        registerButton.setOnClickListener(v -> registerUser());

        // Back to Login
        backButton.setOnClickListener(v -> finish());
    }

    private void setupProgramSpinner() {

        String[] programs = {
                "Select Program",
                "BS-IT",
                "BS-MedTech"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        programs
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        programSpinner.setAdapter(adapter);

        programSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id
                    ) {

                        if (position == 1) {

                            setupITSections();

                        } else if (position == 2) {

                            setupMedTechSections();

                        } else {

                            clearSections();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    private void setupITSections() {

        String[] sections = {
                "Select Section",
                "BS-IT A1"
        };

        // Replace these with the actual IDs from your sections table
        int[] sectionIds = {
                0,
                1
        };

        setupSectionAdapter(sections, sectionIds);
    }

    private void setupMedTechSections() {

        String[] sections = {
                "Select Section",
                "BS-MedTech A1"
        };

        // Replace these with the actual IDs from your sections table
        int[] sectionIds = {
                0,
                2
        };

        setupSectionAdapter(sections, sectionIds);
    }

    private void setupSectionAdapter(
            String[] sections,
            int[] sectionIds
    ) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sections
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        sectionSpinner.setAdapter(adapter);

        sectionSpinner.setTag(sectionIds);
    }

    private void clearSections() {

        String[] sections = {
                "Select Section"
        };

        int[] sectionIds = {
                0
        };

        setupSectionAdapter(sections, sectionIds);
    }

    private void registerUser() {

        String first =
                firstName.getText().toString().trim();

        String last =
                lastName.getText().toString().trim();

        String student =
                studentId.getText().toString().trim();

        String user =
                username.getText().toString().trim();

        String mail =
                email.getText().toString().trim();

        String contact =
                phone.getText().toString().trim();

        String pass =
                password.getText().toString();

        String confirm =
                confirmPassword.getText().toString();

        // Check text fields
        if (first.isEmpty()) {
            firstName.setError("Enter first name");
            firstName.requestFocus();
            return;
        }

        if (last.isEmpty()) {
            lastName.setError("Enter last name");
            lastName.requestFocus();
            return;
        }

        if (student.isEmpty()) {
            studentId.setError("Enter student ID");
            studentId.requestFocus();
            return;
        }

        if (user.isEmpty()) {
            username.setError("Enter username");
            username.requestFocus();
            return;
        }

        if (mail.isEmpty()) {
            email.setError("Enter school email");
            email.requestFocus();
            return;
        }

        if (contact.isEmpty()) {
            phone.setError("Enter contact number");
            phone.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            password.setError("Enter password");
            password.requestFocus();
            return;
        }

        if (confirm.isEmpty()) {
            confirmPassword.setError("Confirm your password");
            confirmPassword.requestFocus();
            return;
        }

        // Check password
        if (!pass.equals(confirm)) {

            confirmPassword.setError(
                    "Passwords do not match"
            );

            confirmPassword.requestFocus();

            return;
        }

        // Check program
        if (programSpinner.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select a program.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Check section
        if (sectionSpinner.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select a section.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Get section ID
        int[] sectionIds =
                (int[]) sectionSpinner.getTag();

        int selectedPosition =
                sectionSpinner.getSelectedItemPosition();

        int sectionId =
                sectionIds[selectedPosition];

        // Create request
        RegisterRequest request =
                new RegisterRequest(
                        first,
                        last,
                        student,
                        user,
                        mail,
                        contact,
                        pass,
                        sectionId
                );

        registerButton.setEnabled(false);

        Client.api()
                .register(request)
                .enqueue(
                        new Callback<ApiResponse>() {

                            @Override
                            public void onResponse(
                                    Call<ApiResponse> call,
                                    Response<ApiResponse> response
                            ) {

                                registerButton.setEnabled(true);

                                if (
                                        response.isSuccessful()
                                                && response.body() != null
                                ) {

                                    ApiResponse result =
                                            response.body();

                                    Toast.makeText(
                                            RegisterActivity.this,
                                            result.message,
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    if (result.success) {

                                        // Return to Login
                                        Intent intent =
                                                new Intent(
                                                        RegisterActivity.this,
                                                        LoginActivity.class
                                                );

                                        intent.setFlags(
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        );

                                        startActivity(intent);

                                        finish();
                                    }

                                } else {

                                    Toast.makeText(
                                            RegisterActivity.this,
                                            "Registration failed. Server error: "
                                                    + response.code(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<ApiResponse> call,
                                    Throwable t
                            ) {

                                registerButton.setEnabled(true);

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Connection failed: "
                                                + t.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }
}