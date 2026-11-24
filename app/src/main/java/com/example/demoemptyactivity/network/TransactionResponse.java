package com.example.demoemptyactivity.network;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Mirrors backend MoneyTransaction. */
public class TransactionResponse {
    public static final int STATUS_PENDING = 0, STATUS_COMPLETED = 1, STATUS_FAILED = 2;

    private String transactionId;
    private String sourceAccountId;
    private String targetAccountId;
    private double amount;
    private int type;
    private int status;
    private String title;
    private String createdAt;

    public String getId() { return transactionId; }
    public double getAmount() { return amount; }
    public int getType() { return type; }
    public int getStatus() { return status; }
    public String getTitle() { return title; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getTargetAccountId() { return targetAccountId; }

    /** Money coming in to {@code myAccountId}. */
    public boolean isIncoming(String myAccountId) {
        if (type == CreateTransactionRequest.TYPE_DEPOSIT) return true;
        return myAccountId != null && myAccountId.equals(targetAccountId) && !myAccountId.equals(sourceAccountId);
    }

    public String displayTitle(String myAccountId) {
        if (title != null && !title.trim().isEmpty()) return title.trim().replaceFirst("^BLIH Payment - ", "");
        switch (type) {
            case CreateTransactionRequest.TYPE_DEPOSIT: return "Wpłata";
            case CreateTransactionRequest.TYPE_WITHDRAWAL: return "Wypłata";
            case CreateTransactionRequest.TYPE_BLIH: return "Płatność BLIH";
            default: return isIncoming(myAccountId) ? "Przelew przychodzący" : "Przelew wychodzący";
        }
    }

    public String displaySubtitle(String myAccountId) {
        String kind;
        switch (type) {
            case CreateTransactionRequest.TYPE_DEPOSIT: kind = "Wpłata"; break;
            case CreateTransactionRequest.TYPE_WITHDRAWAL: kind = "Wypłata"; break;
            case CreateTransactionRequest.TYPE_BLIH: kind = "BLIH"; break;
            default: kind = isIncoming(myAccountId) ? "Przelew przychodzący" : "Przelew wychodzący";
        }
        if (status == STATUS_PENDING) kind += " • oczekuje";
        else if (status == STATUS_FAILED) kind += " • odrzucona";
        return kind;
    }

    public Date parsedDate() {
        if (createdAt == null) return null;
        String s = createdAt;
        // Trim fractional seconds to millis & strip zone marker; backend stores UTC.
        s = s.replace("Z", "");
        int plus = s.indexOf('+', 10);
        if (plus > 0) s = s.substring(0, plus);
        int dot = s.indexOf('.');
        if (dot > 0) {
            String frac = s.substring(dot + 1);
            frac = (frac + "000").substring(0, 3);
            s = s.substring(0, dot) + "." + frac;
        } else {
            s = s + ".000";
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            return in.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public String displayDate() {
        Date d = parsedDate();
        if (d == null) return createdAt != null ? createdAt : "";
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(d);
    }
}
