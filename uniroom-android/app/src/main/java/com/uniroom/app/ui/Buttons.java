package com.uniroom.app.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Shows a small spinner inside a button while something is "loading". */
public final class Buttons {

    private Buttons() {}

    /**
     * @param button the button LinearLayout
     * @param label  its TextView
     * @param spinner a ProgressBar placed inside the button (gone by default)
     */
    public static void setLoading(ViewGroup button, TextView label, ProgressBar spinner,
                                  boolean loading, String text) {
        label.setText(text);
        spinner.setVisibility(loading ? View.VISIBLE : View.GONE);
        button.setEnabled(!loading);
        button.setAlpha(loading ? 0.6f : 1f);
    }
}
