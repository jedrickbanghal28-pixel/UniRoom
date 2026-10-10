package com.uniroom.app.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.Clock;
import com.uniroom.app.data.DemoData;
import com.uniroom.app.model.AppAlert;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Role;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.Student;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.NextClassCard;
import com.uniroom.app.ui.Rows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Student home: greeting, next class, latest alert, later today, rooms this week. */
public class StudentHomeTab extends TabPage {

    public StudentHomeTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_student_home, parent, false);
        v.findViewById(R.id.home_bell).setOnClickListener(x -> activity.openAlerts());
        v.findViewById(R.id.home_alert).setOnClickListener(x -> activity.openAlerts());
        v.findViewById(R.id.btn_full_schedule).setOnClickListener(x -> activity.showTab(MainActivity.TAB_SCHEDULE));
        return v;
    }

    @Override
    public boolean coloredTop() { return true; }

    @Override
    public void refresh() {
        AppState state = AppState.get();
        Student student = state.getAccount().student;
        List<ClassSession> sessions = state.mySessions();
        List<ClassSession> todays = Clock.sessionsForDay(sessions, Clock.todayIndex());
        ClassSession next = Clock.nextSession(sessions);

        // Header
        ((TextView) find(R.id.home_date)).setText(Clock.longDate(Clock.today()));
        ((TextView) find(R.id.home_greeting)).setText(Clock.greeting() + ", " + student.firstName);
        ((TextView) find(R.id.home_context)).setText(student.program.label + " · " + student.section);

        // Unread badge on the bell
        List<AppAlert> unread = new ArrayList<>();
        for (AppAlert a : state.myAlerts()) if (a.unread) unread.add(a);
        TextView badge = (TextView) find(R.id.home_badge);
        badge.setVisibility(unread.isEmpty() ? View.GONE : View.VISIBLE);
        badge.setText(String.valueOf(unread.size()));
        find(R.id.home_bell).setContentDescription(unread.isEmpty()
                ? "Notifications" : "Notifications, " + unread.size() + " unread");

        // Next class card + "View full schedule"
        View card = find(R.id.next_card);
        NextClassCard.bind(card, next, Role.STUDENT, !todays.isEmpty());
        card.findViewById(R.id.btn_full_schedule).setVisibility(View.VISIBLE);

        // Latest unread alert
        View alert = find(R.id.home_alert);
        if (unread.isEmpty()) {
            alert.setVisibility(View.GONE);
        } else {
            AppAlert latest = unread.get(0);
            alert.setVisibility(View.VISIBLE);
            ((TextView) find(R.id.home_alert_title)).setText(latest.title);
            ((TextView) find(R.id.home_alert_meta)).setText("From " + latest.from + " · " + latest.time);
        }

        // Later today (classes after the next one)
        List<ClassSession> later = new ArrayList<>();
        if (next != null) {
            for (ClassSession s : todays) {
                if (Clock.toMinutes(s.start) > Clock.toMinutes(next.start)) later.add(s);
            }
        }
        find(R.id.later_section).setVisibility(later.isEmpty() ? View.GONE : View.VISIBLE);
        Rows.dayClasses((LinearLayout) find(R.id.later_list), later, Role.STUDENT);

        // Rooms this week: each room once, with the days it is used
        Map<String, Set<Integer>> days = new LinkedHashMap<>();
        for (ClassSession s : sessions) {
            if (!days.containsKey(s.room)) days.put(s.room, new LinkedHashSet<Integer>());
            days.get(s.room).add(s.day);
        }
        LinearLayout week = (LinearLayout) find(R.id.week_list);
        week.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(activity);
        for (Map.Entry<String, Set<Integer>> e : days.entrySet()) {
            Room room = DemoData.findRoom(e.getKey());
            StringBuilder dayNames = new StringBuilder();
            for (int d : e.getValue()) {
                if (dayNames.length() > 0) dayNames.append(", ");
                dayNames.append(Clock.dayShort(d));
            }
            View row = inf.inflate(R.layout.item_week_room, week, false);
            Rows.text(row, R.id.week_code, e.getKey());
            Rows.text(row, R.id.week_meta, (room != null ? room.type : "Room") + " · 3rd floor");
            Rows.text(row, R.id.week_days, dayNames.toString());
            week.addView(row);
        }
        if (days.isEmpty()) Rows.note(week, "No rooms scheduled this week.");
    }
}
