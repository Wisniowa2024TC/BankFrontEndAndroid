package com.example.demoemptyactivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.LoginRequest;
import com.example.demoemptyactivity.network.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_REGISTER = 1;

    private EditText etEmail;
    private EditText etPassword;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private MaterialButton btnLogin;
    private View progress;
    private MaterialButton btnServer;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.init(this);

        // Already logged in (e.g. relaunch from recents) -> go straight to dashboard
        if (Session.isLoggedIn()) {
            openDashboard();
            return;
        }

        EdgeToEdge.enable(this,
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
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

        etEmail = findViewById(R.id.user);
        etPassword = findViewById(R.id.haslo);
        emailLayout = findViewById(R.id.user_layout);
        passwordLayout = findViewById(R.id.haslo_layout);
        btnLogin = findViewById(R.id.btn_login);
        progress = findViewById(R.id.login_progress);
        btnServer = findViewById(R.id.btn_server);

        String lastEmail = prefs().getString("last_email", "");
        etEmail.setText(lastEmail);
        Ui.clearErrorOnEdit(emailLayout, passwordLayout);

        btnLogin.setOnClickListener(v -> login());
        etPassword.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { login(); return true; }
            return false;
        });
        findViewById(R.id.btn_go_register).setOnClickListener(v ->
                startActivityForResult(new Intent(this, RegisterActivity.class), REQ_REGISTER));
        btnServer.setOnClickListener(v -> showServerDialog());
        updateServerLabel();

        if (getIntent().getBooleanExtra("EXPIRED", false)) {
            Snackbar.make(root, "Sesja wygasła — zaloguj się ponownie", Snackbar.LENGTH_LONG).show();
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("goonbank", MODE_PRIVATE);
    }

    private void updateServerLabel() {
        btnServer.setText("Serwer: " + ApiClient.getBaseUrl().replace("http://", "").replace("https://", "").replaceAll("/$", ""));
    }

    private void showServerDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_deposit, null);
        TextInputLayout layout = view.findViewById(R.id.amount_layout);
        EditText input = view.findViewById(R.id.amount);
        view.findViewById(R.id.amount_chips).setVisibility(View.GONE);
        layout.setHint("Adres serwera");
        layout.setSuffixText(null);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(ApiClient.getBaseUrl());
        input.setSelectAllOnFocus(true);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Serwer GoonBank")
                .setIcon(R.drawable.ic_server)
                .setView(view)
                .setPositiveButton("Zapisz", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        ApiClient.setBaseUrl(this, url);
                        updateServerLabel();
                    }
                })
                .setNeutralButton("Domyślny", (d, w) -> {
                    ApiClient.setBaseUrl(this, ApiClient.DEFAULT_BASE_URL);
                    updateServerLabel();
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void setLoading(boolean l) {
        loading = l;
        btnLogin.setEnabled(!l);
        btnLogin.setText(l ? "Logowanie…" : "Zaloguj się");
        progress.setVisibility(l ? View.VISIBLE : View.INVISIBLE);
    }

    private void login() {
        if (loading) return;
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        emailLayout.setError(null);
        passwordLayout.setError(null);
        boolean ok = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Podaj poprawny e-mail");
            ok = false;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("Podaj hasło");
            ok = false;
        }
        if (!ok) return;

        setLoading(true);
        ApiClient.getApiService().login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isFinishing()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse auth = response.body();
                    Session.token = auth.getToken();
                    Session.accountId = auth.getAccountId();
                    Session.firstName = nz(auth.getFirstName());
                    Session.lastName = nz(auth.getLastName());
                    Session.email = nz(auth.getEmail()).isEmpty() ? email : auth.getEmail();
                    Session.accountNo = nz(auth.getAccountNo());
                    Session.currency = auth.getCurrency() != null ? auth.getCurrency() : "PLN";
                    Session.balance = auth.getBalance();
                    prefs().edit().putString("last_email", email).apply();
                    etPassword.setText("");
                    openDashboard();
                } else if (response.code() == 401) {
                    passwordLayout.setError(ApiClient.errorMessage(response).contains("not active")
                            ? "Konto nieaktywne" : "Nieprawidłowy e-mail lub hasło");
                } else {
                    snack(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (isFinishing()) return;
                setLoading(false);
                snack("Brak połączenia z serwerem (" + ApiClient.getBaseUrl() + ")");
            }
        });
    }

    private void openDashboard() {
        Intent intent = new Intent(this, BankMainActivity.class);
        startActivity(intent);
        finish();
    }

    private void snack(String msg) {
        Snackbar.make(findViewById(R.id.main), msg, Snackbar.LENGTH_LONG).show();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_REGISTER && resultCode == RESULT_OK && data != null) {
            etEmail.setText(data.getStringExtra("EMAIL"));
            etPassword.setText("");
            etPassword.requestFocus();
            snack("Konto utworzone! Astolfo mówi: zaloguj się ♡");
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
