package com.uniroom.app.screens;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.ApiClient;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.DemoData;
import com.uniroom.app.model.Account;
import com.uniroom.app.model.Program;
import com.uniroom.app.model.Room;
import com.uniroom.app.ui.Buttons;
import com.uniroom.app.ui.Header;
import com.uniroom.app.ui.Segmented;
import com.uniroom.app.ui.SelectField;
import com.uniroom.app.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Faculty: send a room change or an announcement to the students of one
 * section, or of the whole course ("All sections"). The notification is saved
 * in the database and shows up in those students' alerts and as a phone
 * notification.
 */
public class SendNotificationActivity extends Activity {

    private Account account;
    private Program program;
    private String section = "";
    private boolean transfer = true;
    private boolean sending = false;
    private int recipients = -1;
    /** Chip that sends to every section of the course. */
    private static final String ALL_SECTIONS = "All sections";

    private SelectField fromRoom, toRoom;
    private EditText message;
    private TextView sectionError, messageError, messageLabel, messageCount, previewTitle, previewBody;
    private LinearLayout sendButton;
    private TextView sendLabel;
    private ProgressBar sendSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        account = AppState.get().getAccount();
        setTheme(Ui.themeFor(account));
        super.onCreate(savedInstanceState);
        if (account == null || !account.isFaculty()) { finish(); return; }
        program = AppState.get().getFacultyProgram();

        setContentView(R.layout.activity_notify);
        Ui.edgeToEdge(this);
        Ui.statusBarIcons(this, false);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.status_spacer), findViewById(R.id.footer));

        Header.bind(findViewById(R.id.header), "Notify a section",
                program.portalLabel + " · room changes and updates", this::finish);

        sectionError = findViewById(R.id.section_error);
        messageError = findViewById(R.id.message_error);
        messageLabel = findViewById(R.id.message_label);
        messageCount = findViewById(R.id.message_count);
        previewTitle = findViewById(R.id.preview_title);
        previewBody = findViewById(R.id.preview_body);
        message = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.btn_send);
        sendLabel = findViewById(R.id.send_label);
        sendSpinner = findViewById(R.id.send_spinner);

        // Type: Room change | Announcement
        Segmented.attach(findViewById(R.id.type_switch), new String[]{"Room change", "Announcement"},
                0, false, 40, index -> {
                    transfer = index == 0;
                    messageError.setVisibility(View.GONE);
                    update();
                });

        // From (my rooms) -> To (any 3rd floor room)
        List<String> mine = AppState.get().assignedRooms();
        List<String> all = new ArrayList<>();
        for (Room r : DemoData.roomsOnFloor(3)) all.add(r.code);
        fromRoom = new SelectField(findViewById(R.id.f_from), "From").setOptions(mine, "Room");
        if (!mine.isEmpty()) fromRoom.setValue(mine.get(0));
        toRoom = new SelectField(findViewById(R.id.f_to), "To").setOptions(all, "Room");
        fromRoom.onChange(v -> update());
        toRoom.onChange(v -> update());

        message.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                messageError.setVisibility(View.GONE);
                message.setBackgroundResource(R.drawable.bg_field);
                update();
            }
        });

        sendButton.setOnClickListener(v -> send());
        findViewById(R.id.btn_done).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send_another).setOnClickListener(v -> reset());

        Ui.label(findViewById(R.id.section_label), "Send to *");
        buildSectionChips();
        update();
    }

    private void buildSectionChips() {
        LinearLayout row = findViewById(R.id.section_row);
        row.removeAllViews();
        List<String> chips = new ArrayList<>();
        chips.add(ALL_SECTIONS);
        for (String s : program.sections) chips.add(s);
        for (final String s : chips) {
            boolean active = s.equals(section);
            TextView chip = new TextView(this);
            chip.setText(s);
            chip.setTextAppearance(R.style.T_SmallSemi);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
            chip.setBackgroundResource(active ? R.drawable.bg_primary_pill : R.drawable.bg_outline_pill);
            chip.setForeground(getDrawable(R.drawable.ripple_round_16));
            chip.setTextColor(active ? 0xFFFFFFFF : getColor(R.color.ink));
            chip.setSelected(active);
            chip.setOnClickListener(v -> {
                section = s;
                sectionError.setVisibility(View.GONE);
                buildSectionChips();
                update();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 40));
            lp.setMarginEnd(Ui.dp(this, 8));
            row.addView(chip, lp);
        }
    }

    /** Updates labels, counter, preview and the send button text. */
    private void update() {
        findViewById(R.id.transfer_row).setVisibility(transfer ? View.VISIBLE : View.GONE);

        if (transfer) {
            SpannableStringBuilder sb = new SpannableStringBuilder("Message ");
            int start = sb.length();
            sb.append("(optional)");
            sb.setSpan(new ForegroundColorSpan(getColor(R.color.muted)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            messageLabel.setText(sb);
            message.setHint("Add details, e.g. “Bring your laptops.”");
        } else {
            messageLabel.setText("Message");
            message.setHint("What should your students know?");
        }
        String msg = message.getText().toString().trim();
        messageCount.setText(message.length() + "/200");

        String from = fromRoom.getValue().isEmpty() ? "—" : fromRoom.getValue();
        String to = toRoom.getValue().isEmpty() ? "—" : toRoom.getValue();
        previewTitle.setText(transfer ? "Room change: " + from + " → " + to
                : "Announcement from Prof. " + account.teacher.lastName);
        if (!msg.isEmpty()) previewBody.setText(msg);
        else if (transfer) previewBody.setText("Your class moves to "
                + (toRoom.getValue().isEmpty() ? "a new room" : toRoom.getValue()) + " starting today. Same time.");
        else previewBody.setText("Your message will appear here.");

        if (!sending) sendLabel.setText(section.isEmpty() ? "Send notification"
                : ALL_SECTIONS.equals(section) ? "Send to all " + program.label + " students"
                : "Send to " + section);
    }

    private void send() {
        if (sending) return;
        // Start fresh each time, like the prototype.
        fromRoom.setError(null);
        toRoom.setError(null);
        sectionError.setVisibility(View.GONE);
        messageError.setVisibility(View.GONE);
        message.setBackgroundResource(R.drawable.bg_field);
        boolean ok = true;
        if (section.isEmpty()) {
            sectionError.setText("Choose a section (or All sections)");
            sectionError.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (transfer) {
            if (fromRoom.getValue().isEmpty()) { fromRoom.setError("Required"); ok = false; }
            if (toRoom.getValue().isEmpty()) { toRoom.setError("Required"); ok = false; }
            else if (toRoom.getValue().equals(fromRoom.getValue())) { toRoom.setError("Pick a different room"); ok = false; }
        } else if (message.getText().toString().trim().isEmpty()) {
            messageError.setText("Write a message for your students");
            messageError.setVisibility(View.VISIBLE);
            message.setBackgroundResource(R.drawable.bg_field_error);
            ok = false;
        }
        if (!ok) return;

        sending = true;
        Buttons.setLoading(sendButton, sendLabel, sendSpinner, true, "Sending…");
        final String title = previewTitle.getText().toString();
        final String body = previewBody.getText().toString();
        final String target = ALL_SECTIONS.equals(section) ? "" : section;

        ApiClient.sendNotification(AppState.get().getToken(), program.name(), target,
                transfer ? "transfer" : "announcement", title, body, (json, error) -> {
                    if (isFinishing()) return;
                    if (ApiClient.ok(json)) {
                        recipients = json.optInt("recipients", -1);
                        showSent();
                        return;
                    }
                    sending = false;
                    Buttons.setLoading(sendButton, sendLabel, sendSpinner, false, "Send notification");
                    update();
                    String msg = ApiClient.sessionExpired(json)
                            ? "Your sign-in has expired. Please log out and sign in again."
                            : ApiClient.message(json, error, "Unable to send the notification.");
                    sectionError.setText(msg);
                    sectionError.setVisibility(View.VISIBLE);
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
                });
    }

    private void showSent() {
        sending = false;
        Buttons.setLoading(sendButton, sendLabel, sendSpinner, false, "Send notification");
        findViewById(R.id.form_scroll).setVisibility(View.GONE);
        findViewById(R.id.footer_line).setVisibility(View.GONE);
        findViewById(R.id.footer_inner).setVisibility(View.GONE);
        View sent = findViewById(R.id.sent_view);
        sent.setVisibility(View.VISIBLE);
        findViewById(R.id.sent_check).startAnimation(AnimationUtils.loadAnimation(this, R.anim.zoom_in));

        String who = ALL_SECTIONS.equals(section) ? "all " + program.label + " sections" : section;
        SpannableStringBuilder sb = new SpannableStringBuilder(
                recipients >= 0 ? recipients + (recipients == 1 ? " student in " : " students in ") : "Students in ");
        int start = sb.length();
        sb.append(who);
        sb.setSpan(new ForegroundColorSpan(getColor(R.color.ink)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (android.os.Build.VERSION.SDK_INT >= 28) {   // TypefaceSpan(Typeface) needs Android 9+
            sb.setSpan(new android.text.style.TypefaceSpan(getResources().getFont(R.font.inter_semibold)),
                    start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        sb.append(" will see it in their notifications.");
        ((TextView) findViewById(R.id.sent_text)).setText(sb);
    }

    private void reset() {
        section = "";
        toRoom.setValue("");
        toRoom.setError(null);
        fromRoom.setError(null);
        message.setText("");
        sectionError.setVisibility(View.GONE);
        messageError.setVisibility(View.GONE);
        buildSectionChips();
        findViewById(R.id.sent_view).setVisibility(View.GONE);
        findViewById(R.id.form_scroll).setVisibility(View.VISIBLE);
        findViewById(R.id.footer_line).setVisibility(View.VISIBLE);
        findViewById(R.id.footer_inner).setVisibility(View.VISIBLE);
        update();
    }
}
