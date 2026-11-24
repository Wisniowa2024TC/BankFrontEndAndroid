package com.example.demoemptyactivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.TransactionResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BankMainActivity extends AppCompatActivity {

    private double currentBalance = 6769.0;
    private TextView tvBalance;
    private LinearLayout transactionsContainer;
    private String token;
    private String accountId;
    private String userName = "Artem Kowalski";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_main);

        // 1. Retrieve session data from login Intent
        Intent intent = getIntent();
        if (intent != null) {
            currentBalance = intent.getDoubleExtra("BALANCE", 6769.0);
            token = intent.getStringExtra("TOKEN");
            accountId = intent.getStringExtra("ACCOUNT_ID");
            String name = intent.getStringExtra("NAME");
            if (name != null && !name.isEmpty()) {
                userName = name;
            }
        }

        // 2. Set balance display
        tvBalance = findViewById(R.id.tv_balance);
        transactionsContainer = findViewById(R.id.transactions_container);
        updateBalanceDisplay();

        // 3. Settings button (≡)
        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsDialog());
        }

        // 4. Profile & Logout button (👤)
        View btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> showProfileDialog());
        }

        // 5. Wpłać button - Opens Deposit Dialog
        View btnWplac = findViewById(R.id.btn_wplac);
        if (btnWplac != null) {
            btnWplac.setOnClickListener(v -> showDepositDialog());
        }

        // 6. Blih button - Opens BLIK Screen
        View btnBlih = findViewById(R.id.btn_blih);
        if (btnBlih != null) {
            btnBlih.setOnClickListener(v -> {
                Intent blihIntent = new Intent(BankMainActivity.this, BlihActivity.class);
                blihIntent.putExtra("TOKEN", token);
                blihIntent.putExtra("ACCOUNT_ID", accountId);
                startActivity(blihIntent);
            });
        }

        // 7. Load backend transaction history
        loadTransactions();
    }

    private void updateBalanceDisplay() {
        if (tvBalance != null) {
            tvBalance.setText(String.format(Locale.getDefault(), "%.0f zł", currentBalance));
        }
    }

    // --- DIALOG: SETTINGS (≡) ---
    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⚙️ Ustawienia Banku")
                .setMessage("Bank: GOONBANK Mobile\nWersja: 1.0.0 (Release)\nJęzyk: Polski\nSerwer: http://10.0.2.2:5001\nStatus: Aktywny")
                .setPositiveButton("Zamknij", null)
                .show();
    }

    // --- DIALOG: PROFILE & LOGOUT (👤) ---
    private void showProfileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("👤 Profil Użytkownika")
                .setMessage("Imię i nazwisko: " + userName + "\nNr konta: PL61 1090 1014 0000 0712 1981 2874\nWaluta: PLN\nStatus konta: Aktywne")
                .setPositiveButton("Zamknij", null)
                .setNegativeButton("Wyloguj się", (dialog, which) -> performLogout())
                .show();
    }

    // --- LOGOUT LOGIC ---
    private void performLogout() {
        if (token != null) {
            ApiClient.getApiService().logout("Bearer " + token).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }

        Toast.makeText(this, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show();
        Intent loginIntent = new Intent(this, MainActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    // --- DIALOG: DEPOSIT (Wpłać) ---
    private void showDepositDialog() {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Kwota np. 100");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("💵 Wpłata Gotówkowa")
                .setMessage("Wprowadź kwotę do wpłacenia na konto (PLN):")
                .setView(input)
                .setPositiveButton("Wpłać", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(val);
                            if (amount > 0) {
                                currentBalance += amount;
                                updateBalanceDisplay();

                                String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
                                addTransactionRow("Wpłata we wpłatomacie", "Wpłata gotówkowa", amount, today, true);

                                Toast.makeText(BankMainActivity.this, "Wpłacono +" + String.format(Locale.getDefault(), "%.0f zł", amount) + "!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    // --- TRANSACTION HISTORY LOADING ---
    private void loadTransactions() {
        if (token != null && accountId != null) {
            ApiClient.getApiService().getTransactions("Bearer " + token, accountId)
                    .enqueue(new Callback<List<TransactionResponse>>() {
                        @Override
                        public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                if (transactionsContainer != null) {
                                    transactionsContainer.removeAllViews();
                                }
                                for (TransactionResponse t : response.body()) {
                                    addTransactionRow(t.getTitle(), t.getDescription(), t.getAmount(), t.getDate(), t.isIncoming());
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {
                            // Offline or network error: existing default items stay visible
                        }
                    });
        }
    }

    // Dynamically adds a new transaction row to the scrollable card
    private void addTransactionRow(String title, String subtitle, double amount, String date, boolean isIncoming) {
        if (transactionsContainer == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 0);
        row.setLayoutParams(params);

        // Arrow icon
        TextView tvArrow = new TextView(this);
        tvArrow.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)));
        tvArrow.setGravity(Gravity.CENTER);
        tvArrow.setText(isIncoming ? "↙" : "↗");
        tvArrow.setTextSize(20);
        tvArrow.setTextColor(isIncoming ? 0xFF2E7D32 : 0xFFC62828);
        tvArrow.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvArrow);

        // Title and Subtitle column
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        colParams.setMargins(dpToPx(12), 0, 0, 0);
        textCol.setLayoutParams(colParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(0xFF1A1A24);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(subtitle);
        tvSubtitle.setTextSize(12);
        tvSubtitle.setTextColor(0xFF666666);
        textCol.addView(tvSubtitle);

        row.addView(textCol);

        // Amount and Date column
        LinearLayout amountCol = new LinearLayout(this);
        amountCol.setOrientation(LinearLayout.VERTICAL);
        amountCol.setGravity(Gravity.END);
        amountCol.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvAmount = new TextView(this);
        tvAmount.setText((isIncoming ? "+" : "-") + String.format(Locale.getDefault(), "%.0f zł", amount));
        tvAmount.setTextSize(15);
        tvAmount.setTextColor(isIncoming ? 0xFF2E7D32 : 0xFFC62828);
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
        amountCol.addView(tvAmount);

        TextView tvDate = new TextView(this);
        tvDate.setText(date);
        tvDate.setTextSize(12);
        tvDate.setTextColor(0xFF777777);
        amountCol.addView(tvDate);

        row.addView(amountCol);

        // Insert at the top of the history list
        transactionsContainer.addView(row, 0);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
