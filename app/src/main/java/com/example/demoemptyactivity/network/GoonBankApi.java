package com.example.demoemptyactivity.network;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GoonBankApi {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
