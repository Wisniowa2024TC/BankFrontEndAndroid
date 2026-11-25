package com.example.demoemptyactivity.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // School server (Caddy HTTPS in front of the API, CA trusted via network_security_config).
    // For a local backend use e.g. http://10.0.2.2:5001/ from the emulator; changeable at runtime
    // via the "Serwer" button on the login screen.
    public static final String DEFAULT_BASE_URL = "https://192.168.0.10/";
    private static final String PREFS = "goonbank";
    private static final String KEY_URL = "base_url";

    private static GoonBankApi apiService;
    private static String baseUrl;

    public static void init(Context ctx) {
        if (baseUrl == null) {
            baseUrl = prefs(ctx).getString(KEY_URL, DEFAULT_BASE_URL);
        }
    }

    public static String getBaseUrl() {
        return baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
    }

    public static void setBaseUrl(Context ctx, String url) {
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        if (!url.endsWith("/")) url += "/";
        baseUrl = url;
        apiService = null;
        prefs(ctx).edit().putString(KEY_URL, url).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized GoonBankApi getApiService() {
        if (apiService == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiService = retrofit.create(GoonBankApi.class);
        }
        return apiService;
    }

    /** Extracts {"message": "..."} from an error body, or falls back to HTTP code. */
    public static String errorMessage(retrofit2.Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                if (obj.has("message")) return obj.get("message").getAsString();
                if (obj.has("title")) return obj.get("title").getAsString();
            }
        } catch (Exception ignored) {}
        return "Błąd serwera (" + response.code() + ")";
    }
}
