package com.example.demoemptyactivity.network;

/**
 * Body of POST api/Blih/payment/initiate. The backend treats {@code merchantId} as the payee and
 * credits it when it is an account ID, so passing our own account ID requests money from the
 * owner of {@code blihCode}; they still have to approve it in their BLIH screen.
 */
public class InitiateBlihPaymentRequest {
    private final String blihCode;
    private final String merchantId;
    private final double amount;

    public InitiateBlihPaymentRequest(String blihCode, String merchantId, double amount) {
        this.blihCode = blihCode;
        this.merchantId = merchantId;
        this.amount = amount;
    }
}
