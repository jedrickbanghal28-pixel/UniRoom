package com.uniroom.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.Clock;
import com.uniroom.app.data.DemoData;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.RoomLocation;
import com.uniroom.app.screens.RoomLocationActivity;

import java.util.ArrayList;
import java.util.List;

/** Bottom sheet with a room's details, its photo/directions link and today's classes. */
public final class RoomDetailSheet {

    private RoomDetailSheet() {}

    /**
     * @param sessions classes to look through for "Today in this room"
     * @param footer   optional extra buttons (faculty), may be null
     */
    public static BottomSheet show(final Activity activity, final Room room, List<ClassSession> sessions, View footer) {
        BottomSheet sheet = new BottomSheet(activity, room.code);
        View content = LayoutInflater.from(activity).inflate(R.layout.sheet_room_detail, sheet.body(), false);
        sheet.body().addView(content);

        ((FrameLayout) content.findViewById(R.id.rd_pill)).addView(Pills.status(activity, room.status));
        Rows.text(content, R.id.rd_type, room.type);
        Rows.text(content, R.id.rd_department, room.department);
        Rows.text(content, R.id.rd_building, room.building);
        Rows.text(content, R.id.rd_floor, String.valueOf(room.floor));
        Rows.text(content, R.id.rd_capacity, String.valueOf(room.capacity));

        // "View room location" row (only for rooms that have a photo)
        View locRow = content.findViewById(R.id.rd_location);
        final RoomLocation loc = DemoData.locationFor(room.code);
        if (loc != null) {
            ImageView thumb = content.findViewById(R.id.rd_location_image);
            thumb.setImageResource(loc.imageRes);
            thumb.setClipToOutline(true); // rounded corners
            Rows.text(content, R.id.rd_location_landmark, loc.landmark);
            locRow.setOnClickListener(v -> {
                Intent i = new Intent(activity, RoomLocationActivity.class);
                i.putExtra(RoomLocationActivity.EXTRA_ROOM, room.code);
                activity.startActivity(i);
                activity.overridePendingTransition(R.anim.zoom_in, R.anim.hold);
            });
        } else {
            locRow.setVisibility(View.GONE);
        }

        // Today in this room
        List<ClassSession> today = new ArrayList<>();
        for (ClassSession s : Clock.sessionsForDay(sessions, Clock.todayIndex())) {
            if (s.room.equals(room.code)) today.add(s);
        }
        LinearLayout list = content.findViewById(R.id.rd_today_list);
        TextView none = content.findViewById(R.id.rd_today_none);
        if (today.isEmpty()) {
            none.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else {
            LayoutInflater inf = LayoutInflater.from(activity);
            for (ClassSession c : today) {
                View row = inf.inflate(R.layout.item_sheet_class, list, false);
                Rows.text(row, R.id.sc_title, c.code + " · " + c.title);
                Rows.text(row, R.id.sc_time, Clock.formatRange(c.start, c.end));
                list.addView(row);
            }
        }

        if (footer != null) {
            LinearLayout f = content.findViewById(R.id.rd_footer);
            f.setVisibility(View.VISIBLE);
            f.addView(footer, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        sheet.show();
        return sheet;
    }
}
