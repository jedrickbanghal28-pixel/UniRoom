package com.uniroom.app.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.Clock;
import com.uniroom.app.data.DemoData;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Role;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.Teacher;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.NextClassCard;
import com.uniroom.app.ui.Pills;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Segmented;

import java.util.List;

/** Faculty home: greeting, program switcher, next class, notify button, today's classes, rooms. */
public class FacultyHomeTab extends TabPage {

    public FacultyHomeTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_faculty_home, parent, false);
        v.findViewById(R.id.btn_notify).setOnClickListener(x -> activity.openNotify());
        v.findViewById(R.id.link_manage).setOnClickListener(x -> activity.showTab(MainActivity.TAB_ROOMS));
        return v;
    }

    @Override
    public boolean coloredTop() { return true; }

    @Override
    public void refresh() {
        AppState state = AppState.get();
        Teacher t = state.getAccount().teacher;
        List<ClassSession> sessions = state.mySessions();
        List<ClassSession> todays = Clock.sessionsForDay(sessions, Clock.todayIndex());

        ((TextView) find(R.id.home_date)).setText(Clock.longDate(Clock.today()));
        ((TextView) find(R.id.home_greeting)).setText("Welcome, Prof. " + t.lastName);
        ((TextView) find(R.id.home_subject)).setText(t.subject);
        ((TextView) find(R.id.home_initials)).setText(t.initials);
        Segmented.programSwitcher((LinearLayout) find(R.id.program_switcher), true, this::refresh);

        NextClassCard.bind(find(R.id.next_card), Clock.nextSession(sessions), Role.FACULTY, !todays.isEmpty());

        LinearLayout today = (LinearLayout) find(R.id.today_list);
        if (todays.isEmpty()) Rows.note(today, "No classes for this program today.");
        else Rows.dayClasses(today, todays, Role.FACULTY);

        // Room cards
        LinearLayout row = (LinearLayout) find(R.id.rooms_row);
        row.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(activity);
        int count = 0;
        for (Room r : DemoData.ROOMS) {
            if (!state.assignedRooms().contains(r.code)) continue;
            View card = inf.inflate(R.layout.item_room_card, row, false);
            Rows.text(card, R.id.card_code, r.code);
            Rows.text(card, R.id.card_type, r.type);
            ((FrameLayout) card.findViewById(R.id.card_pill)).addView(Pills.status(activity, r.status));
            row.addView(card);
            count++;
        }
        find(R.id.rooms_scroll).setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        LinearLayout empty = (LinearLayout) find(R.id.rooms_empty);
        empty.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
        if (count == 0) Rows.note(empty, "No rooms assigned for this program.");
    }
}
