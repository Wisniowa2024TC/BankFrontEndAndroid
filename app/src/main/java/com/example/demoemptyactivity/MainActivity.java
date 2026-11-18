
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

import com.example.demoemptyactivity.network.ApiClient;
import com.example.demoemptyactivity.network.LoginRequest;
import com.example.demoemptyactivity.network.LoginResponse;

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

    public void newuser(View view) {
        Toast.makeText(MainActivity.this, "Rejestracja w trakcie implementacji...", Toast.LENGTH_SHORT).show();
    }
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

        // Call the backend via retrofit
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
                // If server not reachable
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
