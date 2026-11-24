package com.example.demoemptyactivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BankMainActivity extends AppCompatActivity {

    private double currentBalance = 6769.0;
    private TextView tvBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_main);

        // 1. Read real data passed from Login
        Intent intent = getIntent();
        if (intent != null) {
            currentBalance = intent.getDoubleExtra("BALANCE", 6769.0);
            String name = intent.getStringExtra("NAME");
            if (name != null && !name.isEmpty()) {
                Toast.makeText(this, "Zalogowano: " + name, Toast.LENGTH_SHORT).show();
            }
        }

        // 2. Display real balance
        tvBalance = findViewById(R.id.tv_balance);
        updateBalanceDisplay();

        // 3. Wpłać button - increases balance & displays feedback
        View btnWplac = findViewById(R.id.btn_wplac);
        if (btnWplac != null) {
            btnWplac.setOnClickListener(v -> {
                currentBalance += 100.0;
                updateBalanceDisplay();
                Toast.makeText(this, "Wpłacono +100 zł! Nowe saldo: " + String.format(Locale.getDefault(), "%.0f zł", currentBalance), Toast.LENGTH_SHORT).show();
            });
        }

        // 4. Blih button - passes token and account ID to BlihActivity
        View btnBlih = findViewById(R.id.btn_blih);
        if (btnBlih != null) {
            btnBlih.setOnClickListener(v -> {
                Intent blihIntent = new Intent(BankMainActivity.this, BlihActivity.class);
                if (intent != null) {
                    blihIntent.putExtra("TOKEN", intent.getStringExtra("TOKEN"));
                    blihIntent.putExtra("ACCOUNT_ID", intent.getStringExtra("ACCOUNT_ID"));
                }
                startActivity(blihIntent);
            });
        }
    }

    private void updateBalanceDisplay() {
        if (tvBalance != null) {
            tvBalance.setText(String.format(Locale.getDefault(), "%.0f zł", currentBalance));
        }
    }
}
