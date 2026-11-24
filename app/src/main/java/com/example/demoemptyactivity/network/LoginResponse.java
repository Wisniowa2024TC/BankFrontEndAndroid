package com.example.demoemptyactivity.network;

public class LoginResponse {
    private String token;
    private String accountId;
    private String firstName;
    private String lastName;
    private double balance;
    private String email;
    private String accountNo;
    private String currency;

    public String getEmail() { return email; }
    public String getAccountNo() { return accountNo; }
    public String getCurrency() { return currency; }

    public String getToken() {
        return token;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public double getBalance() {
        return balance;
    }
}
