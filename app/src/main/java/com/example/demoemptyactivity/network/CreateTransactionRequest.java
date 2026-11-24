package com.example.demoemptyactivity.network;

public class CreateTransactionRequest {
    public static final int TYPE_TRANSFER = 0, TYPE_WITHDRAWAL = 1, TYPE_DEPOSIT = 2, TYPE_BLIH = 3;

    private final String sourceAccountId;
    private final String targetAccountId;
    private final double amount;
    private final int transactionType;
    private final String description;

    public CreateTransactionRequest(String sourceAccountId, String targetAccountId, double amount, int transactionType, String description) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
    }
}
