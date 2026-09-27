package com.example.uniroom;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniroom.model.Models;
import com.example.uniroom.model.Models.ApiResponse;
import com.example.uniroom.model.Models.DeleteRequest;
import com.example.uniroom.model.Models.ListResponse;
import com.example.uniroom.model.Models.Room;
import com.example.uniroom.model.Models.RoomRequest;
import com.example.uniroom.network.Client;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomCrudActivity extends AppCompatActivity {

    private LinearLayout roomContainer;
    private Button addRoomButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_crud);

        roomContainer = findViewById(R.id.roomContainer);
        addRoomButton = findViewById(R.id.addRoomButton);
        backButton = findViewById(R.id.backButton);

        loadRooms();

        addRoomButton.setOnClickListener(v -> showAddRoomDialog());

        backButton.setOnClickListener(v -> finish());
    }

    // ============================================================
    // LOAD ROOMS
    // ============================================================

    private void loadRooms() {

        Client.api().rooms("").enqueue(new Callback<ListResponse<Room>>() {

            @Override
            public void onResponse(
                    Call<ListResponse<Room>> call,
                    Response<ListResponse<Room>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    ListResponse<Room> result = response.body();

                    if (result.success && result.data != null) {

                        roomContainer.removeAllViews();

                        for (Room room : result.data) {
                            addRoomCard(room);
                        }

                    } else {

                        Toast.makeText(
                                RoomCrudActivity.this,
                                result.message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                } else {

                    Toast.makeText(
                            RoomCrudActivity.this,
                            "Server error: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<ListResponse<Room>> call,
                    Throwable t) {

                Toast.makeText(
                        RoomCrudActivity.this,
                        "Connection failed: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // ============================================================
    // ROOM CARD
    // ============================================================

    private void addRoomCard(Room room) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 15);

        card.setLayoutParams(cardParams);

        // Room name
        TextView roomName = new TextView(this);

        roomName.setText(
                room.room_name != null
                        ? room.room_name
                        : "Unnamed Room"
        );

        roomName.setTextSize(22);
        roomName.setTextColor(0xFF252B3A);
        roomName.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        card.addView(roomName);

        // Building
        TextView building = new TextView(this);

        building.setText(
                "Building: " +
                        (room.building != null
                                ? room.building
                                : "N/A")
        );

        building.setTextSize(16);
        building.setTextColor(0xFF52647A);

        card.addView(building);

        // Floor
        TextView floor = new TextView(this);

        floor.setText(
                "Floor: " + room.floor
        );

        floor.setTextSize(15);
        floor.setTextColor(0xFF777777);

        card.addView(floor);

        // Capacity
        TextView capacity = new TextView(this);

        capacity.setText(
                "Capacity: " + room.capacity
        );

        capacity.setTextSize(15);
        capacity.setTextColor(0xFF777777);

        card.addView(capacity);

        // Status
        TextView status = new TextView(this);

        status.setText(
                "Status: " +
                        (room.status != null
                                ? room.status
                                : "Available")
        );

        status.setTextSize(15);
        status.setTextColor(0xFF85180F);
        status.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(0, 5, 0, 10);

        status.setLayoutParams(statusParams);

        card.addView(status);

        // Button row
        LinearLayout buttonRow = new LinearLayout(this);

        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        // EDIT
        Button editButton = new Button(this);

        editButton.setText("EDIT");
        editButton.setTextColor(0xFFFFFFFF);
        editButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        0xFF85180F
                )
        );

        // DELETE
        Button deleteButton = new Button(this);

        deleteButton.setText("DELETE");
        deleteButton.setTextColor(0xFFFFFFFF);
        deleteButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        0xFF777777
                )
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        52,
                        1
                );

        buttonParams.setMargins(5, 0, 5, 0);

        editButton.setLayoutParams(buttonParams);
        deleteButton.setLayoutParams(buttonParams);

        buttonRow.addView(editButton);
        buttonRow.addView(deleteButton);

        card.addView(buttonRow);

        // Edit interaction
        editButton.setOnClickListener(v ->
                showEditRoomDialog(room)
        );

        // Delete interaction
        deleteButton.setOnClickListener(v ->
                confirmDelete(room)
        );

        roomContainer.addView(card);
    }

    // ============================================================
    // ADD ROOM
    // ============================================================

    private void showAddRoomDialog() {

        LinearLayout layout = createDialogLayout();

        EditText roomName = createInput("Room Name");
        EditText building = createInput("Building");
        EditText floor = createInput("Floor");
        EditText capacity = createInput("Capacity");
        EditText status = createInput("Status");

        layout.addView(roomName);
        layout.addView(building);
        layout.addView(floor);
        layout.addView(capacity);
        layout.addView(status);

        new AlertDialog.Builder(this)
                .setTitle("Add Room")
                .setView(layout)
                .setPositiveButton("ADD", null)
                .setNegativeButton("CANCEL", null)
                .create()
                .show();

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Add Room")
                        .setView(layout)
                        .setPositiveButton("ADD", null)
                        .setNegativeButton("CANCEL", null)
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if (roomName.getText().toString().trim().isEmpty()) {
                    roomName.setError("Enter room name");
                    return;
                }

                if (building.getText().toString().trim().isEmpty()) {
                    building.setError("Enter building");
                    return;
                }

                int floorNumber =
                        getNumber(floor, "Floor");

                int capacityNumber =
                        getNumber(capacity, "Capacity");

                if (floorNumber < 0 || capacityNumber < 0) {
                    return;
                }

                String statusText =
                        status.getText().toString().trim();

                if (statusText.isEmpty()) {
                    statusText = "Available";
                }

                RoomRequest request =
                        new RoomRequest(
                                0,
                                roomName.getText().toString().trim(),
                                building.getText().toString().trim(),
                                floorNumber,
                                capacityNumber,
                                statusText
                        );

                createRoom(request, dialog);
            });

        });

        dialog.show();
    }

    // ============================================================
    // CREATE ROOM API
    // ============================================================

    private void createRoom(
            RoomRequest request,
            AlertDialog dialog) {

        Client.api()
                .createRoom(request)
                .enqueue(new Callback<ApiResponse>() {

                    @Override
                    public void onResponse(
                            Call<ApiResponse> call,
                            Response<ApiResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success) {

                            Toast.makeText(
                                    RoomCrudActivity.this,
                                    "Room added successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();

                            loadRooms();

                        } else {

                            Toast.makeText(
                                    RoomCrudActivity.this,
                                    "Unable to add room.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<ApiResponse> call,
                            Throwable t) {

                        Toast.makeText(
                                RoomCrudActivity.this,
                                "Connection failed: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // ============================================================
    // EDIT ROOM
    // ============================================================

    private void showEditRoomDialog(Room room) {

        LinearLayout layout = createDialogLayout();

        EditText roomName =
                createInput(room.room_name);

        EditText building =
                createInput(room.building);

        EditText floor =
                createInput(String.valueOf(room.floor));

        EditText capacity =
                createInput(String.valueOf(room.capacity));

        EditText status =
                createInput(room.status);

        layout.addView(roomName);
        layout.addView(building);
        layout.addView(floor);
        layout.addView(capacity);
        layout.addView(status);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Edit Room")
                        .setView(layout)
                        .setPositiveButton("SAVE", null)
                        .setNegativeButton("CANCEL", null)
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                int floorNumber =
                        getNumber(floor, "Floor");

                int capacityNumber =
                        getNumber(capacity, "Capacity");

                if (floorNumber < 0 || capacityNumber < 0) {
                    return;
                }

                RoomRequest request =
                        new RoomRequest(
                                room.id,
                                roomName.getText().toString().trim(),
                                building.getText().toString().trim(),
                                floorNumber,
                                capacityNumber,
                                status.getText().toString().trim()
                        );

                updateRoom(request, dialog);
            });

        });

        dialog.show();
    }

    // ============================================================
    // UPDATE ROOM API
    // ============================================================

    private void updateRoom(
            RoomRequest request,
            AlertDialog dialog) {

        Client.api()
                .updateRoom(request)
                .enqueue(new Callback<ApiResponse>() {

                    @Override
                    public void onResponse(
                            Call<ApiResponse> call,
                            Response<ApiResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success) {

                            Toast.makeText(
                                    RoomCrudActivity.this,
                                    "Room updated successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();

                            loadRooms();

                        } else {

                            Toast.makeText(
                                    RoomCrudActivity.this,
                                    "Unable to update room.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<ApiResponse> call,
                            Throwable t) {

                        Toast.makeText(
                                RoomCrudActivity.this,
                                "Connection failed: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // ============================================================
    // DELETE CONFIRMATION
    // ============================================================

    private void confirmDelete(Room room) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Room")
                .setMessage(
                        "Delete " +
                                room.room_name +
                                "?"
                )
                .setPositiveButton(
                        "DELETE",
                        (dialog, which) ->
                                deleteRoom(room.id)
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    // ============================================================
    // DELETE ROOM API
    // ============================================================

    private void deleteRoom(int roomId) {

        Client.api()
                .deleteRoom(
                        new DeleteRequest(roomId)
                )
                .enqueue(new Callback<ApiResponse>() {

                    @Override
                    public void onResponse(
                            Call<ApiResponse> call,
                            Response<ApiResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success) {

                            Toast.makeText(
                                    RoomCrudActivity.this,
                                    "Room deleted successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadRooms();

                        } else {

                            Toast.makeText(
                                    RoomCrudActivity.this,
                                    "Unable to delete room.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<ApiResponse> call,
                            Throwable t) {

                        Toast.makeText(
                                RoomCrudActivity.this,
                                "Connection failed: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private LinearLayout createDialogLayout() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30,
                10,
                30,
                5
        );

        return layout;
    }

    private EditText createInput(String hint) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);

        input.setPadding(
                10,
                10,
                10,
                10
        );

        return input;
    }

    private int getNumber(
            EditText input,
            String name) {

        try {

            return Integer.parseInt(
                    input.getText()
                            .toString()
                            .trim()
            );

        } catch (Exception e) {

            input.setError(
                    "Enter a valid " + name
            );

            input.requestFocus();

            return -1;
        }
    }
}