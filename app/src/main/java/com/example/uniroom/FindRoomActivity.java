package com.example.uniroom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models.ListResponse;
import com.example.uniroom.model.Models.Room;
import com.example.uniroom.network.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindRoomActivity extends AppCompatActivity {

    private LinearLayout roomContainer;
    private List<Room> rooms = new ArrayList<>();
    private String activeFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_room);

        roomContainer = findViewById(R.id.roomContainer);
        setupFilters();
        setupBottomNavigation();
        loadRooms();
    }

    private void loadRooms() {
        roomContainer.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("Loading rooms...");
        loading.setTextColor(Color.rgb(101, 113, 125));
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(20, 60, 20, 60);
        roomContainer.addView(loading);

        Client.api().rooms("").enqueue(new Callback<ListResponse<Room>>() {
            @Override
            public void onResponse(Call<ListResponse<Room>> call, Response<ListResponse<Room>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    rooms = response.body().data;
                    renderRooms();
                } else {
                    showMessage("Unable to load rooms.");
                }
            }

            @Override
            public void onFailure(Call<ListResponse<Room>> call, Throwable t) {
                showMessage("Could not connect to the room server.");
            }
        });
    }

    private void renderRooms() {
        roomContainer.removeAllViews();
        int count = 0;

        for (Room room : rooms) {
            if (!matchesFilter(room)) continue;
            addRoomCard(room);
            count++;
        }

        if (count == 0) showMessage("No rooms match this filter.");
    }

    private boolean matchesFilter(Room room) {
        if ("ALL".equals(activeFilter)) return true;
        if ("TECH".equals(activeFilter)) return room.building != null && room.building.toLowerCase(Locale.US).contains("tech");
        if ("FLOOR3".equals(activeFilter)) return room.floor == 3;
        if ("AVAILABLE".equals(activeFilter)) return room.status == null || !room.status.equalsIgnoreCase("occupied");
        return true;
    }

    private void addRoomCard(Room room) {
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(18);
        card.setCardElevation(2);
        card.setStrokeWidth(1);
        card.setStrokeColor(Color.rgb(220, 227, 234));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(18, 16, 18, 16);

        TextView title = new TextView(this);
        title.setText(room.room_name == null ? "Room" : room.room_name);
        title.setTextColor(Color.rgb(23, 32, 42));
        title.setTextSize(21);
        title.setTypeface(null, Typeface.BOLD);

        TextView location = new TextView(this);
        location.setText((room.building == null ? "" : room.building) + " • Floor " + room.floor);
        location.setTextColor(Color.rgb(31, 78, 121));
        location.setTextSize(14);
        location.setPadding(0, 4, 0, 0);

        TextView details = new TextView(this);
        details.setText("Capacity: " + room.capacity + "   •   Status: " + (room.status == null ? "Available" : room.status));
        details.setTextColor(Color.rgb(101, 113, 125));
        details.setTextSize(13);
        details.setPadding(0, 8, 0, 0);

        content.addView(title);
        content.addView(location);
        content.addView(details);
        card.addView(content);

        card.setOnClickListener(v -> Toast.makeText(this,
                room.room_name + " • " + room.building + " • Floor " + room.floor,
                Toast.LENGTH_SHORT).show());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 12);
        roomContainer.addView(card, params);
    }

    private void showMessage(String message) {
        roomContainer.removeAllViews();
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.rgb(101, 113, 125));
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER);
        text.setPadding(20, 60, 20, 60);
        roomContainer.addView(text);
    }

    private void setupFilters() {
        findViewById(R.id.allFilter).setOnClickListener(v -> selectFilter("ALL"));
        findViewById(R.id.techHubFilter).setOnClickListener(v -> selectFilter("TECH"));
        findViewById(R.id.floor3Filter).setOnClickListener(v -> selectFilter("FLOOR3"));
        findViewById(R.id.availableFilter).setOnClickListener(v -> selectFilter("AVAILABLE"));
    }

    private void selectFilter(String filter) {
        activeFilter = filter;
        renderRooms();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentDashboardActivity.class));
            finish();
        });

        findViewById(R.id.scheduleButton).setOnClickListener(v -> {
            startActivity(new Intent(this, ScheduleActivity.class));
            finish();
        });

        findViewById(R.id.findRoomButton).setOnClickListener(v -> { });

        findViewById(R.id.profileButton).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}
