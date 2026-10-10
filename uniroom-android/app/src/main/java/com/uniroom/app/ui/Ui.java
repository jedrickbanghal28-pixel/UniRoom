package com.uniroom.app.ui;

import android.widget.TextView;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.uniroom.app.R;
import com.uniroom.app.model.Account;

/** Small helpers used by every screen. */
public final class Ui {

    private Ui() {}

    public static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics()));
    }

    public static int color(Context c, int colorRes) {
        return c.getColor(colorRes);
    }

    /** Reads a colour from the current theme, e.g. R.attr.uniPrimary. */
    public static int attrColor(Context c, int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    public static int primary(Context c) { return attrColor(c, R.attr.uniPrimary); }

    public static int primarySoft(Context c) { return attrColor(c, R.attr.uniPrimarySoft); }

    /** Blue theme for students, maroon theme for faculty. */
    public static int themeFor(Account account) {
        return account != null && account.isFaculty() ? R.style.Theme_UniRoom_Faculty : R.style.Theme_UniRoom;
    }

    public static void tint(View v, int color) {
        v.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    // ---------------------------------------------------------------------------------------
    // Edge-to-edge: the app draws behind the status bar and navigation bar, and we add padding
    // ourselves so nothing is hidden. This also works with Android 15's forced edge-to-edge.
    // ---------------------------------------------------------------------------------------

    public static void edgeToEdge(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            w.setStatusBarContrastEnforced(false);
            w.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decor.setSystemUiVisibility(flags);
        }
    }

    /**
     * @param darkBackground true when the area behind the status bar is coloured (white icons),
     *                       false when it is light (dark icons).
     */
    public static void statusBarIcons(final Activity a, final boolean darkBackground) {
        applyBarIcons(a, darkBackground);
        // Apply again once the window is attached; some Android versions reset it from the theme.
        a.getWindow().getDecorView().post(() -> applyBarIcons(a, darkBackground));
    }

    @SuppressWarnings("deprecation")
    private static void applyBarIcons(Activity a, boolean darkBackground) {
        Window w = a.getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(darkBackground
                        ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : light, light);
            }
        } else {
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            if (darkBackground) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    /**
     * Makes room for the system bars and the keyboard:
     *  - status bar height  -> top padding of {@code top}
     *  - navigation bar     -> bottom padding of {@code bottom}
     *  - keyboard (extra)   -> bottom padding of {@code root}, so scrolling areas shrink
     *  - left/right bars    -> side padding of {@code root} (landscape, camera cut-outs)
     * {@code top} and {@code bottom} may be null.
     */
    @SuppressWarnings("deprecation")
    public static void applyInsets(final View root, final View top, final View bottom) {
        final int topBase = top != null ? top.getPaddingTop() : 0;
        final int bottomBase = bottom != null ? bottom.getPaddingBottom() : 0;
        final int rl = root.getPaddingLeft(), rr = root.getPaddingRight(), rb = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int t, b, l, r, keyboard;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                t = bars.top; b = bars.bottom; l = bars.left; r = bars.right;
                keyboard = Math.max(0, ime.bottom - bars.bottom);
            } else {
                t = insets.getSystemWindowInsetTop();
                b = insets.getStableInsetBottom();
                l = insets.getSystemWindowInsetLeft();
                r = insets.getSystemWindowInsetRight();
                keyboard = Math.max(0, insets.getSystemWindowInsetBottom() - b);
            }
            if (top != null) {
                top.setPadding(top.getPaddingLeft(), topBase + t, top.getPaddingRight(), top.getPaddingBottom());
            }
            if (bottom != null) {
                bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomBase + b);
            }
            root.setPadding(rl + l, root.getPaddingTop(), rr + r, rb + keyboard);
            return insets;
        });
        root.requestApplyInsets();
    }

    /**
     * Sets a field label. A label ending in " *" is a required field:
     * the * is shown in red.
     */
    public static void label(TextView t, String text) {
        if (text != null && text.endsWith("*")) {
            android.text.SpannableString s = new android.text.SpannableString(text);
            s.setSpan(new android.text.style.ForegroundColorSpan(t.getContext().getColor(R.color.danger)),
                    text.length() - 1, text.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            t.setText(s);
        } else {
            t.setText(text);
        }
    }
}
