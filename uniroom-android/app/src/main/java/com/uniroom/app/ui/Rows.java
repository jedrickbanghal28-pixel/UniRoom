package com.uniroom.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.Clock;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Role;
import com.uniroom.app.model.Room;

import java.util.List;

/** Fills list containers with rows (instead of a RecyclerView – the lists are short). */
public final class Rows {

    public interface OnRoom { void onRoom(Room room); }

    private Rows() {}

    /** time | title + "code · teacher/section" | room */
    public static void dayClasses(LinearLayout list, List<ClassSession> sessions, Role role) {
        list.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(list.getContext());
        for (ClassSession s : sessions) {
            View row = inf.inflate(R.layout.item_day_class, list, false);
            text(row, R.id.row_start, Clock.formatTime(s.start));
            text(row, R.id.row_end, Clock.formatTime(s.end));
            text(row, R.id.row_title, s.title);
            text(row, R.id.row_meta, s.code + " · " + (role == Role.STUDENT ? s.instructor : s.section));
            text(row, R.id.row_room, s.room);
            list.addView(row);
        }
    }

    /** Room rows with a status pill; tapping one calls listener. */
    public static void rooms(LinearLayout list, List<Room> rooms, OnRoom listener) {
        list.removeAllViews();
        list.setClipToOutline(true); // keep the touch ripple inside the rounded corners
        LayoutInflater inf = LayoutInflater.from(list.getContext());
        for (final Room r : rooms) {
            View row = inf.inflate(R.layout.item_room, list, false);
            text(row, R.id.room_code, r.code);
            text(row, R.id.room_meta, r.building + " · Floor " + r.floor + " · " + r.capacity + " seats");
            ((FrameLayout) row.findViewById(R.id.room_pill)).addView(Pills.status(list.getContext(), r.status));
            row.setOnClickListener(v -> listener.onRoom(r));
            list.addView(row);
        }
    }

    public static void text(View parent, int id, CharSequence value) {
        ((TextView) parent.findViewById(id)).setText(value);
    }

    public static void emptyState(View view, int iconRes, String title, String body) {
        ((ImageView) view.findViewById(R.id.empty_icon)).setImageResource(iconRes);
        text(view, R.id.empty_title, title);
        text(view, R.id.empty_body, body);
    }

    /** Builds a standalone empty state view (for lists filled in code). */
    public static View newEmptyState(ViewGroup parent, int iconRes, String title, String body) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_empty_state, parent, false);
        emptyState(v, iconRes, title, body);
        return v;
    }

    /** A plain grey sentence inside a white card, e.g. "No rooms assigned for this program." */
    public static void note(LinearLayout card, String message) {
        card.removeAllViews();
        TextView t = new TextView(card.getContext());
        t.setTextAppearance(R.style.T_Small);
        t.setText(message);
        int p = Ui.dp(card.getContext(), 16);
        t.setPadding(p, p, p, p);
        card.addView(t);
    }
}
