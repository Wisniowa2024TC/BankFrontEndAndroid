package com.example.demoemptyactivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.demoemptyactivity.network.AccountResponse;
import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.CreateTransactionRequest;
import com.example.demoemptyactivity.network.TransactionResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BankMainActivity extends AppCompatActivity {

    private static final long AUTO_UPDATE_MS = 5000;
    private static final String[] FILTER_LABELS = {"Wszystkie", "Przychody", "Wydatki"};

    private TextView tvBalance;
    private Chip chipFilter;
    private LinearLayout transactionsContainer;
    private SwipeRefreshLayout swipeRefresh;

    private final Handler autoUpdateHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccountData(false);
            autoUpdateHandler.postDelayed(this, AUTO_UPDATE_MS);
        }
    };

    // 0 = all, 1 = incoming, 2 = outgoing
    private int currentFilter = 0;
    private final List<TransactionResponse> allTransactions = new ArrayList<>();
    private boolean transactionsLoaded = false;
    private boolean fetchInFlight = false;
    private boolean depositInFlight = false;
    private boolean offlineShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn()) {
            Session.kickToLogin(this, false);
            return;
        }
        setContentView(R.layout.activity_bank_main);
        getWindow().setStatusBarColor(0xFFFFD5E1);

        tvBalance = findViewById(R.id.tv_balance);
        chipFilter = findViewById(R.id.btn_filter_transactions);
        transactionsContainer = findViewById(R.id.transactions_container);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        TextView greeting = findViewById(R.id.tv_greeting);
        if (!Session.firstName.isEmpty()) greeting.setText("Hej " + Session.firstName + " ♡ Twoje saldo");

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.pink_primary));
        swipeRefresh.setOnRefreshListener(() -> fetchAccountData(true));
        // Only let pull-to-refresh trigger when the history list is scrolled to top
        View scroll = findViewById(R.id.transactions_scroll);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> scroll.canScrollVertically(-1));

        findViewById(R.id.btn_settings).setOnClickListener(v -> showSettingsDialog());
        findViewById(R.id.btn_profile).setOnClickListener(v -> showProfileSheet());
        findViewById(R.id.btn_wplac).setOnClickListener(v -> showDepositDialog());
        findViewById(R.id.btn_blih).setOnClickListener(v -> startActivity(new Intent(this, BlihActivity.class)));
        chipFilter.setOnClickListener(v -> showFilterDialog());

        updateBalanceDisplay();
        renderTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.isLoggedIn()) return;
        autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
        autoUpdateHandler.post(autoUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
    }

    private void updateBalanceDisplay() {
        tvBalance.setText(Session.money(Session.balance));
    }

    // ---------- Filter ----------
    private void showFilterDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Filtruj historię")
                .setSingleChoiceItems(new String[]{"Wszystkie", "Przychody ↙", "Wydatki ↗"}, currentFilter, (dialog, which) -> {
                    currentFilter = which;
                    chipFilter.setText(FILTER_LABELS[which]);
                    renderTransactions();
                    dialog.dismiss();
                })
                .show();
    }

    // ---------- Transactions list ----------
    private void renderTransactions() {
        transactionsContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int shown = 0;
        for (TransactionResponse t : allTransactions) {
            boolean in = t.isIncoming(Session.accountId);
            if (currentFilter == 1 && !in) continue;
            if (currentFilter == 2 && in) continue;

            View row = inflater.inflate(R.layout.item_transaction, transactionsContainer, false);
            ImageView icon = row.findViewById(R.id.tx_icon);
            TextView title = row.findViewById(R.id.tx_title);
            TextView sub = row.findViewById(R.id.tx_subtitle);
            TextView amount = row.findViewById(R.id.tx_amount);
            TextView date = row.findViewById(R.id.tx_date);

            int color = ContextCompat.getColor(this, in ? R.color.money_in : R.color.money_out);
            icon.setImageResource(in ? R.drawable.ic_arrow_in : R.drawable.ic_arrow_out);
            icon.setBackgroundResource(in ? R.drawable.bg_tx_icon_in : R.drawable.bg_tx_icon_out);
            icon.setColorFilter(color);
            title.setText(t.displayTitle(Session.accountId));
            sub.setText(t.displaySubtitle(Session.accountId));
            amount.setText((in ? "+" : "−") + Session.money(t.getAmount()));
            amount.setTextColor(color);
            if (t.getStatus() != TransactionResponse.STATUS_COMPLETED) {
                amount.setPaintFlags(t.getStatus() == TransactionResponse.STATUS_FAILED
                        ? amount.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        : amount.getPaintFlags());
                row.setAlpha(0.55f);
            }
            date.setText(t.displayDate());
            transactionsContainer.addView(row);
            shown++;
        }

        if (shown == 0) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setPadding(0, dp(24), 0, dp(24));

            ImageView astolfo = new ImageView(this);
            astolfo.setImageResource(R.drawable.astolfochibiblih);
            astolfo.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(90)));
            empty.addView(astolfo);

            TextView tv = new TextView(this);
            tv.setText(!transactionsLoaded ? "Astolfo ładuje historię…"
                    : currentFilter == 0 ? "Brak transakcji — wpłać coś, Astolfo czeka ♡"
                    : "Brak transakcji w tej kategorii");
            tv.setTextColor(ContextCompat.getColor(this, R.color.pink_on_surface_variant));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dp(8), 0, 0);
            empty.addView(tv);
            transactionsContainer.addView(empty);
        }
    }

    // ---------- Backend sync ----------
    private void fetchAccountData(boolean manual) {
        if (fetchInFlight) {
            if (manual) swipeRefresh.setRefreshing(false);
            return;
        }
        fetchInFlight = true;
        final int[] pending = {2};
        Runnable done = () -> {
            if (--pending[0] == 0) {
                fetchInFlight = false;
                swipeRefresh.setRefreshing(false);
            }
        };

        ApiClient.getApiService().getAccount(Session.bearer(), Session.accountId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (isFinishing()) return;
                done.run();
                if (handleAuthError(response)) return;
                if (response.isSuccessful() && response.body() != null) {
                    AccountResponse a = response.body();
                    Session.balance = a.getBalance();
                    if (a.getAccountNo() != null) Session.accountNo = a.getAccountNo();
                    if (a.getEmail() != null) Session.email = a.getEmail();
                    updateBalanceDisplay();
                    offlineShown = false;
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable t) {
                if (isFinishing()) return;
                done.run();
                if (manual || !offlineShown) {
                    offlineShown = true;
                    snack("Brak połączenia z serwerem");
                }
            }
        });

        ApiClient.getApiService().getTransactions(Session.bearer(), Session.accountId).enqueue(new Callback<List<TransactionResponse>>() {
            @Override
            public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                if (isFinishing()) return;
                done.run();
                if (handleAuthError(response)) return;
                if (response.isSuccessful() && response.body() != null) {
                    allTransactions.clear();
                    allTransactions.addAll(response.body());
                    transactionsLoaded = true;
                    renderTransactions();
                }
            }

            @Override
            public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {
                if (isFinishing()) return;
                done.run();
            }
        });
    }

    /** Returns true if the token is no longer valid and the user was sent to login. */
    private boolean handleAuthError(Response<?> response) {
        if (response.code() == 401) {
            autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
            Session.kickToLogin(this, true);
            return true;
        }
        return false;
    }

    // ---------- Deposit ----------
    private void showDepositDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_deposit, null);
        EditText input = view.findViewById(R.id.amount);
        TextInputLayout layout = view.findViewById(R.id.amount_layout);
        ChipGroup chips = view.findViewById(R.id.amount_chips);
        for (int i = 0; i < chips.getChildCount(); i++) {
            Chip c = (Chip) chips.getChildAt(i);
            c.setOnClickListener(v -> {
                input.setText(c.getText());
                input.setSelection(input.length());
            });
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Wpłata")
                .setMessage("Ile wpłacasz na konto?")
                .setView(view)
                .setPositiveButton("Wpłać", null)
                .setNegativeButton("Anuluj", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double amount;
            try {
                amount = Double.parseDouble(input.getText().toString().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                layout.setError("Podaj kwotę");
                return;
            }
            amount = Math.round(amount * 100) / 100.0;
            if (amount < 0.01) { layout.setError("Kwota musi być większa od 0"); return; }
            if (amount > 1_000_000) { layout.setError("Max 1 000 000 zł"); return; }
            dialog.dismiss();
            doDeposit(amount);
        }));
        Ui.clearErrorOnEdit(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        input.requestFocus();
        dialog.show();
    }

    private void doDeposit(double amount) {
        if (depositInFlight) return;
        depositInFlight = true;
        CreateTransactionRequest req = new CreateTransactionRequest(
                Session.accountId, null, amount, CreateTransactionRequest.TYPE_DEPOSIT, "Wpłata własna");
        ApiClient.getApiService().createTransaction(Session.bearer(), req).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                if (isFinishing()) return;
                if (handleAuthError(response)) return;
                if (!response.isSuccessful() || response.body() == null) {
                    depositInFlight = false;
                    snack(ApiClient.errorMessage(response));
                    return;
                }
                executeDeposit(response.body().getId(), amount);
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable t) {
                if (isFinishing()) return;
                depositInFlight = false;
                snack("Brak połączenia — wpłata nie powiodła się");
            }
        });
    }

    private void executeDeposit(String txId, double amount) {
        ApiClient.getApiService().executeTransaction(Session.bearer(), txId).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                if (isFinishing()) return;
                depositInFlight = false;
                if (handleAuthError(response)) return;
                if (response.isSuccessful()) {
                    snack("Wpłacono " + Session.money(amount) + " ♡");
                } else {
                    snack(ApiClient.errorMessage(response));
                }
                fetchAccountData(false);
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable t) {
                if (isFinishing()) return;
                depositInFlight = false;
                snack("Brak połączenia — sprawdź historię");
                fetchAccountData(false);
            }
        });
    }

    // ---------- Settings ----------
    private void showSettingsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Ustawienia")
                .setIcon(R.drawable.ic_settings)
                .setMessage("GOONBANK Mobile 1.0\nSerwer: " + ApiClient.getBaseUrl()
                        + "\nWaluta: " + Session.currency
                        + "\n\nAdres serwera zmienisz na ekranie logowania.")
                .setPositiveButton("Zamknij", null)
                .show();
    }

    // ---------- Profile ----------
    private void showProfileSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.sheet_profile, null);
        ((TextView) v.findViewById(R.id.profile_name)).setText((Session.firstName + " " + Session.lastName).trim());
        ((TextView) v.findViewById(R.id.profile_email)).setText(Session.email);
        String accNo = Session.formatAccountNo(Session.accountNo);
        ((TextView) v.findViewById(R.id.profile_account_no)).setText(accNo.isEmpty() ? "—" : accNo);
        ((TextView) v.findViewById(R.id.profile_meta)).setText("Waluta: " + Session.currency + "   •   Saldo: " + Session.money(Session.balance));
        v.findViewById(R.id.profile_account_card).setOnClickListener(x -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Numer konta", Session.accountNo));
            snack("Skopiowano numer konta");
        });
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> {
            sheet.dismiss();
            confirmLogout();
        });
        sheet.setContentView(v);
        sheet.show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Wylogować?")
                .setMessage("Astolfo będzie tęsknić ♡")
                .setPositiveButton("Wyloguj", (d, w) -> performLogout())
                .setNegativeButton("Zostań", null)
                .show();
    }

    private void performLogout() {
        autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
        ApiClient.getApiService().logout(Session.bearer()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
        Session.kickToLogin(this, false);
    }

    private void snack(String msg) {
        Snackbar.make(findViewById(R.id.bank_main), msg, Snackbar.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
