package com.uniroom.app.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.uniroom.app.screens.MainActivity;

/** One of the four bottom-navigation tabs. */
public abstract class TabPage {

    protected final MainActivity activity;
    private View view;

    protected TabPage(MainActivity activity) {
        this.activity = activity;
    }

    public final View getView(ViewGroup parent) {
        if (view == null) {
            view = onCreateView(LayoutInflater.from(activity), parent);
            refresh();
        }
        return view;
    }

    protected abstract View onCreateView(LayoutInflater inflater, ViewGroup parent);

    /** Re-reads the data and updates the screen (called when coming back to the tab). */
    public void refresh() {}

    /** True when the top of this tab is coloured (blue/maroon), so the status bar should match. */
    public boolean coloredTop() { return false; }

    protected View find(int id) { return view.findViewById(id); }
}
