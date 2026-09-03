package com.example.uniroom;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ApiResponse;
import com.example.uniroom.model.Models.RegisterRequest;
import com.example.uniroom.network.Client;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText[] e = new EditText[8];
    private Spinner program;
    private Spinner section;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.activity_register);

        int[] ids = {
                R.id.fn,
                R.id.ln,
                R.id.sid,
                R.id.user,
                R.id.email,
                R.id.phone,
                R.id.pass,
                R.id.confirm
        };

        for (int i = 0; i < 8; i++) {
            e[i] = findViewById(ids[i]);
        }

        program = findViewById(R.id.program);
        section = findViewById(R.id.section);

        program.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[]{
                                "BS-IT",
                                "BS-MedTech"
                        }
                )
        );

        section.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[]{
                                "BS-IT 1A",
                                "BS-MedTech 1A"
                        }
                )
        );

        findViewById(R.id.btn).setOnClickListener(v -> go());

        findViewById(R.id.back).setOnClickListener(v -> finish());
    }

    private boolean empty(EditText x, String message) {

        if (x.getText().toString().trim().isEmpty()) {
            x.setError(message);
            x.requestFocus();
            return true;
        }

        return false;
    }

    private void go() {

        for (int i = 0; i < 8; i++) {
            if (empty(e[i], "Required")) {
                return;
            }
        }

        String email = e[4].getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            e[4].setError("Invalid email");
            e[4].requestFocus();
            return;
        }

        String password = e[6].getText().toString();

        if (password.length() < 6) {
            e[6].setError("Minimum 6 characters");
            e[6].requestFocus();
            return;
        }

        String confirmPassword = e[7].getText().toString();

        if (!password.equals(confirmPassword)) {
            e[7].setError("Passwords do not match");
            e[7].requestFocus();
            return;
        }

        int sec = section.getSelectedItemPosition() + 1;

        RegisterRequest request = new RegisterRequest(
                e[0].getText().toString().trim(),
                e[1].getText().toString().trim(),
                e[2].getText().toString().trim(),
                e[3].getText().toString().trim(),
                email,
                e[5].getText().toString().trim(),
                password,
                sec
        );

        Client.api().register(request).enqueue(
                new Callback<ApiResponse>() {

                    @Override
                    public void onResponse(
                            Call<ApiResponse> call,
                            Response<ApiResponse> response
                    ) {

                        if (response.isSuccessful()) {

                            ApiResponse body = response.body();

                            if (body != null) {

                                Toast.makeText(
                                        RegisterActivity.this,
                                        body.message != null
                                                ? body.message
                                                : "Registration successful.",
                                        Toast.LENGTH_LONG
                                ).show();

                                if (body.success) {
                                    finish();
                                }

                            } else {

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Empty server response.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                        } else {

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Registration failed. Server code: "
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

                        Toast.makeText(
                                RegisterActivity.this,
                                "API connection failed: "
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}