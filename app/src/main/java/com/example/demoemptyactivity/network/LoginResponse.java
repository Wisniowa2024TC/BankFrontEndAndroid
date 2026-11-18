package com.example.demoemptyactivity.network;

public class LoginResponse {
    private String token;
    private String accountId;
    private String firstName;
    private String lastName;
    private double balance;

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
