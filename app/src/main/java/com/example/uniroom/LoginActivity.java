package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ApiResponse;
import com.example.uniroom.model.Models.LoginRequest;
import com.example.uniroom.network.Client;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput;
    private MaterialButton loginButton;
    private TextView registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        registerButton = findViewById(R.id.register);

        registerButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        loginButton.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (username.isEmpty()) {
            usernameInput.setError("Enter your username or email");
            usernameInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            passwordInput.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");

        Client.api().login(new LoginRequest(username, password))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Sign In");

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(LoginActivity.this,
                                    "Server error: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        ApiResponse result = response.body();

                        if (!result.success || result.user == null) {
                            Toast.makeText(LoginActivity.this,
                                    result.message == null ? "Login failed." : result.message,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        new Session(LoginActivity.this).save(result.user);

                        Intent intent;
                        if ("student".equalsIgnoreCase(result.user.role)) {
                            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                        } else if ("faculty".equalsIgnoreCase(result.user.role)) {
                            intent = new Intent(LoginActivity.this, FacultyDashboardActivity.class);
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "This account role is not supported by the mobile app.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        intent.putExtra("user_id", result.user.id);
                        intent.putExtra("first_name", result.user.first_name);
                        intent.putExtra("last_name", result.user.last_name);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Sign In");
                        Toast.makeText(LoginActivity.this,
                                "Connection failed. Check that Apache is running and your phone is on the same network.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
