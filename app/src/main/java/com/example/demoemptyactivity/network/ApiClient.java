package com.example.demoemptyactivity.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ApiClient {
    // 10.0.2.2 points to host from android emulator
    // TODO: CHANGE TO REAL IP
    private static final String BASE_URL = "http://10.0.2.2:5001/";
    private static GoonBankApi apiService;

    public static GoonBankApi getApiService() {
        if (apiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(GoonBankApi.class);
        }
        return apiService;
        }
}
