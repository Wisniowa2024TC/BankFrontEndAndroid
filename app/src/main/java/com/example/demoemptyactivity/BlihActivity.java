package com.example.demoemptyactivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.BlihCodeRequest;
import com.example.demoemptyactivity.network.BlihCodeResponse;
import com.example.demoemptyactivity.network.BlihPaymentResponse;

import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlihActivity extends AppCompatActivity {

    private TextView tvCode;
    private TextView tvTimer;
    private CountDownTimer timer;
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private String currentCode = "627354";
    private String token;
    private boolean isDialogShowing = false;

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

        token = getIntent().getStringExtra("TOKEN");
        String accountId = getIntent().getStringExtra("ACCOUNT_ID");

        // Display code 627 354
        displayCodeAndStartTimer("627354");

        // Fetch real code from backend if account ID available
        if (token != null && accountId != null && !accountId.isEmpty()) {
            fetchRealBlihCode(token, accountId);
        }

        // Start listening for merchant payment requests for this code
        startPaymentListener();
    }

    private void fetchRealBlihCode(String token, String accountId) {
        ApiClient.getApiService().generateBlihCode("Bearer " + token, new BlihCodeRequest(accountId))
                .enqueue(new Callback<BlihCodeResponse>() {
                    @Override
                    public void onResponse(Call<BlihCodeResponse> call, Response<BlihCodeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String code = response.body().getCode();
                            currentCode = code.replace(" ", "");
                            displayCodeAndStartTimer(code);
                        }
                    }

                    @Override
                    public void onFailure(Call<BlihCodeResponse> call, Throwable t) {}
                });
    }

    private void displayCodeAndStartTimer(String rawCode) {
        currentCode = rawCode.replace(" ", "");
        if (tvCode != null) {
            if (rawCode.length() == 6) {
                tvCode.setText(rawCode.substring(0, 3) + " " + rawCode.substring(3, 6));
            } else {
                tvCode.setText(rawCode);
            }
        }

        if (timer != null) {
            timer.cancel();
        }

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

    // Polls backend every 1.5 seconds for incoming payment requests on this code
    private void startPaymentListener() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDialogShowing && currentCode != null && !currentCode.isEmpty()) {
                    checkIncomingPayment(currentCode);
                }
                pollHandler.postDelayed(this, 1500);
            }
        };
        pollHandler.postDelayed(pollRunnable, 1000);
    }

    private void checkIncomingPayment(String code) {
        ApiClient.getApiService().checkPayment(code).enqueue(new Callback<BlihPaymentResponse>() {
            @Override
            public void onResponse(Call<BlihPaymentResponse> call, Response<BlihPaymentResponse> response) {
                if (response.isSuccessful() && response.body() != null && !isDialogShowing) {
                    BlihPaymentResponse payment = response.body();
                    showPaymentConfirmationDialog(payment);
                }
            }

            @Override
            public void onFailure(Call<BlihPaymentResponse> call, Throwable t) {}
        });
    }

    // Shows confirmation dialog when merchant initiates payment
    private void showPaymentConfirmationDialog(BlihPaymentResponse payment) {
        isDialogShowing = true;
        new AlertDialog.Builder(this)
                .setTitle("💳 Potwierdzenie płatności BLIK")
                .setMessage("Odbiorca: " + payment.getMerchantId() + "\nKwota: " + String.format(Locale.getDefault(), "%.2f zł", payment.getAmount()) + "\n\nCzy akceptujesz płatność?")
                .setCancelable(false)
                .setPositiveButton("✓ Akceptuj", (dialog, which) -> {
                    confirmPayment(payment.getPaymentId(), payment.getAmount());
                })
                .setNegativeButton("✗ Odrzuć", (dialog, which) -> {
                    Toast.makeText(BlihActivity.this, "Płatność odrzucona.", Toast.LENGTH_SHORT).show();
                    isDialogShowing = false;
                })
                .show();
    }

    private void confirmPayment(String paymentId, double amount) {
        String authHeader = token != null ? "Bearer " + token : "";
        ApiClient.getApiService().confirmPayment(authHeader, paymentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(BlihActivity.this, "Płatność " + String.format(Locale.getDefault(), "%.2f zł", amount) + " zaakceptowana!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BlihActivity.this, "Płatność zaakceptowana!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }
}
