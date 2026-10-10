package com.uniroom.app.ui;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.uniroom.app.R;

/** Controls one included view_text_field layout: label, icon, input, password eye and error. */
public class FormField {

    private final TextView label;
    private final View box;
    private final ImageView icon;
    public final EditText input;
    private final ImageView toggle;
    private final TextView error;
    private boolean passwordVisible = false;
    private Runnable onChange;

    public FormField(View root, String labelText) {
        label = root.findViewById(R.id.field_label);
        box = root.findViewById(R.id.field_box);
        icon = root.findViewById(R.id.field_icon);
        input = root.findViewById(R.id.field_input);
        toggle = root.findViewById(R.id.field_toggle);
        error = root.findViewById(R.id.field_error);
        Ui.label(label, labelText);
        // Several fields on one screen share the same inner ids (they come from one <include>),
        // so Android must not save/restore their text by id – it would mix the fields up.
        input.setSaveEnabled(false);
        // Tapping anywhere on the box focuses the input.
        box.setOnClickListener(v -> input.requestFocus());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (onChange != null) onChange.run();
            }
        });
    }

    public FormField hint(String hint) { input.setHint(hint); return this; }

    public FormField icon(int drawableRes) {
        icon.setImageResource(drawableRes);
        icon.setVisibility(View.VISIBLE);
        return this;
    }

    public FormField inputType(int type) {
        Typeface tf = input.getTypeface();
        input.setInputType(type);
        input.setTypeface(tf);
        return this;
    }

    /** Hides the text and shows an eye button to reveal it. */
    public FormField password() {
        inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        toggle.setVisibility(View.VISIBLE);
        toggle.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            int sel = input.getSelectionEnd();
            inputType(InputType.TYPE_CLASS_TEXT | (passwordVisible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            input.setSelection(Math.max(0, sel));
            toggle.setImageResource(passwordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            toggle.setContentDescription(passwordVisible ? "Hide password" : "Show password");
        });
        return this;
    }

    public FormField onChange(Runnable r) { onChange = r; return this; }

    public String text() { return input.getText().toString(); }

    public void setText(String s) { input.setText(s); }

    /** Shows a red message under the field (or hides it when msg is null). */
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

    public boolean hasError() { return error.getVisibility() == View.VISIBLE; }
}
