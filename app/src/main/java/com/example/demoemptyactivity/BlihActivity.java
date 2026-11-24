package com.example.demoemptyactivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.BlihCodeRequest;
import com.example.demoemptyactivity.network.BlihCodeResponse;
import com.example.demoemptyactivity.network.BlihPaymentResponse;
import com.example.demoemptyactivity.network.CreateBlihProfileRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlihActivity extends AppCompatActivity {

    private static final long CODE_TTL_MS = 120_000;
    private static final long POLL_MS = 1500;

    private TextView tvCode;
    private TextView tvTimer;
    private CountDownTimer timer;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (paymentDialog == null && !actionInFlight) checkIncomingPayment();
            pollHandler.postDelayed(this, POLL_MS);
        }
    };

    private boolean codeActive = false;
    private boolean fetchingCode = false;
    private boolean actionInFlight = false;
    private boolean triedCreatingProfile = false;
    private AlertDialog paymentDialog;
    private final Set<String> handledPayments = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn()) {
            Session.kickToLogin(this, false);
            return;
        }
        setContentView(R.layout.blih);
        getWindow().setStatusBarColor(0xFFFFD5E1);
        getWindow().setNavigationBarColor(0xFFFFD5E1);

        tvCode = findViewById(R.id.textView9);
        tvTimer = findViewById(R.id.textView7);
        findViewById(R.id.button4).setOnClickListener(v -> finish());

        View.OnClickListener refresh = v -> { if (!codeActive) fetchCode(); };
        tvCode.setOnClickListener(refresh);
        tvTimer.setOnClickListener(refresh);

        fetchCode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    // ---------- Code ----------
    private void fetchCode() {
        if (fetchingCode) return;
        fetchingCode = true;
        codeActive = false;
        if (timer != null) timer.cancel();
        tvCode.setText("------");
        tvTimer.setText("Astolfo generuje kod…");
        tvCode.setAlpha(1f);

        ApiClient.getApiService().generateBlihCode(Session.bearer(), new BlihCodeRequest(Session.accountId))
                .enqueue(new Callback<BlihCodeResponse>() {
                    @Override
                    public void onResponse(Call<BlihCodeResponse> call, Response<BlihCodeResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        fetchingCode = false;
                        if (response.code() == 401) { Session.kickToLogin(BlihActivity.this, true); return; }
                        if (response.isSuccessful() && response.body() != null && response.body().getCode() != null) {
                            showCode(response.body().getCode().replace(" ", ""));
                        } else if (response.code() == 400 && !triedCreatingProfile) {
                            // BLIH not enabled for this account yet -> enable it and retry
                            triedCreatingProfile = true;
                            enableBlihProfile();
                        } else {
                            showError(ApiClient.errorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<BlihCodeResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        fetchingCode = false;
                        showError("Brak połączenia z serwerem");
                    }
                });
    }

    private void enableBlihProfile() {
        tvTimer.setText("Aktywuję BLIH…");
        ApiClient.getApiService().createBlihProfile(Session.bearer(),
                new CreateBlihProfileRequest(Session.accountId, sha256("goonbank-phone:" + Session.accountId)))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isFinishing() || isDestroyed()) return;
                        // 409 = profile already exists; either way try again
                        if (response.isSuccessful() || response.code() == 409) fetchCode();
                        else showError(ApiClient.errorMessage(response));
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        showError("Brak połączenia z serwerem");
                    }
                });
    }

    private void showError(String msg) {
        codeActive = false;
        tvCode.setText("------");
        tvTimer.setText("Dotknij, aby spróbować ponownie");
        snack(msg);
    }

    private void showCode(String code) {
        codeActive = true;
        tvCode.setAlpha(1f);
        tvCode.setText(code.length() == 6 ? code.substring(0, 3) + " " + code.substring(3) : code);
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(CODE_TTL_MS, 1000) {
            @Override
            public void onTick(long ms) {
                long s = ms / 1000;
                tvTimer.setText(String.format(Locale.getDefault(), "Ważny przez %d:%02d", s / 60, s % 60));
            }

            @Override
            public void onFinish() {
                codeActive = false;
                tvCode.setText("------");
                tvTimer.setText("Kod wygasł — dotknij, aby odświeżyć");
            }
        }.start();
    }

    // ---------- Incoming payments ----------
    private void checkIncomingPayment() {
        ApiClient.getApiService().getPendingPayment(Session.bearer(), Session.accountId).enqueue(new Callback<BlihPaymentResponse>() {
            @Override
            public void onResponse(Call<BlihPaymentResponse> call, Response<BlihPaymentResponse> response) {
                if (isFinishing() || isDestroyed() || paymentDialog != null) return;
                BlihPaymentResponse p = response.body();
                if (response.isSuccessful() && p != null && p.getPaymentId() != null
                        && !handledPayments.contains(p.getPaymentId())) {
                    showPaymentDialog(p);
                }
            }

            @Override
            public void onFailure(Call<BlihPaymentResponse> call, Throwable t) {}
        });
    }

    private void showPaymentDialog(BlihPaymentResponse p) {
        // Code was consumed by the merchant
        codeActive = false;
        if (timer != null) timer.cancel();
        tvTimer.setText("Kod użyty ✓");
        tvCode.setAlpha(0.35f);

        View v = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        ((TextView) v.findViewById(R.id.pay_amount)).setText(Session.money(p.getAmount()));
        ((TextView) v.findViewById(R.id.pay_merchant)).setText("Odbiorca: " + p.getMerchantId());

        paymentDialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .setPositiveButton("Zapłać", (d, w) -> respond(p, true))
                .setNegativeButton("Odrzuć", (d, w) -> respond(p, false))
                .show();
    }

    private void respond(BlihPaymentResponse p, boolean accept) {
        paymentDialog = null;
        handledPayments.add(p.getPaymentId());
        actionInFlight = true;
        Call<Void> call = accept
                ? ApiClient.getApiService().confirmPayment(Session.bearer(), p.getPaymentId())
                : ApiClient.getApiService().rejectPayment(Session.bearer(), p.getPaymentId());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> response) {
                if (isFinishing() || isDestroyed()) return;
                actionInFlight = false;
                if (response.isSuccessful()) {
                    if (accept) {
                        showResult("Zapłacono!", "Odbiorca: " + p.getMerchantId() + "\nAstolfo jest dumny ♡", true, p.getAmount());
                    } else {
                        snack("Płatność odrzucona");
                        fetchCode();
                    }
                } else {
                    String msg = ApiClient.errorMessage(response);
                    if (msg.contains("Insufficient")) msg = "Za mało środków na koncie";
                    else if (msg.contains("timed out")) msg = "Czas na potwierdzenie minął";
                    showResult("Płatność nieudana", msg, false, 0);
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                actionInFlight = false;
                showResult("Brak połączenia", "Nie wiadomo, czy płatność przeszła — sprawdź historię.", false, 0);
            }
        });
    }

    private void showResult(String title, String msg, boolean success, double amount) {
        View v = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        ((ImageView) v.findViewById(R.id.pay_image)).setImageResource(
                success ? R.drawable.astolfochibiblih : R.drawable.astolfoincorner);
        ((TextView) v.findViewById(R.id.pay_title)).setText(title);
        TextView amountView = v.findViewById(R.id.pay_amount);
        if (success) amountView.setText("−" + Session.money(amount));
        else amountView.setVisibility(View.GONE);
        ((TextView) v.findViewById(R.id.pay_merchant)).setText(msg);

        new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .setPositiveButton(success ? "Super ♡" : "Nowy kod", (d, w) -> {
                    if (success) finish(); else fetchCode();
                })
                .show();
    }

    private void snack(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.button4)
                .show();
    }

    private static String sha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        pollHandler.removeCallbacks(pollRunnable);
        if (paymentDialog != null) paymentDialog.dismiss();
    }
}
