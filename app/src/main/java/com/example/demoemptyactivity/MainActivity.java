
package com.example.demoemptyactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText poleZLoginem;
    EditText haslo;

    Myuser u1 = new Myuser("koi", "1234");
    Myuser u2 = new Myuser("kostek", "25000");
    Myuser u3 = new Myuser("Artem", "31231!k");

    ArrayList<Myuser> users = new ArrayList<>();

    {
        users.add(u1);
        users.add(u2);
        users.add(u3);
    }

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
        EditText user = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);

        String username = user.getText().toString().trim();
        String haslo = password.getText().toString();

        if (username.isEmpty() || haslo.isEmpty()) {
            Toast.makeText(
                    this,
                    "Podaj username i hasło!",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        for (Myuser u : users) {
            if (u.getUsername().equals(username)) {
                Toast.makeText(
                        this,
                        "Taki username już istnieje!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        Myuser userkolejny = new Myuser(username, haslo);
        users.add(userkolejny);

        int numer = users.size();

        Toast.makeText(
                this,
                "Utworzono użytkownika nr " + numer,
                Toast.LENGTH_SHORT
        ).show();

        setContentView(R.layout.activity_main);

        poleZLoginem = findViewById(R.id.user);
        this.haslo = findViewById(R.id.haslo);
    }

    public void login(View view) {
        EditText user = findViewById(R.id.user);
        EditText haslo = findViewById(R.id.haslo);

        String login = user.getText().toString().trim();
        String password = haslo.getText().toString();

        for (Myuser u : users) {
            if (u.getUsername().equals(login)
                    && u.getPassword().equals(password)) {

                setContentView(R.layout.zalogowany);
                return;
            }
        }

        Toast.makeText(
                this,
                "Zły login",
                Toast.LENGTH_SHORT
        ).show();
    }
}
