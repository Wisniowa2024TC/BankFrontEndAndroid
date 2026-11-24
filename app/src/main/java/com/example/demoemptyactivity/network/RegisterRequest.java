package com.example.demoemptyactivity.network;

public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private String dateOfBirth;
    private String currency;
    private int accountType;

    public RegisterRequest(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = password;
        this.dateOfBirth = "2000-01-01";
        this.currency = "PLN";
        this.accountType = 0;
    }
}
