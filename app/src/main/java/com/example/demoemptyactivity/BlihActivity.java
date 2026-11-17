package com.example.demoemptyactivity;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class BlihActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blih);

        Button btnWyjdz = findViewById(R.id.button4);
        if (btnWyjdz != null) {
            btnWyjdz.setOnClickListener(v -> finish());
        }
    }
}
