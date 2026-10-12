package com.uniroom.app.screens;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;
import com.uniroom.app.data.AppState;
import com.uniroom.app.data.RoomCatalog;
import com.uniroom.app.model.Account;
import com.uniroom.app.model.RoomLocation;
import com.uniroom.app.ui.Rows;
import com.uniroom.app.ui.Ui;

/** "Where is TH 309?" – photo of the area plus step-by-step directions. */
public class RoomLocationActivity extends Activity {

    public static final String EXTRA_ROOM = "room";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Account account = AppState.get().getAccount();
        setTheme(account != null && account.isFaculty()
                ? R.style.Theme_UniRoom_Faculty_Viewer : R.style.Theme_UniRoom_Viewer);
        super.onCreate(savedInstanceState);

        String roomCode = getIntent().getStringExtra(EXTRA_ROOM);
        RoomLocation loc = roomCode != null ? RoomCatalog.locationFor(roomCode) : null;
        if (loc == null) { finish(); return; }

        setContentView(R.layout.activity_room_location);
        Ui.edgeToEdge(this);
        Ui.statusBarIcons(this, true);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.top_bar), findViewById(R.id.loc_sheet));

        findViewById(R.id.loc_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.loc_title)).setText("Where is " + roomCode + "?");
        ((TextView) findViewById(R.id.loc_subtitle)).setText("Tech Hub · " + loc.range);

        ImageView image = findViewById(R.id.loc_image);
        image.setImageResource(loc.imageRes);
        image.setContentDescription("Photo of the Tech Hub 3rd floor area where " + roomCode
                + " is located: " + loc.landmark);
        ((TextView) findViewById(R.id.loc_label)).setText(loc.label);
        ((TextView) findViewById(R.id.loc_landmark)).setText(loc.landmark);

        LinearLayout steps = findViewById(R.id.loc_steps);
        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < loc.directions.length; i++) {
            View row = inf.inflate(R.layout.item_direction, steps, false);
            Rows.text(row, R.id.step_number, String.valueOf(i + 1));
            Rows.text(row, R.id.step_text, loc.directions[i]);
            steps.addView(row);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.hold, R.anim.zoom_out);
    }
}
