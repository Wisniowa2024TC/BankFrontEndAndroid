package com.example.demoemptyactivity.network;

/** Response of GET api/Blih/payment/{id}/status: "Pending", "Approved" or "Failed". */
public class BlihStatusResponse {
    public static final String PENDING = "Pending", APPROVED = "Approved", FAILED = "Failed";

    private String status;

    public String getStatus() { return status; }
}
