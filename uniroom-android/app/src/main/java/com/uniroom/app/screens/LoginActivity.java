package com.uniroom.app.screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.uniroom.app.R;
import com.uniroom.app.data.ApiClient;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.SavedSession;
import com.uniroom.app.data.DataSync;
import com.uniroom.app.notify.NotificationCheckJob;
import com.uniroom.app.notify.Notifier;
import com.uniroom.app.ui.Buttons;
import com.uniroom.app.ui.FormField;
import com.uniroom.app.ui.Ui;

import org.json.JSONObject;

/**
 * Sign-in screen for students and faculty.
 *
 * Accounts are created only by the admin on the UNIROOM website. The user
 * receives an email with their username and a temporary password.
 *
 *  1. They sign in with the username (or email) and temporary password.
 *  2. The app asks them to choose their own password (required).
 *  3. They are signed in again with the new password and the dashboard opens
 *     with their name, section (or subjects), schedule and notifications,
 *     all loaded from the database.
 *
 * The sign-in is remembered on the phone until Log out.
 */
public class LoginActivity extends Activity {

    /** Set when the screen is opened from a phone notification. */
    public static final String EXTRA_OPEN_ALERTS = "open_alerts";

    private FormField username, password;
    private View errorBox;
    private TextView errorText;
    private LinearLayout signInButton;
    private TextView signInLabel;
    private ProgressBar spinner;
    private boolean loading = false;
    private boolean openAlerts = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_UniRoom);
        super.onCreate(savedInstanceState);
        openAlerts = getIntent().getBooleanExtra(EXTRA_OPEN_ALERTS, false);

        if (AppState.get().isSignedIn()) {
            openHome();
            return;
        }

        setContentView(R.layout.activity_login);
        Ui.edgeToEdge(this);
        Ui.statusBarIcons(this, true);
        Ui.applyInsets(findViewById(R.id.login_root), findViewById(R.id.login_header), findViewById(R.id.login_sheet));

        username = new FormField(findViewById(R.id.field_username), "Username or school email *")
                .icon(R.drawable.ic_user)
                .hint("Your username or email")
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        password = new FormField(findViewById(R.id.field_password), "Password *")
                .icon(R.drawable.ic_lock)
                .hint("Enter your password")
                .password();
        password.input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        password.input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); return true; }
            return false;
        });

        errorBox = findViewById(R.id.login_error);
        errorText = findViewById(R.id.login_error_text);
        signInButton = findViewById(R.id.btn_sign_in);
        signInLabel = findViewById(R.id.sign_in_label);
        spinner = findViewById(R.id.sign_in_spinner);

        signInButton.setOnClickListener(v -> submit());

        // Signed in before (and not logged out): go straight to the dashboard.
        JSONObject saved = SavedSession.user(this);
        String savedToken = SavedSession.token(this);
        if (saved != null && savedToken != null) {
            username.setText(saved.optString("username", ""));
            continueAs(saved, savedToken, true);
        }
    }

    private void submit() {
        if (loading) return;
        final String login = username.text().trim();
        final String pass = password.text();
        username.setError(null);
        password.setError(null);
        if (login.isEmpty() || pass.isEmpty()) {
            if (login.isEmpty()) username.setError("Required");
            if (pass.isEmpty()) password.setError("Required");
            showError("Please complete the required fields marked with *.");
            return;
        }
        errorBox.setVisibility(View.GONE);
        signIn(login, pass);
    }

    /** Checks the username/password with the API. */
    private void signIn(String login, String pass) {
        setBusy(true);
        ApiClient.login(login, pass, (json, error) -> {
            if (isFinishing()) return;

            JSONObject u = json == null ? null : json.optJSONObject("user");
            if (!ApiClient.ok(json) || u == null) {
                setBusy(false);
                showError(ApiClient.message(json, error, "Invalid username/email or password."));
                return;
            }

            String role = u.optString("role", "");
            if (!"student".equalsIgnoreCase(role) && !"faculty".equalsIgnoreCase(role)) {
                setBusy(false);
                showError("Admin accounts use the UNIROOM admin website, not the mobile app.");
                return;
            }

            // First sign-in: the temporary password must be replaced first.
            if (u.optBoolean("must_change_password", false)) {
                setBusy(false);
                askForNewPassword(login, pass, u);
                return;
            }

            String token = json.optString("token", "");
            if (token.isEmpty()) {
                setBusy(false);
                showError("Unable to sign in. Please try again.");
                return;
            }
            continueAs(u, token, false);
        });
    }

    /**
     * Signed in with a real password: load the user's account, classes, rooms
     * and notifications from the server, then open the dashboard. Works for
     * every student and faculty account the admin creates.
     */
    private void continueAs(JSONObject u, String token, boolean restoring) {
        setBusy(true);
        DataSync.signIn(this, u, token, (result, message) -> {
            if (isFinishing()) return;
            if (result == DataSync.EXPIRED) {
                // Saved sign-in is too old: sign in again.
                SavedSession.clear(this);
                NotificationCheckJob.cancel(this);
                setBusy(false);
                showError(restoring ? "Please sign in again." : message);
                return;
            }
            if (result == DataSync.FAILED) {
                setBusy(false);
                showError(message == null ? "Unable to sign in. Please try again." : message);
                return;
            }

            SavedSession.save(this, u, token);
            if (result == DataSync.OFFLINE) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
            if (AppState.get().getAccount().isStudent()) {
                if (result == DataSync.OK) {
                    // What is already there is shown in the app, not as phone notifications.
                    JSONObject alerts = SavedSession.cache(this, SavedSession.CACHE_ALERTS);
                    if (alerts != null) Notifier.showNew(this, ApiClient.data(alerts), true);
                }
                NotificationCheckJob.schedule(this);
            }
            openHome();
        });
    }

    /** First sign-in with a temporary password: choose a new one (required). */
    private void askForNewPassword(String login, String temporaryPassword, JSONObject u) {
        int pad = Ui.dp(this, 20);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, Ui.dp(this, 8), pad, 0);

        EditText next = passwordInput("New password *");
        EditText confirm = passwordInput("Confirm new password *");
        TextView rules = new TextView(this);
        rules.setText("At least 8 characters, with letters and numbers.");
        rules.setTextColor(Ui.color(this, R.color.muted));
        rules.setPadding(0, Ui.dp(this, 6), 0, 0);
        TextView problem = new TextView(this);
        problem.setTextColor(Color.parseColor("#B42318"));
        problem.setPadding(0, Ui.dp(this, 6), 0, 0);

        box.addView(next);
        box.addView(confirm);
        box.addView(rules);
        box.addView(problem);

        String first = u.optString("first_name", "");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change your temporary password")
                .setMessage("Hi " + first + "! You signed in with the temporary password from your email. "
                        + "Choose your own password before you can use UNIROOM.")
                .setView(box)
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> showError("You must change your temporary password before you can sign in."))
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String pw = next.getText().toString();
                String again = confirm.getText().toString();

                if (pw.isEmpty() || again.isEmpty()) {
                    problem.setText("Please complete the required fields marked with *.");
                    return;
                }
                if (pw.length() < 8 || !pw.matches(".*[A-Za-z].*") || !pw.matches(".*[0-9].*")) {
                    problem.setText("Use at least 8 characters with letters and numbers.");
                    return;
                }
                if (pw.equals(temporaryPassword)) {
                    problem.setText("Use a different password from the temporary one.");
                    return;
                }
                if (!pw.equals(again)) {
                    problem.setText("Passwords do not match.");
                    return;
                }

                save.setEnabled(false);
                problem.setText("Saving…");
                ApiClient.changePassword(login, temporaryPassword, pw, (json, error) -> {
                    if (isFinishing()) return;
                    save.setEnabled(true);
                    if (ApiClient.ok(json)) {
                        dialog.dismiss();
                        password.setText("");
                        errorBox.setVisibility(View.GONE);
                        // Sign in with the new password (this gives the login token).
                        signIn(login, pw);
                    } else {
                        problem.setText(ApiClient.message(json, error, "Unable to change the password."));
                    }
                });
            });
        });

        dialog.show();
    }

    private EditText passwordInput(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    private void setBusy(boolean busy) {
        loading = busy;
        Buttons.setLoading(signInButton, signInLabel, spinner, busy, busy ? "Signing in…" : "Sign in");
    }

    private void showError(String message) {
        errorText.setText(message);
        errorBox.setVisibility(View.VISIBLE);
    }

    private void openHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_ALERTS, openAlerts);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
