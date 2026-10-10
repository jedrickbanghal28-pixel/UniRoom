package com.uniroom.app.tabs;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.DemoData;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.RoomStatus;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.Header;
import com.uniroom.app.ui.RoomDetailSheet;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Students: search and filter rooms, then tap one to see details. */
public class FindRoomTab extends TabPage {

    private static final String[] FILTERS = {"All", "Available", "Labs", "Lecture rooms"};

    private String query = "";
    private int filter = 0;
    private boolean showSecondFloor = false;

    public FindRoomTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_find_room, parent, false);
        Header.bind(v.findViewById(R.id.header), "Find a room", "Tech Hub · live availability", null);

        EditText search = v.findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                refresh();
            }
        });
        v.findViewById(R.id.second_toggle).setOnClickListener(x -> {
            showSecondFloor = !showSecondFloor;
            refresh();
        });
        return v;
    }

    private boolean matches(Room r) {
        String q = query.toLowerCase().replace(" ", "");
        boolean matchesQuery = r.code.toLowerCase().replace(" ", "").contains(q);
        boolean matchesFilter;
        switch (filter) {
            case 1: matchesFilter = r.status == RoomStatus.AVAILABLE; break;
            case 2: matchesFilter = r.isLab(); break;
            case 3: matchesFilter = Room.LECTURE_ROOM.equals(r.type); break;
            default: matchesFilter = true;
        }
        return matchesQuery && matchesFilter;
    }

    @Override
    public void refresh() {
        buildFilters();

        List<Room> third = new ArrayList<>(), second = new ArrayList<>();
        for (Room r : DemoData.ROOMS) {
            if (!matches(r)) continue;
            if (r.floor == 3) third.add(r); else if (r.floor == 2) second.add(r);
        }

        // 3rd floor
        int available = 0;
        for (Room r : third) if (r.status == RoomStatus.AVAILABLE) available++;
        TextView count = (TextView) find(R.id.available_count);
        count.setVisibility(third.isEmpty() ? View.GONE : View.VISIBLE);
        count.setText(available + " available");

        LinearLayout thirdList = (LinearLayout) find(R.id.third_list);
        if (third.isEmpty()) {
            thirdList.removeAllViews();
            thirdList.addView(Rows.newEmptyState(thirdList, R.drawable.ic_search_x,
                    "No rooms match", "Try a different room code or filter."));
        } else {
            Rows.rooms(thirdList, third, this::openRoom);
        }

        // 2nd floor
        boolean open = showSecondFloor || !query.trim().isEmpty();
        find(R.id.second_chevron).setRotation(open ? 180 : 0);
        LinearLayout container = (LinearLayout) find(R.id.second_container);
        container.setVisibility(open ? View.VISIBLE : View.GONE);
        container.removeAllViews();
        if (!open) return;

        TextView note = new TextView(activity);
        note.setTextAppearance(R.style.T_Meta);
        note.setText("These rooms aren’t used for BSIT or BS MedTech classes.");
        note.setPadding(Ui.dp(activity, 4), 0, 0, 0);
        container.addView(note);

        Set<String> departments = new LinkedHashSet<>();
        for (Room r : second) departments.add(r.department);
        if (departments.isEmpty()) {
            TextView none = new TextView(activity);
            none.setTextAppearance(R.style.T_Small);
            none.setText("No 2nd floor rooms match.");
            none.setPadding(Ui.dp(activity, 4), Ui.dp(activity, 16), 0, 0);
            container.addView(none);
        }
        for (String d : departments) {
            TextView title = new TextView(activity);
            title.setTextAppearance(R.style.T_CaptionSemi);
            title.setTextColor(activity.getColor(R.color.muted));
            title.setText(DemoData.departmentName(d));
            title.setPadding(Ui.dp(activity, 4), Ui.dp(activity, 16), 0, Ui.dp(activity, 8));
            container.addView(title);

            LinearLayout card = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.view_card_list, container, false);
            List<Room> rooms = new ArrayList<>();
            for (Room r : second) if (r.department.equals(d)) rooms.add(r);
            Rows.rooms(card, rooms, this::openRoom);
            container.addView(card);
        }
    }

    private void buildFilters() {
        LinearLayout row = (LinearLayout) find(R.id.filter_row);
        row.removeAllViews();
        for (int i = 0; i < FILTERS.length; i++) {
            final int index = i;
            boolean active = i == filter;
            TextView chip = new TextView(activity);
            chip.setText(FILTERS[i]);
            chip.setTextAppearance(R.style.T_SmallInk);
            chip.setTypeface(activity.getResources().getFont(R.font.inter_medium));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(activity, 16), 0, Ui.dp(activity, 16), 0);
            chip.setBackgroundResource(R.drawable.bg_pill);
            chip.setForeground(activity.getDrawable(R.drawable.ripple_round_16));
            Ui.tint(chip, active ? Ui.primary(activity) : activity.getColor(R.color.surface));
            chip.setTextColor(active ? 0xFFFFFFFF : activity.getColor(R.color.ink));
            chip.setSelected(active);
            chip.setOnClickListener(v -> { filter = index; refresh(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(activity, 36));
            lp.setMarginEnd(Ui.dp(activity, 8));
            row.addView(chip, lp);
        }
    }

    private void openRoom(Room room) {
        RoomDetailSheet.show(activity, room, DemoData.SESSIONS, null);
    }
}
