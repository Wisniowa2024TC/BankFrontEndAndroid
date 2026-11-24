package com.example.demoemptyactivity.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface GoonBankApi {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/logout")
    Call<Void> logout(@Header("Authorization") String token);

    @POST("api/Account")
    Call<AccountResponse> register(@Body RegisterRequest request);

    @GET("api/Account/{id}")
    Call<AccountResponse> getAccount(@Header("Authorization") String token, @Path("id") String accountId);

    @GET("api/Account/{id}/transactions")
    Call<List<TransactionResponse>> getTransactions(@Header("Authorization") String token, @Path("id") String accountId);

    @POST("api/Transaction")
    Call<TransactionResponse> createTransaction(@Header("Authorization") String token, @Body CreateTransactionRequest request);

    @PUT("api/Transaction/{id}/execute")
    Call<TransactionResponse> executeTransaction(@Header("Authorization") String token, @Path("id") String transactionId);

    @POST("api/BlihProfile")
    Call<Void> createBlihProfile(@Header("Authorization") String token, @Body CreateBlihProfileRequest request);

    @POST("api/Blih/code")
    Call<BlihCodeResponse> generateBlihCode(@Header("Authorization") String token, @Body BlihCodeRequest request);

    @GET("api/Blih/payment/pending/{accountId}")
    Call<BlihPaymentResponse> getPendingPayment(@Header("Authorization") String token, @Path("accountId") String accountId);

    @POST("api/Blih/payment/{id}/confirm")
    Call<Void> confirmPayment(@Header("Authorization") String token, @Path("id") String paymentId);

    @POST("api/Blih/payment/{id}/reject")
    Call<Void> rejectPayment(@Header("Authorization") String token, @Path("id") String paymentId);
}
