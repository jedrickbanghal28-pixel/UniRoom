package com.uniroom.app.ui;

import android.view.View;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.Clock;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Role;

/** Fills in the included view_next_class card. */
public final class NextClassCard {

    private NextClassCard() {}

    public static void bind(View card, ClassSession session, Role role, boolean hasClassesToday) {
        View next = card.findViewById(R.id.next_group);
        View done = card.findViewById(R.id.done_group);
        if (session == null) {
            next.setVisibility(View.GONE);
            done.setVisibility(View.VISIBLE);
            Rows.text(card, R.id.done_label, hasClassesToday ? "You’re done for today" : "Free day");
            Rows.text(card, R.id.done_title, hasClassesToday ? "No more classes scheduled" : "No classes scheduled today");
            return;
        }
        next.setVisibility(View.VISIBLE);
        done.setVisibility(View.GONE);
        Rows.text(card, R.id.next_relative, Clock.formatRelative(Clock.toMinutes(session.start) - Clock.nowMinutes()));
        Rows.text(card, R.id.next_title, session.title);
        Rows.text(card, R.id.next_code, session.code);
        Rows.text(card, R.id.next_room, session.room);
        Rows.text(card, R.id.next_time, Clock.formatRange(session.start, session.end));
        Rows.text(card, R.id.next_building, session.building);
        TextView person = card.findViewById(R.id.next_person);
        person.setText(role == Role.STUDENT ? session.instructor : session.section);
        person.setCompoundDrawablesRelativeWithIntrinsicBounds(
                role == Role.STUDENT ? R.drawable.ic_user_sm : R.drawable.ic_users_sm, 0, 0, 0);
    }
}
