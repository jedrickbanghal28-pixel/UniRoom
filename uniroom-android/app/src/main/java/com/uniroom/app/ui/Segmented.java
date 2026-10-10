package com.uniroom.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.model.Program;

/**
 * A row of two (or more) options where exactly one is selected,
 * like the "BSIT Faculty | BSMEDTECH Faculty" switcher.
 */
public final class Segmented {

    public interface OnSelect { void onSelect(int index); }

    private Segmented() {}

    /**
     * @param container empty horizontal LinearLayout
     * @param onPrimary true when it sits on a blue/maroon header
     */
    public static void attach(final LinearLayout container, final String[] labels, final int selected,
                              final boolean onPrimary, final int heightDp, final OnSelect listener) {
        Context c = container.getContext();
        container.removeAllViews();
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundResource(R.drawable.bg_round_12);
        Ui.tint(container, Ui.color(c, onPrimary ? R.color.white_15 : R.color.line_soft));
        int pad = Ui.dp(c, 4);
        container.setPadding(pad, pad, pad, pad);

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean active = i == selected;
            TextView t = new TextView(c);
            t.setText(labels[i]);
            t.setTextAppearance(R.style.T_MetaSemi);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setBackgroundResource(R.drawable.bg_round_8);
            if (active) {
                Ui.tint(t, onPrimary ? Color.WHITE : Ui.color(c, R.color.surface));
                t.setTextColor(onPrimary ? Ui.primary(c) : Ui.color(c, R.color.ink));
                if (!onPrimary) t.setElevation(Ui.dp(c, 1));
            } else {
                Ui.tint(t, Color.TRANSPARENT);
                t.setTextColor(Ui.color(c, onPrimary ? R.color.white_80 : R.color.muted));
            }
            t.setOnClickListener(v -> {
                if (index != selected) {
                    attach(container, labels, index, onPrimary, heightDp, listener);
                    listener.onSelect(index);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(c, heightDp), 1f);
            container.addView(t, lp);
        }
    }

    /** The faculty "BSIT Faculty / BSMEDTECH Faculty" switcher. */
    public static void programSwitcher(LinearLayout container, boolean onPrimary, final Runnable onChanged) {
        final Program[] programs = Program.values();
        String[] labels = new String[programs.length];
        int selected = 0;
        for (int i = 0; i < programs.length; i++) {
            labels[i] = programs[i].portalLabel;
            if (programs[i] == AppState.get().getFacultyProgram()) selected = i;
        }
        attach(container, labels, selected, onPrimary, 36, index -> {
            AppState.get().setFacultyProgram(programs[index]);
            onChanged.run();
        });
    }
}
