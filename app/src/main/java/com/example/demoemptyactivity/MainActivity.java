package com.example.demoemptyactivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class MainActivity extends AppCompatActivity {
    EditText poleZLoginem;
    EditText haslo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        haslo = findViewById(R.id.haslo);
        poleZLoginem = findViewById(R.id.user);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Myuser u1 = new Myuser("koi","1234");
        Myuser u2 = new Myuser("kostek","25000");
        Myuser u3 = new Myuser("Artem","31231!k");
    }
    public void login(View b1){
        String user = poleZLoginem.getText().toString();
        String p = haslo.getText().toString();
        Myuser[] users = {};
    }
}