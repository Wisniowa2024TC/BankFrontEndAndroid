package com.example.demoemptyactivity.network;

public class TransactionResponse {
    private String id;
    private String title;
    private String description;
    private double amount;
    private String date;
    private boolean isIncoming;

    public TransactionResponse(String id, String title, String description, double amount, String date, boolean isIncoming) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.isIncoming = isIncoming;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public boolean isIncoming() { return isIncoming; }
}
