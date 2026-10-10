package com.uniroom.app.screens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.ApiClient;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.SavedSession;
import com.uniroom.app.data.StudentMapper;
import com.uniroom.app.notify.NotificationCheckJob;
import com.uniroom.app.notify.Notifier;
import com.uniroom.app.model.Account;
import com.uniroom.app.tabs.FacultyHomeTab;
import com.uniroom.app.tabs.FindRoomTab;
import com.uniroom.app.tabs.MyRoomsTab;
import com.uniroom.app.tabs.ProfileTab;
import com.uniroom.app.tabs.ScheduleTab;
import com.uniroom.app.tabs.StudentHomeTab;
import com.uniroom.app.tabs.TabPage;
import com.uniroom.app.ui.Ui;

/** The signed-in part of the app: four tabs and a bottom navigation bar. */
public class MainActivity extends Activity {

    public static final int TAB_HOME = 0, TAB_SCHEDULE = 1, TAB_ROOMS = 2, TAB_PROFILE = 3;

    private Account account;
    private FrameLayout container;
    private View statusSpacer;
    private final View[] navItems = new View[4];
    private int currentTab = -1;
    private TabPage page;

    /** While the app is open, students check for new notifications every 30 seconds. */
    private static final long POLL_MS = 30_000;
    private final Handler poller = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::checkNotifications;
    private boolean visible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        account = AppState.get().getAccount();
        setTheme(Ui.themeFor(account));
        super.onCreate(savedInstanceState);

        if (account == null) {   // e.g. the app was restarted by Android: sign in again (restored automatically)
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        Ui.edgeToEdge(this);
        container = findViewById(R.id.tab_container);
        statusSpacer = findViewById(R.id.status_spacer);
        Ui.applyInsets(findViewById(R.id.root), statusSpacer, findViewById(R.id.bottom_nav));

        buildBottomNav();
        showTab(TAB_HOME);

        if (account.isStudent()) {
            Notifier.askPermission(this);
            if (getIntent().getBooleanExtra(LoginActivity.EXTRA_OPEN_ALERTS, false)) openAlerts();
        }
    }

    private void buildBottomNav() {
        LinearLayout nav = findViewById(R.id.bottom_nav);
        boolean student = account.isStudent();
        int[] icons = {R.drawable.ic_house, R.drawable.ic_calendar_days,
                student ? R.drawable.ic_search : R.drawable.ic_door_open, R.drawable.ic_user};
        String[] labels = {"Home", "Schedule", student ? "Find Room" : "My Rooms", "Profile"};
        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            View item = inf.inflate(R.layout.item_nav, nav, false);
            ((ImageView) item.findViewById(R.id.nav_icon)).setImageResource(icons[i]);
            ((TextView) item.findViewById(R.id.nav_label)).setText(labels[i]);
            item.setContentDescription(labels[i]);
            item.setOnClickListener(v -> showTab(index));
            nav.addView(item);
            navItems[i] = item;
        }
    }

    /** Opens a tab from scratch (like tapping it in the bottom bar). */
    public void showTab(int index) {
        currentTab = index;
        boolean student = account.isStudent();
        switch (index) {
            case TAB_HOME: page = student ? new StudentHomeTab(this) : new FacultyHomeTab(this); break;
            case TAB_SCHEDULE: page = new ScheduleTab(this); break;
            case TAB_ROOMS: page = student ? new FindRoomTab(this) : new MyRoomsTab(this); break;
            default: page = new ProfileTab(this); break;
        }
        container.removeAllViews();
        View v = page.getView(container);
        container.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        boolean colored = page.coloredTop();
        statusSpacer.setBackgroundColor(colored ? Ui.primary(this) : getColor(R.color.canvas));
        Ui.statusBarIcons(this, colored);
        updateNav();
    }

    private void updateNav() {
        for (int i = 0; i < 4; i++) {
            boolean active = i == currentTab;
            View item = navItems[i];
            View pill = item.findViewById(R.id.nav_pill);
            ImageView icon = item.findViewById(R.id.nav_icon);
            TextView label = item.findViewById(R.id.nav_label);
            Ui.tint(pill, active ? Ui.primarySoft(this) : 0x00000000);
            icon.setColorFilter(active ? Ui.primary(this) : getColor(R.color.muted));
            label.setTextColor(getColor(active ? R.color.ink : R.color.muted));
            label.setTypeface(getResources().getFont(active ? R.font.inter_semibold : R.font.inter_medium));
            item.setSelected(active);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from Alerts / Notify: update badges, rooms, etc.
        if (page != null) page.refresh();
        visible = true;
        if (account != null && account.isStudent()) {
            poller.removeCallbacks(poll);
            poller.post(poll);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
        poller.removeCallbacks(poll);
    }

    /** Gets the student's notifications from the server and updates the badge. */
    private void checkNotifications() {
        String token = AppState.get().getToken();
        if (!visible || account == null || !account.isStudent() || token == null) return;

        ApiClient.notifications(token, (json, error) -> {
            if (isFinishing() || AppState.get().getAccount() != account) return;
            if (ApiClient.sessionExpired(json)) {
                logout();
                return;
            }
            if (ApiClient.ok(json)) {
                AppState.get().setAlerts(StudentMapper.alerts(ApiClient.data(json), account.student.program));
                Notifier.showNew(this, ApiClient.data(json), false);
                if (page != null) page.refresh();
            }
            if (visible) poller.postDelayed(poll, POLL_MS);
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (currentTab != TAB_HOME) {
            showTab(TAB_HOME);
        } else {
            super.onBackPressed();
        }
    }

    // ---------- Navigation used by the tabs ----------

    public void openAlerts() {
        startActivity(new Intent(this, AlertsActivity.class));
    }

    public void openNotify() {
        startActivity(new Intent(this, SendNotificationActivity.class));
    }

    /** Called when the faculty program switcher changes: rebuild the current tab. */
    public void onProgramChanged() {
        if (page != null) page.refresh();
    }

    public void logout() {
        poller.removeCallbacks(poll);
        SavedSession.clear(this);
        NotificationCheckJob.cancel(this);
        AppState.get().logout();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
