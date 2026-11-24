package com.example.demoemptyactivity.network;

public class BlihPaymentRequest {
    private String blihCode;
    private String merchantId;
    private double amount;

    public BlihPaymentRequest(String blihCode, String merchantId, double amount) {
        this.blihCode = blihCode;
        this.merchantId = merchantId;
        this.amount = amount;
    }

    public String getBlihCode() { return blihCode; }
    public String getMerchantId() { return merchantId; }
    public double getAmount() { return amount; }
}
