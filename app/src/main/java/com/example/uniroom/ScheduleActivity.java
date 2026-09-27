package com.example.uniroom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ListResponse;
import com.example.uniroom.model.Models.Schedule;
import com.example.uniroom.network.Client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleActivity extends AppCompatActivity {

    private int userId;
    private Calendar weekStart;
    private Calendar selectedDate;
    private LinearLayout dateContainer, scheduleContainer;
    private TextView monthText, selectedDateText;
    private List<Schedule> schedules = new ArrayList<>();

    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.US);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
    private final SimpleDateFormat numberFormat = new SimpleDateFormat("d", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        Session session = new Session(this);
        userId = session.id();
        if (userId <= 0) userId = getIntent().getIntExtra("user_id", 0);

        dateContainer = findViewById(R.id.dateContainer);
        scheduleContainer = findViewById(R.id.scheduleContainer);
        monthText = findViewById(R.id.monthText);
        selectedDateText = findViewById(R.id.selectedDateText);

        Calendar today = Calendar.getInstance();
        selectedDate = (Calendar) today.clone();
        weekStart = (Calendar) today.clone();
        moveToMonday(weekStart);

        findViewById(R.id.previousWeekButton).setOnClickListener(v -> {
            weekStart.add(Calendar.DAY_OF_MONTH, -7);
            selectedDate.add(Calendar.DAY_OF_MONTH, -7);
            showWeek();
        });

        findViewById(R.id.nextWeekButton).setOnClickListener(v -> {
            weekStart.add(Calendar.DAY_OF_MONTH, 7);
            selectedDate.add(Calendar.DAY_OF_MONTH, 7);
            showWeek();
        });

        setupBottomNavigation();
        showWeek();
        loadSchedule();
    }

    private void moveToMonday(Calendar date) {
        int day = date.get(Calendar.DAY_OF_WEEK);
        int diff = (day == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - day;
        date.add(Calendar.DAY_OF_MONTH, diff);
    }

    private void showWeek() {
        dateContainer.removeAllViews();
        monthText.setText(monthFormat.format(weekStart.getTime()));
        selectedDateText.setText(fullDateFormat.format(selectedDate.getTime()));

        for (int i = 0; i < 7; i++) {
            Calendar date = (Calendar) weekStart.clone();
            date.add(Calendar.DAY_OF_MONTH, i);
            dateContainer.addView(createDateView(date));
        }

        showScheduleForSelectedDate();
    }

    private TextView createDateView(Calendar date) {
        TextView view = new TextView(this);
        view.setText(dayFormat.format(date.getTime()) + "\n" + numberFormat.format(date.getTime()));
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, Typeface.BOLD);
        view.setPadding(8, 8, 8, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 58, 1);
        params.setMargins(3, 3, 3, 3);
        view.setLayoutParams(params);
        styleDateView(view, isSameDate(date, selectedDate));

        view.setOnClickListener(v -> {
            selectedDate = (Calendar) date.clone();
            showWeek();
        });
        return view;
    }

    private void styleDateView(TextView view, boolean selected) {
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(selected ? Color.rgb(46, 139, 139) : Color.rgb(31, 78, 121));
    }

    private boolean isSameDate(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private void loadSchedule() {
        if (userId <= 0) {
            Toast.makeText(this, "User session not found.", Toast.LENGTH_LONG).show();
            return;
        }

        Client.api().schedule(userId).enqueue(new Callback<ListResponse<Schedule>>() {
            @Override
            public void onResponse(Call<ListResponse<Schedule>> call, Response<ListResponse<Schedule>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    schedules = response.body().data;
                    showScheduleForSelectedDate();
                } else {
                    Toast.makeText(ScheduleActivity.this, "Unable to load schedule.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ListResponse<Schedule>> call, Throwable t) {
                Toast.makeText(ScheduleActivity.this, "Connection failed. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showScheduleForSelectedDate() {
        scheduleContainer.removeAllViews();
        String selectedDay = new SimpleDateFormat("EEEE", Locale.US).format(selectedDate.getTime());
        List<Schedule> daySchedules = new ArrayList<>();

        for (Schedule schedule : schedules) {
            if (selectedDay.equalsIgnoreCase(schedule.day_of_week)) daySchedules.add(schedule);
        }

        if (daySchedules.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No classes scheduled for " + selectedDay + ".");
            empty.setTextColor(Color.rgb(101, 113, 125));
            empty.setTextSize(16);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(20, 60, 20, 60);
            scheduleContainer.addView(empty);
            return;
        }

        for (Schedule schedule : daySchedules) addScheduleCard(schedule);
    }

    private void addScheduleCard(Schedule item) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(18);
        card.setCardElevation(2);
        card.setStrokeWidth(1);
        card.setStrokeColor(Color.rgb(220, 227, 234));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16, 16, 16, 16);

        TextView time = new TextView(this);
        time.setText(formatTime(item.start_time) + "\n" + formatTime(item.end_time));
        time.setTextColor(Color.rgb(31, 78, 121));
        time.setTextSize(13);
        time.setTypeface(null, Typeface.BOLD);
        time.setGravity(Gravity.CENTER);
        row.addView(time, new LinearLayout.LayoutParams(82, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(12, 0, 0, 0);

        TextView subject = new TextView(this);
        subject.setText(item.subject_code + " — " + item.subject_name);
        subject.setTextColor(Color.rgb(23, 32, 42));
        subject.setTextSize(16);
        subject.setTypeface(null, Typeface.BOLD);

        TextView room = new TextView(this);
        room.setText(item.room_name + " • " + item.building + " • " + item.faculty_name);
        room.setTextColor(Color.rgb(101, 113, 125));
        room.setTextSize(13);
        room.setPadding(0, 5, 0, 0);

        details.addView(subject);
        details.addView(room);
        row.addView(details, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 12);
        scheduleContainer.addView(card, params);
    }

    private String formatTime(String time) {
        try {
            java.text.SimpleDateFormat input = new java.text.SimpleDateFormat("HH:mm", Locale.US);
            java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("h:mm a", Locale.US);
            return output.format(input.parse(time));
        } catch (Exception e) {
            return time;
        }
    }

    private void setupBottomNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.scheduleButton).setOnClickListener(v -> { });

        findViewById(R.id.findRoomButton).setOnClickListener(v -> {
            startActivity(new Intent(this, FindRoomActivity.class));
            finish();
        });

        findViewById(R.id.profileButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });
    }
}
