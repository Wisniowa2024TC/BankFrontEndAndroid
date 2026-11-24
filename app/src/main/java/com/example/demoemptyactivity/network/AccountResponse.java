package com.example.demoemptyactivity.network;

public class AccountResponse {
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
}
