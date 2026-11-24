package com.example.demoemptyactivity.network;

/** Response of GET api/Blih/payment/pending/{accountId}. */
public class BlihPaymentResponse {
    private String blihPaymentId;
    private String merchantId;
    private double amount;
    private String initiatedAt;

    public String getPaymentId() { return blihPaymentId; }
    public String getMerchantId() { return merchantId; }
    public double getAmount() { return amount; }
    public String getInitiatedAt() { return initiatedAt; }
}
