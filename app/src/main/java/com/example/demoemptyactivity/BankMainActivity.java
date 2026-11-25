package com.example.demoemptyactivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.example.demoemptyactivity.network.BlihInitiateResponse;
import com.example.demoemptyactivity.network.BlihStatusResponse;
import com.example.demoemptyactivity.network.CreateTransactionRequest;
import com.example.demoemptyactivity.network.InitiateBlihPaymentRequest;
import com.example.demoemptyactivity.network.TransactionResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BankMainActivity extends AppCompatActivity {

    private static final long AUTO_UPDATE_MS = 5000;
    // Backend expires a pending BLIH payment 2 minutes after it was initiated.
    private static final long BLIH_WAIT_MS = 125_000;
    private static final long BLIH_POLL_MS = 2000;
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
    private boolean transferInFlight = false;
    private boolean offlineShown = false;

    // Incoming BLIH transfer we are waiting for the sender to approve
    private String blihWaitingId;
    private double blihWaitingAmount;
    private long blihWaitDeadline;
    private AlertDialog blihWaitDialog;
    private final Runnable blihPollRunnable = this::pollBlihStatus;

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
        findViewById(R.id.btn_transfer).setOnClickListener(v -> showTransferDialog());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBlihWaiting();
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
                    : currentFilter == 0 ? "Brak transakcji — Astolfo czeka ♡"
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

    // ---------- Transfer ----------
    private void showTransferDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_transfer, null);
        MaterialButtonToggleGroup mode = view.findViewById(R.id.transfer_mode);
        TextInputLayout recipientLayout = view.findViewById(R.id.recipient_layout);
        EditText recipient = view.findViewById(R.id.recipient);
        TextInputLayout codeLayout = view.findViewById(R.id.blih_code_layout);
        EditText code = view.findViewById(R.id.blih_code);
        TextInputLayout amountLayout = view.findViewById(R.id.amount_layout);
        EditText amountInput = view.findViewById(R.id.amount);
        TextInputLayout titleLayout = view.findViewById(R.id.title_layout);
        EditText titleInput = view.findViewById(R.id.transfer_title);
        TextView hint = view.findViewById(R.id.transfer_balance);

        ChipGroup chips = view.findViewById(R.id.amount_chips);
        for (int i = 0; i < chips.getChildCount(); i++) {
            Chip c = (Chip) chips.getChildAt(i);
            c.setOnClickListener(v -> {
                amountInput.setText(c.getText());
                amountInput.setSelection(amountInput.length());
            });
        }

        recipientLayout.setEndIconOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                recipientLayout.setError("Schowek jest pusty");
                return;
            }
            recipient.setText(clip.getItemAt(0).coerceToText(this).toString().trim());
            recipient.setSelection(recipient.length());
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Przelew")
                .setView(view)
                .setPositiveButton("Dalej", null)
                .setNegativeButton("Anuluj", null)
                .create();

        Runnable applyMode = () -> {
            boolean send = mode.getCheckedButtonId() == R.id.mode_send;
            recipientLayout.setVisibility(send ? View.VISIBLE : View.GONE);
            titleLayout.setVisibility(send ? View.VISIBLE : View.GONE);
            codeLayout.setVisibility(send ? View.GONE : View.VISIBLE);
            hint.setText(send ? "Dostępne: " + Session.money(Session.balance)
                    : "Pieniądze wpłyną, gdy nadawca zatwierdzi przelew (ma na to 2 minuty)");
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive != null) positive.setText(send ? "Dalej" : "Poproś");
            (send ? recipient : code).requestFocus();
        };
        mode.addOnButtonCheckedListener((group, id, checked) -> { if (checked) applyMode.run(); });

        dialog.setOnShowListener(d -> {
            applyMode.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                boolean send = mode.getCheckedButtonId() == R.id.mode_send;
                boolean ok = true;
                String target = null, blihCode = null;
                if (send) {
                    target = recipient.getText().toString().replaceAll("\\s", "").toLowerCase(Locale.ROOT);
                    String err = recipientError(target);
                    if (err != null) { recipientLayout.setError(err); ok = false; }
                } else {
                    blihCode = code.getText().toString().replaceAll("\\s", "");
                    if (!blihCode.matches("\\d{6}")) { codeLayout.setError("Kod BLIH ma 6 cyfr"); ok = false; }
                }
                Double amount = parseAmount(amountLayout, amountInput, send);
                if (!ok || amount == null) return;

                dialog.dismiss();
                if (send) confirmTransfer(target, amount, titleInput.getText().toString().trim());
                else requestBlihTransfer(blihCode, amount);
            });
        });
        Ui.clearErrorOnEdit(recipientLayout, codeLayout, amountLayout);
        dialog.show();
    }

    private String recipientError(String id) {
        if (id.isEmpty()) return "Podaj ID konta odbiorcy";
        if (id.matches("\\d{26}")) return "Serwer nie wspiera numeru konta — użyj ID do przelewów odbiorcy";
        if (!AccountResponse.isAccountId(id)) return "Nieprawidłowe ID (format: 1a2b3c4d-…)";
        if (id.equalsIgnoreCase(Session.accountId)) return "To Twoje własne konto";
        return null;
    }

    /** Parses the amount field, showing an error on it and returning null if it is invalid. */
    private Double parseAmount(TextInputLayout layout, EditText input, boolean mustCoverBalance) {
        double amount;
        try {
            amount = Double.parseDouble(input.getText().toString().trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            layout.setError("Podaj kwotę");
            return null;
        }
        amount = Math.round(amount * 100) / 100.0;
        if (amount < 0.01) { layout.setError("Kwota musi być większa od 0"); return null; }
        if (amount > 1_000_000) { layout.setError("Max 1 000 000 zł"); return null; }
        if (mustCoverBalance && amount > Session.balance) {
            layout.setError("Za mało środków (masz " + Session.money(Session.balance) + ")");
            return null;
        }
        return amount;
    }

    private void confirmTransfer(String target, double amount, String title) {
        String name = (Session.firstName + " " + Session.lastName).trim();
        String finalTitle = !title.isEmpty() ? title : name.isEmpty() ? "Przelew" : "Przelew od " + name;

        View v = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        ((ImageView) v.findViewById(R.id.pay_image)).setImageResource(R.drawable.astolfochibiblih);
        ((TextView) v.findViewById(R.id.pay_title)).setText("Wysłać przelew?");
        ((TextView) v.findViewById(R.id.pay_amount)).setText(Session.money(amount));
        ((TextView) v.findViewById(R.id.pay_merchant)).setText("Do: " + target + "\nTytuł: " + finalTitle);

        new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setPositiveButton("Wyślij", (d, w) -> doTransfer(target, amount, finalTitle))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void doTransfer(String target, double amount, String title) {
        if (transferInFlight) return;
        transferInFlight = true;
        CreateTransactionRequest req = new CreateTransactionRequest(
                Session.accountId, target, amount, CreateTransactionRequest.TYPE_TRANSFER, title);
        ApiClient.getApiService().createTransaction(Session.bearer(), req).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                if (isFinishing()) return;
                if (handleAuthError(response)) return;
                if (!response.isSuccessful() || response.body() == null) {
                    transferInFlight = false;
                    snack(friendlyError(ApiClient.errorMessage(response)));
                    return;
                }
                executeTransfer(response.body().getId(), amount);
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable t) {
                if (isFinishing()) return;
                transferInFlight = false;
                snack("Brak połączenia — przelew nie został wysłany");
            }
        });
    }

    private void executeTransfer(String txId, double amount) {
        ApiClient.getApiService().executeTransaction(Session.bearer(), txId).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                if (isFinishing()) return;
                transferInFlight = false;
                if (handleAuthError(response)) return;
                if (response.isSuccessful()) {
                    snack("Wysłano " + Session.money(amount) + " ♡");
                } else {
                    snack(friendlyError(ApiClient.errorMessage(response)));
                }
                fetchAccountData(false);
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable t) {
                if (isFinishing()) return;
                transferInFlight = false;
                snack("Brak połączenia — sprawdź historię");
                fetchAccountData(false);
            }
        });
    }

    // ---------- Receive via BLIH ----------
    // We act as the "merchant" with our own account ID as merchantId; the backend debits the owner
    // of the code once they approve it in their BLIH screen and credits our account.
    private void requestBlihTransfer(String blihCode, double amount) {
        if (transferInFlight) return;
        transferInFlight = true;
        ApiClient.getApiService().initiateBlihPayment(new InitiateBlihPaymentRequest(blihCode, Session.accountId, amount))
                .enqueue(new Callback<BlihInitiateResponse>() {
                    @Override
                    public void onResponse(Call<BlihInitiateResponse> call, Response<BlihInitiateResponse> response) {
                        if (isFinishing()) return;
                        transferInFlight = false;
                        if (response.isSuccessful() && response.body() != null && response.body().getPaymentId() != null) {
                            showBlihWaiting(response.body().getPaymentId(), amount);
                        } else {
                            snack(friendlyError(ApiClient.errorMessage(response)));
                        }
                    }

                    @Override
                    public void onFailure(Call<BlihInitiateResponse> call, Throwable t) {
                        if (isFinishing()) return;
                        transferInFlight = false;
                        snack("Brak połączenia z serwerem");
                    }
                });
    }

    private void showBlihWaiting(String paymentId, double amount) {
        View v = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        ((ImageView) v.findViewById(R.id.pay_image)).setImageResource(R.drawable.astolfochibiblih);
        ((TextView) v.findViewById(R.id.pay_title)).setText("Czekam na nadawcę…");
        ((TextView) v.findViewById(R.id.pay_amount)).setText("+" + Session.money(amount));
        ((TextView) v.findViewById(R.id.pay_merchant)).setText("Nadawca musi zatwierdzić przelew w swoim BLIH");

        blihWaitingId = paymentId;
        blihWaitingAmount = amount;
        blihWaitDeadline = SystemClock.elapsedRealtime() + BLIH_WAIT_MS;
        blihWaitDialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .setNegativeButton("Nie czekaj", (d, w) -> {
                    stopBlihWaiting();
                    snack("Jeśli nadawca zatwierdzi, pieniądze i tak wpłyną");
                })
                .show();
        autoUpdateHandler.postDelayed(blihPollRunnable, BLIH_POLL_MS);
    }

    private void pollBlihStatus() {
        String id = blihWaitingId;
        if (id == null) return;
        ApiClient.getApiService().getBlihPaymentStatus(id).enqueue(new Callback<BlihStatusResponse>() {
            @Override
            public void onResponse(Call<BlihStatusResponse> call, Response<BlihStatusResponse> response) {
                if (isFinishing() || isDestroyed() || !id.equals(blihWaitingId)) return;
                String status = response.isSuccessful() && response.body() != null ? response.body().getStatus() : null;
                if (BlihStatusResponse.APPROVED.equals(status)) {
                    double amount = blihWaitingAmount;
                    stopBlihWaiting();
                    showOutcome(true, "Otrzymano!", "+" + Session.money(amount), "Nadawca zatwierdził przelew ♡");
                    fetchAccountData(false);
                } else if (BlihStatusResponse.FAILED.equals(status)) {
                    stopBlihWaiting();
                    showOutcome(false, "Przelew nieudany", null, "Nadawca odrzucił przelew, nie miał środków albo minął czas");
                } else {
                    scheduleBlihPoll();
                }
            }

            @Override
            public void onFailure(Call<BlihStatusResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed() || !id.equals(blihWaitingId)) return;
                scheduleBlihPoll();
            }
        });
    }

    private void scheduleBlihPoll() {
        if (SystemClock.elapsedRealtime() > blihWaitDeadline) {
            stopBlihWaiting();
            showOutcome(false, "Czas minął", null, "Nadawca nie zatwierdził przelewu w ciągu 2 minut");
            return;
        }
        autoUpdateHandler.postDelayed(blihPollRunnable, BLIH_POLL_MS);
    }

    private void stopBlihWaiting() {
        blihWaitingId = null;
        autoUpdateHandler.removeCallbacks(blihPollRunnable);
        if (blihWaitDialog != null) {
            if (blihWaitDialog.isShowing()) blihWaitDialog.dismiss();
            blihWaitDialog = null;
        }
    }

    private void showOutcome(boolean success, String title, String amount, String msg) {
        View v = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        ((ImageView) v.findViewById(R.id.pay_image)).setImageResource(
                success ? R.drawable.astolfochibiblih : R.drawable.astolfoincorner);
        ((TextView) v.findViewById(R.id.pay_title)).setText(title);
        TextView amountView = v.findViewById(R.id.pay_amount);
        if (amount != null) amountView.setText(amount);
        else amountView.setVisibility(View.GONE);
        ((TextView) v.findViewById(R.id.pay_merchant)).setText(msg);
        new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setPositiveButton(success ? "Super ♡" : "OK", null)
                .show();
    }

    /** Translates the backend's English error messages for the transfer flows. */
    private static String friendlyError(String msg) {
        if (msg.contains("Target account not found")) return "Nie znaleziono konta odbiorcy";
        if (msg.contains("Target account is not active")) return "Konto odbiorcy jest nieaktywne";
        if (msg.contains("exceeds account balance") || msg.contains("Insufficient")) return "Za mało środków na koncie";
        if (msg.contains("Cross-currency") || msg.contains("Currency mismatch")) return "Konto odbiorcy ma inną walutę";
        if (msg.contains("Invalid or expired BLIH code")) return "Kod BLIH jest nieprawidłowy lub wygasł";
        if (msg.contains("BLIH profile is not active") || msg.contains("No active bank account")) return "Nadawca nie ma aktywnego BLIH";
        return msg;
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
        ((TextView) v.findViewById(R.id.profile_account_id)).setText(Session.accountId);
        v.findViewById(R.id.profile_account_id_card).setOnClickListener(x -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("ID do przelewów", Session.accountId));
            snack("Skopiowano ID do przelewów");
        });
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
