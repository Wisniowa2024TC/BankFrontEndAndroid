package com.example.demoemptyactivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class BankMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_main);

        // Wpłać button onclick
        View btnWplac = findViewById(R.id.btn_wplac);
        if (btnWplac != null) {
            btnWplac.setOnClickListener(v -> {
                Toast.makeText(this, "Wpłacono",
                Toast.LENGTH_SHORT).show();
            });
        }

        View btnBlih = findViewById(R.id.btn_blih);
        if (btnBlih != null) {
            btnBlih.setOnClickListener(v -> {
                Intent intent = new Intent(BankMainActivity.this, BlihActivity.class);
                startActivity(intent);
            });
        }
    }
}
