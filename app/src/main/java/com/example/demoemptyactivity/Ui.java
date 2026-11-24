package com.example.demoemptyactivity;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputLayout;

final class Ui {
    private Ui() {}

    /** Clears a field's error as soon as the user edits it. */
    static void clearErrorOnEdit(TextInputLayout... layouts) {
        for (TextInputLayout l : layouts) {
            if (l.getEditText() == null) continue;
            l.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    if (l.getError() != null) l.setError(null);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }
}
