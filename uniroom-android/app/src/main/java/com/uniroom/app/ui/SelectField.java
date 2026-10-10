package com.uniroom.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uniroom.app.R;

import java.util.ArrayList;
import java.util.List;

/** Controls one included view_select_field layout. The options open in a bottom sheet. */
public class SelectField {

    public interface OnChange { void onChange(String value); }

    private final View box;
    private final TextView value;
    private final TextView error;
    private final String labelText;
    private List<String> options = new ArrayList<>();
    private String placeholder = "";
    private String selected = "";
    private OnChange onChange;

    public SelectField(View root, String label) {
        // The bottom sheet title is the label without the " *".
        labelText = label.endsWith(" *") ? label.substring(0, label.length() - 2) : label;
        Ui.label((TextView) root.findViewById(R.id.field_label), label);
        box = root.findViewById(R.id.field_box);
        value = root.findViewById(R.id.field_value);
        error = root.findViewById(R.id.field_error);
        box.setOnClickListener(v -> open());
        render();
    }

    public SelectField setOptions(List<String> opts, String placeholderText) {
        options = new ArrayList<>(opts);
        placeholder = placeholderText;
        if (!options.contains(selected)) selected = "";
        render();
        return this;
    }

    public SelectField onChange(OnChange l) { onChange = l; return this; }

    public String getValue() { return selected; }

    public void setValue(String v) {
        selected = v == null ? "" : v;
        render();
    }

    public void setError(String msg) {
        if (msg == null) {
            error.setVisibility(View.GONE);
            box.setBackgroundResource(R.drawable.bg_field);
        } else {
            error.setText(msg);
            error.setVisibility(View.VISIBLE);
            box.setBackgroundResource(R.drawable.bg_field_error);
        }
    }

    private void render() {
        if (selected.isEmpty()) {
            value.setText(placeholder);
            value.setTextColor(box.getContext().getColor(R.color.muted_hint));
        } else {
            value.setText(selected);
            value.setTextColor(box.getContext().getColor(R.color.ink));
        }
    }

    private void open() {
        final BottomSheet sheet = new BottomSheet(box.getContext(), labelText);
        LinearLayout body = sheet.body();
        if (options.isEmpty()) {
            TextView t = new TextView(box.getContext());
            t.setTextAppearance(R.style.T_Small);
            t.setText(placeholder);
            t.setPadding(0, Ui.dp(box.getContext(), 8), 0, Ui.dp(box.getContext(), 8));
            body.addView(t);
        }
        LayoutInflater inf = LayoutInflater.from(box.getContext());
        for (final String o : options) {
            View row = inf.inflate(R.layout.item_option, body, false);
            ((TextView) row.findViewById(R.id.option_text)).setText(o);
            row.findViewById(R.id.option_check).setVisibility(o.equals(selected) ? View.VISIBLE : View.INVISIBLE);
            row.setOnClickListener(v -> {
                selected = o;
                render();
                setError(null);
                sheet.dismiss();
                if (onChange != null) onChange.onChange(o);
            });
            body.addView(row);
        }
        sheet.show();
    }
}
