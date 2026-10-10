package com.uniroom.app.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.DemoData;
import com.uniroom.app.model.Room;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.BottomSheet;
import com.uniroom.app.ui.Pills;
import com.uniroom.app.ui.RoomDetailSheet;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Segmented;
import com.uniroom.app.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/** Faculty: the 3rd floor rooms they teach in, per program. Add, remove, or send an update. */
public class MyRoomsTab extends TabPage {

    private BottomSheet detailSheet;

    public MyRoomsTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_my_rooms, parent, false);
        v.findViewById(R.id.btn_add_room).setOnClickListener(x -> openAddSheet());
        Rows.emptyState(v.findViewById(R.id.my_rooms_empty), R.drawable.ic_door_open,
                "No rooms yet", "Add the 3rd floor rooms you teach in for this program.");
        return v;
    }

    @Override
    public void refresh() {
        AppState state = AppState.get();
        List<Room> mine = new ArrayList<>();
        for (Room r : DemoData.roomsOnFloor(3)) if (state.assignedRooms().contains(r.code)) mine.add(r);

        ((TextView) find(R.id.rooms_subtitle)).setText(
                mine.size() + " assigned · 3rd floor · " + state.getFacultyProgram().portalLabel);
        Segmented.programSwitcher((LinearLayout) find(R.id.program_switcher), false, this::refresh);

        LinearLayout list = (LinearLayout) find(R.id.my_rooms_list);
        list.setVisibility(mine.isEmpty() ? View.GONE : View.VISIBLE);
        find(R.id.my_rooms_empty).setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
        Rows.rooms(list, mine, this::openRoom);
    }

    private void openRoom(final Room room) {
        View footer = LayoutInflater.from(activity).inflate(R.layout.sheet_room_footer, null, false);
        footer.findViewById(R.id.btn_send_update).setOnClickListener(v -> {
            if (detailSheet != null) detailSheet.dismiss();
            activity.openNotify();
        });
        footer.findViewById(R.id.btn_remove_room).setOnClickListener(v -> {
            AppState.get().removeRoom(room.code);
            if (detailSheet != null) detailSheet.dismiss();
            refresh();
        });
        detailSheet = RoomDetailSheet.show(activity, room, AppState.get().mySessions(), footer);
    }

    private void openAddSheet() {
        final BottomSheet sheet = new BottomSheet(activity, "Add a 3rd floor room");
        LinearLayout body = sheet.body();
        List<Room> others = new ArrayList<>();
        for (Room r : DemoData.roomsOnFloor(3)) if (!AppState.get().assignedRooms().contains(r.code)) others.add(r);

        if (others.isEmpty()) {
            TextView t = new TextView(activity);
            t.setTextAppearance(R.style.T_Small);
            t.setText("All 3rd floor rooms are already assigned to you.");
            t.setPadding(0, Ui.dp(activity, 16), 0, Ui.dp(activity, 16));
            body.addView(t);
        } else {
            LinearLayout list = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.view_divided_list, body, false);
            for (final Room r : others) {
                View row = LayoutInflater.from(activity).inflate(R.layout.item_add_room, list, false);
                Rows.text(row, R.id.add_code, r.code);
                Rows.text(row, R.id.add_meta, r.type + " · " + r.capacity + " seats");
                ((FrameLayout) row.findViewById(R.id.add_pill)).addView(Pills.status(activity, r.status));
                View add = row.findViewById(R.id.add_button);
                add.setContentDescription("Add " + r.code);
                add.setOnClickListener(v -> {
                    AppState.get().addRoom(r.code);
                    sheet.dismiss();
                    refresh();
                });
                list.addView(row);
            }
            body.addView(list);
        }
        sheet.show();
    }
}
