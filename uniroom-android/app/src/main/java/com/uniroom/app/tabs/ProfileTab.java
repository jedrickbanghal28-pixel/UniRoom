package com.uniroom.app.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.model.Account;
import com.uniroom.app.model.Student;
import com.uniroom.app.model.Teacher;
import com.uniroom.app.screens.MainActivity;
import com.uniroom.app.ui.Rows;

/** Name, role and account details, plus Log out. */
public class ProfileTab extends TabPage {

    public ProfileTab(MainActivity activity) { super(activity); }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.tab_profile, parent, false);
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> activity.logout());
        return v;
    }

    @Override
    public void refresh() {
        Account a = AppState.get().getAccount();
        String[][] details;
        String name, initials, role;
        if (a.isStudent()) {
            Student s = a.student;
            name = s.fullName;
            initials = s.initials;
            role = "Student · " + s.program.label;
            details = new String[][]{
                    {"School ID", s.schoolId},
                    {"Username", s.username},
                    {"Email", s.email},
                    {"Contact number", s.contact},
                    {"Program", s.program.label},
                    {"Section", s.section}};
        } else {
            Teacher t = a.teacher;
            name = t.fullName;
            initials = t.initials;
            role = "Faculty · BSIT & BSMEDTECH";
            details = new String[][]{
                    {"Username", t.username},
                    {"Email", t.email},
                    {"Subject", t.subject},
                    {"Handles", "BS-IT, BS-MedTech"}};
        }
        ((TextView) find(R.id.profile_initials)).setText(initials);
        ((TextView) find(R.id.profile_name)).setText(name);
        ((TextView) find(R.id.profile_role)).setText(role);

        LinearLayout list = (LinearLayout) find(R.id.profile_details);
        list.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(activity);
        for (String[] d : details) {
            View row = inf.inflate(R.layout.item_detail_row, list, false);
            Rows.text(row, R.id.detail_label, d[0]);
            Rows.text(row, R.id.detail_value, d[1]);
            list.addView(row);
        }
    }
}
