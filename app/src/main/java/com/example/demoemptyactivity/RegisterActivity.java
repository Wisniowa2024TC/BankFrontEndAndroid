package com.example.demoemptyactivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.demoemptyactivity.network.AccountResponse;
import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.RegisterRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private MaterialButton btnRegister;
    private View progress;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.rejestr);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            boolean ime = insets.isVisible(WindowInsetsCompat.Type.ime());
            View hero = findViewById(R.id.login_astolfo);
            if (hero != null) hero.setVisibility(ime ? View.GONE : View.VISIBLE);
            if (ime) {
                // Keep the focused field visible above the keyboard
                v.postDelayed(() -> {
                    View f = getCurrentFocus();
                    if (f != null) {
                        android.graphics.Rect r = new android.graphics.Rect(0, 0, f.getWidth(), f.getHeight() + dp(96));
                        f.requestRectangleOnScreen(r, false);
                    }
                }, 150);
            }
            return insets;
        });

        btnRegister = findViewById(R.id.btn_register);
        progress = findViewById(R.id.register_progress);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_go_login).setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> register());
        Ui.clearErrorOnEdit(findViewById(R.id.first_name_layout), findViewById(R.id.last_name_layout),
                findViewById(R.id.email_layout), findViewById(R.id.password_layout), findViewById(R.id.password2_layout));
    }

    private String text(int id) {
        return ((EditText) findViewById(id)).getText().toString();
    }

    private void register() {
        if (loading) return;
        TextInputLayout lFirst = findViewById(R.id.first_name_layout);
        TextInputLayout lLast = findViewById(R.id.last_name_layout);
        TextInputLayout lEmail = findViewById(R.id.email_layout);
        TextInputLayout lPass = findViewById(R.id.password_layout);
        TextInputLayout lPass2 = findViewById(R.id.password2_layout);
        for (TextInputLayout l : new TextInputLayout[]{lFirst, lLast, lEmail, lPass, lPass2}) l.setError(null);

        String first = text(R.id.first_name).trim();
        String last = text(R.id.last_name).trim();
        String email = text(R.id.email).trim();
        String pass = text(R.id.password);
        String pass2 = text(R.id.password2);

        boolean ok = true;
        if (first.isEmpty()) { lFirst.setError("Wymagane"); ok = false; }
        if (last.isEmpty()) { lLast.setError("Wymagane"); ok = false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { lEmail.setError("Niepoprawny e-mail"); ok = false; }
        if (pass.length() < 6) { lPass.setError("Min. 6 znaków"); ok = false; }
        else if (!pass.equals(pass2)) { lPass2.setError("Hasła się różnią"); ok = false; }
        if (!ok) return;

        setLoading(true);
        ApiClient.getApiService().register(new RegisterRequest(first, last, email, pass)).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (isFinishing()) return;
                setLoading(false);
                if (response.isSuccessful()) {
                    Intent data = new Intent();
                    data.putExtra("EMAIL", email);
                    setResult(RESULT_OK, data);
                    finish();
                } else if (response.code() == 409) {
                    lEmail.setError("Ten e-mail jest już zajęty");
                } else {
                    snack(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable t) {
                if (isFinishing()) return;
                setLoading(false);
                snack("Brak połączenia z serwerem");
            }
        });
    }

    private void setLoading(boolean l) {
        loading = l;
        btnRegister.setEnabled(!l);
        progress.setVisibility(l ? View.VISIBLE : View.INVISIBLE);
    }

    private void snack(String msg) {
        Snackbar.make(findViewById(R.id.register_root), msg, Snackbar.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
