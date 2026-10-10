package com.uniroom.app.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.model.RoomStatus;

/** Small rounded labels, e.g. the green "Available" status pill. */
public final class Pills {

    private Pills() {}

    public static TextView make(Context c, String text, int bgColor, int textColor) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextAppearance(R.style.T_CaptionSemi);
        t.setTextColor(textColor);
        t.setBackgroundResource(R.drawable.bg_pill);
        Ui.tint(t, bgColor);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.dp(c, 10), Ui.dp(c, 4), Ui.dp(c, 10), Ui.dp(c, 4));
        t.setSingleLine(true);
        return t;
    }

    public static TextView status(Context c, RoomStatus status) {
        int bg, fg;
        switch (status) {
            case AVAILABLE: bg = R.color.success_soft; fg = R.color.success; break;
            case RESERVED: bg = R.color.warning_soft; fg = R.color.warning; break;
            default: bg = R.color.danger_soft; fg = R.color.danger; break;
        }
        int fgColor = Ui.color(c, fg);
        TextView t = make(c, status.label, Ui.color(c, bg), fgColor);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(fgColor);
        int s = Ui.dp(c, 6);
        dot.setSize(s, s);
        dot.setBounds(0, 0, s, s);
        t.setCompoundDrawables(dot, null, null, null);
        t.setCompoundDrawablePadding(Ui.dp(c, 6));
        return t;
    }
}
