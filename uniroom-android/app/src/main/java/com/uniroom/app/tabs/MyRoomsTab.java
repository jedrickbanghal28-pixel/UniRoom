package com.uniroom.app.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.model.Room;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.BottomSheet;
import com.uniroom.app.ui.RoomDetailSheet;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Segmented;

import java.util.ArrayList;
import java.util.List;

/**
 * Faculty: the rooms they teach in for the selected program. The list comes
 * from the class schedule in the database (set up by the admin with conflict
 * checking), so it is the same on every phone and survives closing the app.
 */
public class MyRoomsTab extends TabPage {

    private BottomSheet detailSheet;

    public MyRoomsTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_my_rooms, parent, false);
        Rows.emptyState(v.findViewById(R.id.my_rooms_empty), R.drawable.ic_door_open,
                "No rooms yet", "Rooms appear here once the admin schedules your classes for this program.");
        return v;
    }

    @Override
    public void refresh() {
        AppState state = AppState.get();
        List<Room> mine = new ArrayList<>();
        for (String code : state.assignedRooms()) {
            Room r = state.findRoom(code);
            if (r != null) mine.add(r);
        }

        ((TextView) find(R.id.rooms_subtitle)).setText(
                mine.size() + (mine.size() == 1 ? " room" : " rooms") + " in your schedule · "
                        + state.getFacultyProgram().portalLabel);
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
        detailSheet = RoomDetailSheet.show(activity, room, AppState.get().todayInRooms(), footer);
    }
}
