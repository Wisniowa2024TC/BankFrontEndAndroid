package com.example.demoemptyactivity;

import android.app.Activity;
import android.content.Intent;

import java.text.NumberFormat;
import java.util.Locale;

/** In-memory login session. Lost on process death -> user is sent back to login. */
public final class Session {
    public static String token;
    public static String accountId;
    public static String firstName = "";
    public static String lastName = "";
    public static String email = "";
    public static String accountNo = "";
    public static String currency = "PLN";
    public static double balance;

    private Session() {}

    public static boolean isLoggedIn() {
        return token != null && accountId != null;
    }

    public static String bearer() {
        return "Bearer " + token;
    }

    public static void clear() {
        token = null;
        accountId = null;
        firstName = lastName = email = accountNo = "";
        balance = 0;
    }

    /** Sends user to login, clearing the back stack. */
    public static void kickToLogin(Activity a, boolean expired) {
        clear();
        Intent i = new Intent(a, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (expired) i.putExtra("EXPIRED", true);
        a.startActivity(i);
        a.finish();
    }

    public static String money(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v) + " zł";
    }

    public static String formatAccountNo(String no) {
        if (no == null) return "";
        StringBuilder sb = new StringBuilder();
        // Polish NRB style: 2 + groups of 4
        for (int i = 0; i < no.length(); i++) {
            if (i == 2 || (i > 2 && (i - 2) % 4 == 0)) sb.append(' ');
            sb.append(no.charAt(i));
        }
        return sb.toString();
    }
}
