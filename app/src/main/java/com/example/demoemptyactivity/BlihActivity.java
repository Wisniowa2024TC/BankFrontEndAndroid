package com.example.demoemptyactivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.BlihCodeRequest;
import com.example.demoemptyactivity.network.BlihCodeResponse;

import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlihActivity extends AppCompatActivity {

    private TextView tvCode;
    private TextView tvTimer;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blih);

        tvCode = findViewById(R.id.textView9);
        tvTimer = findViewById(R.id.textView7);
        Button btnWyjdz = findViewById(R.id.button4);

        if (btnWyjdz != null) {
            btnWyjdz.setOnClickListener(v -> finish());
        }

        String token = getIntent().getStringExtra("TOKEN");
        String accountId = getIntent().getStringExtra("ACCOUNT_ID");

        // Fetch real BLIK code from backend if authenticated
        if (token != null && accountId != null && !accountId.isEmpty()) {
            fetchRealBlihCode(token, accountId);
        } else {
            // Fallback for offline / direct testing
            generateFallbackCode();
        }
    }

    private void fetchRealBlihCode(String token, String accountId) {
        if (tvTimer != null) tvTimer.setText("Generowanie kodu...");

        ApiClient.getApiService().generateBlihCode("Bearer " + token, new BlihCodeRequest(accountId))
                .enqueue(new Callback<BlihCodeResponse>() {
                    @Override
                    public void onResponse(Call<BlihCodeResponse> call, Response<BlihCodeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String code = response.body().getCode();
                            displayCodeAndStartTimer(code);
                        } else {
                            generateFallbackCode();
                        }
                    }

                    @Override
                    public void onFailure(Call<BlihCodeResponse> call, Throwable t) {
                        Toast.makeText(BlihActivity.this, "Tryb offline: używam kodu tymczasowego", Toast.LENGTH_SHORT).show();
                        generateFallbackCode();
                    }
                });
    }

    private void generateFallbackCode() {
        int randomCode = 100000 + new Random().nextInt(900000);
        displayCodeAndStartTimer(String.valueOf(randomCode));
    }

    private void displayCodeAndStartTimer(String rawCode) {
        if (tvCode != null) {
            if (rawCode != null && rawCode.length() == 6) {
                // Format: 123 456
                tvCode.setText(rawCode.substring(0, 3) + " " + rawCode.substring(3, 6));
            } else {
                tvCode.setText(rawCode);
            }
        }

        if (timer != null) {
            timer.cancel();
        }

        // 2-minute BLIK countdown timer
        timer = new CountDownTimer(120000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvTimer != null) {
                    long minutes = (millisUntilFinished / 1000) / 60;
                    long seconds = (millisUntilFinished / 1000) % 60;
                    tvTimer.setText(String.format(Locale.getDefault(), "Ważny przez: %02d:%02d", minutes, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (tvTimer != null) tvTimer.setText("Kod wygasł!");
                if (tvCode != null) tvCode.setText("------");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
