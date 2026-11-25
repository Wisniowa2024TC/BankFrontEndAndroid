package com.example.demoemptyactivity.network;

/** Response of POST api/Blih/payment/initiate. */
public class BlihInitiateResponse {
    private String paymentId;
    private String message;

    public String getPaymentId() { return paymentId; }
    public String getMessage() { return message; }
}
