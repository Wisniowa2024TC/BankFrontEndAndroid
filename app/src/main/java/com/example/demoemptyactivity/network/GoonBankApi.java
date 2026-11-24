package com.example.demoemptyactivity.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GoonBankApi {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/Account")
    Call<AccountResponse> register(@Body RegisterRequest request);

    @GET("api/Account/{id}")
    Call<AccountResponse> getAccount(@Header("Authorization") String token, @Path("id") String accountId);

    @POST("api/Blih/code")
    Call<BlihCodeResponse> generateBlihCode(@Header("Authorization") String token, @Body BlihCodeRequest request);
}
