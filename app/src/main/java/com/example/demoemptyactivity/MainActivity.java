package com.example.demoemptyactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.demoemptyactivity.network.AccountResponse;
import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.LoginRequest;
import com.example.demoemptyactivity.network.LoginResponse;
import com.example.demoemptyactivity.network.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    EditText poleZLoginem;
    EditText haslo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        poleZLoginem = findViewById(R.id.user);
        haslo = findViewById(R.id.haslo);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    public void rejestracja(View view) {
        setContentView(R.layout.rejestr);
    }

    // REAL BACKEND REGISTRATION (POST /api/Account)
    public void newuser(View view) {
        EditText etUsername = findViewById(R.id.username);
        EditText etPassword = findViewById(R.id.password);

        if (etUsername == null || etPassword == null) return;

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Podaj username i hasło!", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = username.contains("@") ? username : username.toLowerCase() + "@goonbank.pl";
        RegisterRequest request = new RegisterRequest(username, "Klient", email, password);

        ApiClient.getApiService().register(request).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Konto utworzone! Zaloguj się.", Toast.LENGTH_SHORT).show();
                    setContentView(R.layout.activity_main);
                    poleZLoginem = findViewById(R.id.user);
                    haslo = findViewById(R.id.haslo);
                    if (poleZLoginem != null) {
                        poleZLoginem.setText(email);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Błąd rejestracji (Kod: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // REAL BACKEND LOGIN (POST /api/auth/login)
    public void login(View view) {
        String email = poleZLoginem.getText().toString().trim();
        String password = haslo.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Wypełnij wszystkie pola",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        LoginRequest request = new LoginRequest(email, password);
        ApiClient.getApiService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse auth = response.body();

                    Toast.makeText(MainActivity.this, "Witaj " + auth.getFirstName(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, BankMainActivity.class);
                    intent.putExtra("TOKEN", auth.getToken());
                    intent.putExtra("ACCOUNT_ID", auth.getAccountId());
                    intent.putExtra("NAME", auth.getFirstName() + " " + auth.getLastName());
                    intent.putExtra("BALANCE", auth.getBalance());
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Błąd logowania. (Kod: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Błąd połączenia z serwerem: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void backToLogin(View view) {
        setContentView(R.layout.activity_main);
        poleZLoginem = findViewById(R.id.user);
        haslo = findViewById(R.id.haslo);
    }
}
