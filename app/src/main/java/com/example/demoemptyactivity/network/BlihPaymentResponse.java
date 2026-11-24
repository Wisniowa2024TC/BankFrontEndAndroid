package com.example.demoemptyactivity.network;

public class BlihPaymentResponse {
    private String paymentId;
    private String merchantId;
    private double amount;
    private String status;

    public String getPaymentId() { return paymentId; }
    public String getMerchantId() { return merchantId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
}
