package com.example.demoemptyactivity.network;

public class CreateBlihProfileRequest {
    private final String accountId;
    private final String phoneHash;

    public CreateBlihProfileRequest(String accountId, String phoneHash) {
        this.accountId = accountId;
        this.phoneHash = phoneHash;
    }
}
