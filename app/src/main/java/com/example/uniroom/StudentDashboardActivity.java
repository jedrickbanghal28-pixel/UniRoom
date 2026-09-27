package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ListResponse;
import com.example.uniroom.model.Models.Schedule;
import com.example.uniroom.model.Models.StudentRoomResponse;
import com.example.uniroom.network.Client;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private int userId;
    private Session session;
    private TextView welcomeText, sectionText;
    private TextView roomStatusText, roomNameText, roomDetailsText;
    private TextView nextStatusText, nextClassText, nextClassDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        session = new Session(this);
        userId = session.id();
        if (userId <= 0) userId = getIntent().getIntExtra("user_id", 0);

        welcomeText = findViewById(R.id.welcomeText);
        sectionText = findViewById(R.id.sectionText);
        roomStatusText = findViewById(R.id.roomStatusText);
        roomNameText = findViewById(R.id.roomNameText);
        roomDetailsText = findViewById(R.id.roomDetailsText);
        nextStatusText = findViewById(R.id.nextStatusText);
        nextClassText = findViewById(R.id.nextClassText);
        nextClassDetails = findViewById(R.id.nextClassDetails);

        String firstName = getIntent().getStringExtra("first_name");
        if (firstName == null || firstName.isEmpty()) firstName = session.name().split(" ")[0];
        welcomeText.setText("Welcome, " + firstName + "!");

        String program = session.preferencesProgram();
        String section = session.preferencesSection();
        if (section != null && !section.isEmpty()) {
            sectionText.setText((program == null || program.isEmpty() ? "" : program + " • ") + section);
        }

        findViewById(R.id.scheduleButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        findViewById(R.id.findRoomButton).setOnClickListener(v ->
                startActivity(new Intent(this, FindRoomActivity.class))
        );

        findViewById(R.id.alertsButton).setOnClickListener(v ->
                startActivity(new Intent(this, AlertsActivity.class))
        );

        findViewById(R.id.profileButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        findViewById(R.id.roomCard).setOnClickListener(v ->
                startActivity(new Intent(this, FindRoomActivity.class))
        );

        loadAssignedRoom();
        loadNextClass();
    }

    private void loadAssignedRoom() {
        if (userId <= 0) {
            showNoRoom("Please log in again.");
            return;
        }

        Client.api().studentRoom(userId).enqueue(new Callback<StudentRoomResponse>() {
            @Override
            public void onResponse(Call<StudentRoomResponse> call, Response<StudentRoomResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().room != null) {
                    StudentRoomResponse result = response.body();
                    roomStatusText.setText("ASSIGNED ROOM");
                    roomNameText.setText(result.room.room_name);
                    roomDetailsText.setText(result.room.building + " • Floor " + result.room.floor + " • " + result.room.section_name);
                } else {
                    showNoRoom(response.body() == null ? "Unable to load your room." : response.body().message);
                }
            }

            @Override
            public void onFailure(Call<StudentRoomResponse> call, Throwable t) {
                showNoRoom("Unable to connect to the server.");
            }
        });
    }

    private void loadNextClass() {
        if (userId <= 0) return;

        Client.api().schedule(userId).enqueue(new Callback<ListResponse<Schedule>>() {
            @Override
            public void onResponse(Call<ListResponse<Schedule>> call, Response<ListResponse<Schedule>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    showNoClass("Schedule unavailable");
                    return;
                }

                Schedule next = findNextClass(response.body().data);
                if (next == null) {
                    nextStatusText.setText("TODAY");
                    nextClassText.setText("No class scheduled today");
                    nextClassDetails.setText("Open My Schedule to view the full week.");
                } else {
                    nextStatusText.setText("NEXT CLASS • " + next.day_of_week.toUpperCase(Locale.US));
                    nextClassText.setText(next.subject_code + " — " + next.subject_name);
                    nextClassDetails.setText(formatTime(next.start_time) + "–" + formatTime(next.end_time) + " • " + next.room_name + " • " + next.faculty_name);
                }
            }

            @Override
            public void onFailure(Call<ListResponse<Schedule>> call, Throwable t) {
                showNoClass("Schedule unavailable");
            }
        });
    }

    private Schedule findNextClass(List<Schedule> schedules) {
        String today = new SimpleDateFormat("EEEE", Locale.US).format(Calendar.getInstance().getTime());
        int now = Integer.parseInt(new SimpleDateFormat("HHmm", Locale.US).format(Calendar.getInstance().getTime()));
        Schedule firstToday = null;

        for (Schedule item : schedules) {
            if (today.equalsIgnoreCase(item.day_of_week)) {
                int start = parseTime(item.start_time);
                if (start >= now && (firstToday == null || start < parseTime(firstToday.start_time))) firstToday = item;
            }
        }
        if (firstToday != null) return firstToday;

        for (Schedule item : schedules) {
            if (today.equalsIgnoreCase(item.day_of_week)) return item;
        }
        return schedules.isEmpty() ? null : schedules.get(0);
    }

    private int parseTime(String time) {
        try { return Integer.parseInt(time.replace(":", "")); }
        catch (Exception e) { return 9999; }
    }

    private String formatTime(String time) {
        try {
            java.text.SimpleDateFormat input = new java.text.SimpleDateFormat("HH:mm", Locale.US);
            java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("h:mm a", Locale.US);
            return output.format(input.parse(time));
        } catch (Exception e) { return time; }
    }

    private void showNoRoom(String message) {
        roomStatusText.setText("NO ROOM ASSIGNED");
        roomNameText.setText("No room yet");
        roomDetailsText.setText(message == null ? "Ask your department for assistance." : message);
    }

    private void showNoClass(String message) {
        nextStatusText.setText("TODAY");
        nextClassText.setText(message);
        nextClassDetails.setText("Open My Schedule to try again.");
    }
}
