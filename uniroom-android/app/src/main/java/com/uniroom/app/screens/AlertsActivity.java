package com.uniroom.app.screens;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.ApiClient;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.StudentMapper;
import com.uniroom.app.model.Account;
import com.uniroom.app.model.AppAlert;
import com.uniroom.app.ui.Header;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Ui;

import java.util.List;

/**
 * Students: room changes and announcements their instructors sent to their
 * course/section. Loaded from the database; read status is saved there too.
 */
public class AlertsActivity extends Activity {

    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        account = AppState.get().getAccount();
        setTheme(Ui.themeFor(account));
        super.onCreate(savedInstanceState);
        if (account == null || !account.isStudent()) { finish(); return; }

        setContentView(R.layout.activity_alerts);
        Ui.edgeToEdge(this);
        Ui.statusBarIcons(this, false);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.status_spacer), findViewById(R.id.content));

        Rows.emptyState(findViewById(R.id.alerts_empty), R.drawable.ic_bell_off, "You’re all caught up",
                "Room changes and announcements from your instructors will appear here.");
        render();
        load();
    }

    /** Gets the latest notifications from the server. */
    private void load() {
        String token = AppState.get().getToken();
        if (token == null) return;
        ApiClient.notifications(token, (json, error) -> {
            if (isFinishing()) return;
            if (ApiClient.ok(json)) {
                AppState.get().setAlerts(StudentMapper.alerts(ApiClient.data(json), account.student.program));
                render();
            } else if (AppState.get().myAlerts().isEmpty()) {
                android.widget.Toast.makeText(this,
                        ApiClient.message(json, error, "Unable to load notifications."),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void render() {
        List<AppAlert> alerts = AppState.get().myAlerts();
        boolean hasUnread = false;
        for (AppAlert a : alerts) if (a.unread) hasUnread = true;

        View header = findViewById(R.id.header);
        Header.bind(header, "Notifications", null, this::finish);
        Header.action(header, hasUnread ? "Mark all read" : null, () -> {
            AppState.get().markAllRead();
            render();
            ApiClient.markAllRead(AppState.get().getToken(), (json, error) -> { });
        });

        LinearLayout list = findViewById(R.id.alerts_list);
        list.removeAllViews();
        list.setClipToOutline(true);
        list.setVisibility(alerts.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.alerts_empty).setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inf = LayoutInflater.from(this);
        Typeface semibold = getResources().getFont(R.font.inter_semibold);
        Typeface medium = getResources().getFont(R.font.inter_medium);
        for (final AppAlert a : alerts) {
            View row = inf.inflate(R.layout.item_alert, list, false);
            boolean transfer = AppAlert.TRANSFER.equals(a.kind);

            ImageView icon = row.findViewById(R.id.alert_icon);
            icon.setImageResource(transfer ? R.drawable.ic_arrow_right_left : R.drawable.ic_megaphone);
            icon.setColorFilter(transfer ? getColor(R.color.warning) : Ui.primary(this));
            Ui.tint(icon, transfer ? getColor(R.color.warning_soft) : Ui.primarySoft(this));

            TextView title = row.findViewById(R.id.alert_title);
            title.setText(a.title);
            title.setTypeface(a.unread ? semibold : medium);
            Rows.text(row, R.id.alert_time, a.time);
            Rows.text(row, R.id.alert_body, a.body);
            Rows.text(row, R.id.alert_from, "From " + a.from);

            row.findViewById(R.id.alert_dot).setVisibility(a.unread ? View.VISIBLE : View.INVISIBLE);
            if (a.unread) row.setBackgroundColor(Ui.attrColor(this, R.attr.uniPrimaryTint));
            row.setOnClickListener(v -> {
                if (!a.unread) return;
                AppState.get().markRead(a.id);
                render();
                try {
                    ApiClient.markRead(AppState.get().getToken(), Integer.parseInt(a.id), (json, error) -> { });
                } catch (NumberFormatException ignored) { }
            });
            list.addView(row);
        }
    }
}
