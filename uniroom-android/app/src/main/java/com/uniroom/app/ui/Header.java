package com.uniroom.app.ui;

import android.view.View;
import android.widget.TextView;

import com.uniroom.app.R;

/** Fills in an included view_back_header. */
public final class Header {

    private Header() {}

    public static void bind(View header, String title, String subtitle, Runnable onBack) {
        ((TextView) header.findViewById(R.id.header_title)).setText(title);
        TextView sub = header.findViewById(R.id.header_subtitle);
        if (subtitle != null) {
            sub.setText(subtitle);
            sub.setVisibility(View.VISIBLE);
        } else {
            sub.setVisibility(View.GONE);
        }
        View back = header.findViewById(R.id.header_back);
        if (onBack != null) {
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> onBack.run());
        } else {
            back.setVisibility(View.GONE);
        }
    }

    /** Text button on the right of the title (e.g. "Mark all read"). Pass null to hide it. */
    public static void action(View header, String text, Runnable onClick) {
        TextView a = header.findViewById(R.id.header_action);
        if (text == null) {
            a.setVisibility(View.GONE);
            return;
        }
        a.setText(text);
        a.setVisibility(View.VISIBLE);
        a.setOnClickListener(v -> onClick.run());
    }
}
