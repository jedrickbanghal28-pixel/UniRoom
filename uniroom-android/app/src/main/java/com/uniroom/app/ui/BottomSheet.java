package com.uniroom.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;

/**
 * A panel that slides up from the bottom of the screen, with a title and a close button.
 * Put your content into {@link #body()}.
 */
@SuppressWarnings("deprecation")
public class BottomSheet {

    private final Dialog dialog;
    private final LinearLayout body;
    private final TextView title;

    public BottomSheet(Context context, String titleText) {
        dialog = new Dialog(context, R.style.SheetOverlay);
        View root = LayoutInflater.from(context).inflate(R.layout.sheet_base, null, false);
        dialog.setContentView(root);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setGravity(Gravity.BOTTOM);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.4f);
            w.setNavigationBarColor(Color.TRANSPARENT);
            w.setStatusBarColor(Color.TRANSPARENT);
            // Draw behind the navigation bar too, with dark nav icons on the white sheet.
            if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);
            if (Build.VERSION.SDK_INT >= 30) {
                w.setDecorFitsSystemWindows(false);
                if (w.getInsetsController() != null) {
                    w.getInsetsController().setSystemBarsAppearance(
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
            } else {
                w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }

        View sheet = root.findViewById(R.id.sheet);
        body = root.findViewById(R.id.sheet_body);
        title = root.findViewById(R.id.sheet_title);
        title.setText(titleText);

        // Tap outside the panel or on X to close it.
        root.setOnClickListener(v -> dismiss());
        sheet.setOnClickListener(v -> { /* swallow taps inside the panel */ });
        root.findViewById(R.id.sheet_close).setOnClickListener(v -> dismiss());

        Ui.applyInsets(root, null, body);
    }

    public LinearLayout body() { return body; }

    public void setTitle(String text) { title.setText(text); }

    public void setOnDismiss(Runnable r) {
        dialog.setOnDismissListener(d -> r.run());
    }

    public void show() { dialog.show(); }

    public void dismiss() {
        if (dialog.isShowing()) dialog.dismiss();
    }

    public boolean isShowing() { return dialog.isShowing(); }
}
