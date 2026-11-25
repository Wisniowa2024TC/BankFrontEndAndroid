package com.example.demoemptyactivity.network;

import java.util.regex.Pattern;

public class AccountResponse {
    // Account IDs are GUIDs (Guid.NewGuid().ToString() on the backend).
    private static final Pattern ACCOUNT_ID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private String accountId;
    private String email;
    private String firstName;
    private String lastName;
    private String accountNo;
    private String currency;
    private double balance;

    public String getAccountId() { return accountId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getAccountNo() { return accountNo; }
    public String getCurrency() { return currency; }
    public double getBalance() { return balance; }

    public static boolean isAccountId(String s) {
        return s != null && ACCOUNT_ID.matcher(s).matches();
    }
}
