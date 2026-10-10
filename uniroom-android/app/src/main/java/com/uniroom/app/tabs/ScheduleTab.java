package com.uniroom.app.tabs;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.Clock;
import com.uniroom.app.model.Account;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Segmented;
import com.uniroom.app.ui.Ui;

import java.util.Calendar;
import java.util.List;

/** Weekly schedule with a day picker. Faculty can switch between programs. */
public class ScheduleTab extends TabPage {

    private int weekOffset = 0;
    private int selectedDay = Clock.todayIndex();

    public ScheduleTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_schedule, parent, false);
        v.findViewById(R.id.week_prev).setOnClickListener(x -> { weekOffset--; refresh(); });
        v.findViewById(R.id.week_next).setOnClickListener(x -> { weekOffset++; refresh(); });
        return v;
    }

    @Override
    public void refresh() {
        Account account = AppState.get().getAccount();
        List<ClassSession> sessions = AppState.get().mySessions();
        Calendar selectedDate = Clock.dateOf(weekOffset, selectedDay);

        String context = account.isStudent()
                ? account.student.program.label + " · " + account.student.section
                : AppState.get().getFacultyProgram().portalLabel;
        ((TextView) find(R.id.sched_context)).setText(context + " · " + Clock.monthYear(selectedDate));

        LinearLayout switcher = (LinearLayout) find(R.id.program_switcher);
        if (account.isFaculty()) {
            switcher.setVisibility(View.VISIBLE);
            Segmented.programSwitcher(switcher, false, this::refresh);
        }

        buildDayStrip();

        // Selected day
        List<ClassSession> dayClasses = Clock.sessionsForDay(sessions, selectedDay);
        ((TextView) find(R.id.day_title)).setText(Clock.longDate(selectedDate));
        TextView count = (TextView) find(R.id.day_count);
        count.setVisibility(dayClasses.isEmpty() ? View.GONE : View.VISIBLE);
        count.setText(dayClasses.size() + (dayClasses.size() == 1 ? " class" : " classes"));

        LinearLayout list = (LinearLayout) find(R.id.day_list);
        list.removeAllViews();
        if (dayClasses.isEmpty()) {
            list.addView(Rows.newEmptyState(list, R.drawable.ic_calendar_off,
                    "No classes on " + Clock.dayLong(selectedDay),
                    "Enjoy the free day. Your next classes will show up here."));
            return;
        }
        LayoutInflater inf = LayoutInflater.from(activity);
        for (ClassSession s : dayClasses) {
            View row = inf.inflate(R.layout.item_schedule_class, list, false);
            Rows.text(row, R.id.sched_start, Clock.formatTime(s.start));
            Rows.text(row, R.id.sched_end, Clock.formatTime(s.end));
            Rows.text(row, R.id.sched_code, s.code);
            Rows.text(row, R.id.sched_title, s.title);
            Rows.text(row, R.id.sched_room, s.room + " · " + s.building);
            TextView person = row.findViewById(R.id.sched_person);
            person.setText(account.isStudent() ? s.instructor : s.section);
            person.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    account.isStudent() ? R.drawable.ic_user_sm : R.drawable.ic_users_sm, 0, 0, 0);
            boolean live = weekOffset == 0 && Clock.isOngoing(s);
            row.findViewById(R.id.sched_now).setVisibility(live ? View.VISIBLE : View.GONE);
            list.addView(row);
        }
    }

    private void buildDayStrip() {
        LinearLayout strip = (LinearLayout) find(R.id.day_strip);
        strip.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(activity);
        int primary = Ui.primary(activity);
        for (int i = 0; i < 7; i++) {
            final int day = i;
            boolean active = i == selectedDay;
            boolean isToday = weekOffset == 0 && i == Clock.todayIndex();
            View chip = inf.inflate(R.layout.item_day_chip, strip, false);
            TextView dayName = chip.findViewById(R.id.chip_day);
            TextView date = chip.findViewById(R.id.chip_date);
            dayName.setText(Clock.dayShort(i));
            date.setText(String.valueOf(Clock.dateOf(weekOffset, i).get(Calendar.DAY_OF_MONTH)));

            Ui.tint(chip, active ? primary : Color.TRANSPARENT);
            dayName.setTextColor(activity.getColor(active ? R.color.white_75 : R.color.muted));
            date.setTextColor(active ? Color.WHITE : activity.getColor(R.color.ink));
            View dot = chip.findViewById(R.id.chip_dot);
            Ui.tint(dot, isToday ? (active ? Color.WHITE : primary) : Color.TRANSPARENT);

            chip.setSelected(active);
            chip.setContentDescription(Clock.longDate(Clock.dateOf(weekOffset, i)));
            chip.setOnClickListener(v -> { selectedDay = day; refresh(); });
            strip.addView(chip);
        }
    }
}
