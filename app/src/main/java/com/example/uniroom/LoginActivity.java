package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ApiResponse;
import com.example.uniroom.model.Models.ChangePasswordRequest;
import com.example.uniroom.model.Models.LoginRequest;
import com.example.uniroom.model.Models.User;
import com.example.uniroom.network.Client;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * Students and faculty log in with the account the admin created on the
 * UNIROOM admin website (BSIT or BSMEDTECH). They can type their email or
 * their username.
 *
 * If the admin chose "temporary password", the API answers with
 * must_change_password = true and the user must choose a new password
 * before the dashboard opens.
 */
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

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Signing in..." : "Sign In");
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

        setBusy(true);

        Client.api().login(new LoginRequest(username, password))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        setBusy(false);

                        ApiResponse result = readBody(response);

                        if (result == null) {
                            toast("Server error (" + response.code() + "). Please try again.");
                            return;
                        }

                        if (!result.success || result.user == null) {
                            toast(result.message == null ? "Login failed." : result.message);
                            return;
                        }

                        User user = result.user;

                        if (!"student".equalsIgnoreCase(user.role)
                                && !"faculty".equalsIgnoreCase(user.role)) {
                            toast("Admin accounts use the UNIROOM admin website, not the mobile app.");
                            return;
                        }

                        if (user.must_change_password) {
                            askForNewPassword(username, password, user);
                        } else {
                            openDashboard(user);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        setBusy(false);
                        toast("Unable to connect to the server. Check your internet connection and try again.");
                    }
                });
    }

    /*
     * First login with a temporary password: the user chooses a new one.
     */
    private void askForNewPassword(String login, String temporaryPassword, User user) {
        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        TextInputLayout newLayout = passwordField("New password *");
        TextInputLayout confirmLayout = passwordField("Confirm new password *");
        TextView rules = new TextView(this);
        rules.setText("At least 8 characters, with letters and numbers.");
        rules.setPadding(0, pad / 4, 0, 0);

        box.addView(newLayout);
        box.addView(confirmLayout);
        box.addView(rules);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set a new password")
                .setMessage("Hi " + user.first_name + "! You are using a temporary password. "
                        + "Please choose your own password to continue.")
                .setView(box)
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String next = text(newLayout);
                String confirm = text(confirmLayout);
                newLayout.setError(null);
                confirmLayout.setError(null);

                if (next.isEmpty()) { newLayout.setError("New password is required."); return; }
                if (next.length() < 8 || !next.matches(".*[A-Za-z].*") || !next.matches(".*[0-9].*")) {
                    newLayout.setError("Use at least 8 characters with letters and numbers.");
                    return;
                }
                if (next.equals(temporaryPassword)) {
                    newLayout.setError("Use a different password from the temporary one.");
                    return;
                }
                if (!next.equals(confirm)) { confirmLayout.setError("Passwords do not match."); return; }

                save.setEnabled(false);
                Client.api().changePassword(new ChangePasswordRequest(login, temporaryPassword, next))
                        .enqueue(new Callback<ApiResponse>() {
                            @Override
                            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                                save.setEnabled(true);
                                ApiResponse result = readBody(response);
                                if (result != null && result.success) {
                                    dialog.dismiss();
                                    passwordInput.setText("");
                                    toast("Password changed. Welcome to UNIROOM!");
                                    openDashboard(user);
                                } else {
                                    newLayout.setError(result == null || result.message == null
                                            ? "Unable to change the password." : result.message);
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse> call, Throwable t) {
                                save.setEnabled(true);
                                toast("Unable to connect to the server.");
                            }
                        });
            });
        });

        dialog.show();
    }

    private void openDashboard(User user) {
        new Session(this).save(user);

        Intent intent = "faculty".equalsIgnoreCase(user.role)
                ? new Intent(this, FacultyDashboardActivity.class)
                : new Intent(this, StudentDashboardActivity.class);

        intent.putExtra("user_id", user.id);
        intent.putExtra("first_name", user.first_name);
        intent.putExtra("last_name", user.last_name);
        startActivity(intent);
        finish();
    }

    /*
     * The API sends a JSON message even for errors (401 wrong password,
     * 422 invalid password, ...). Retrofit puts those in errorBody().
     */
    private ApiResponse readBody(Response<ApiResponse> response) {
        if (response.body() != null) return response.body();
        try {
            if (response.errorBody() != null) {
                return new Gson().fromJson(response.errorBody().charStream(), ApiResponse.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private TextInputLayout passwordField(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(input);
        return layout;
    }

    private String text(TextInputLayout layout) {
        return layout.getEditText() == null ? "" : layout.getEditText().getText().toString();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
