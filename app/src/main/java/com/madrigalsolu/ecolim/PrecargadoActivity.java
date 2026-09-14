package com.madrigalsolu.ecolim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Pantalla de Bienvenida / Splash Screen.
 * Verifica la sesión del operario y redirige a MainActivity o LoginActivity.
 */
public class PrecargadoActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navegarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            SharedPreferences prefs = getSharedPreferences("ecolim_prefs", MODE_PRIVATE);
            boolean sesionIniciada = prefs.getBoolean("sesion_iniciada", false);
            boolean sesionFirebase = false;
            try {
                sesionFirebase = FirebaseAuth.getInstance().getCurrentUser() != null;
            } catch (Exception ignored) {
            }

            if (sesionIniciada || sesionFirebase) {
                startActivity(new Intent(PrecargadoActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(PrecargadoActivity.this, LoginActivity.class));
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_precargado);

        android.view.View layoutCenter = findViewById(R.id.layoutCenter);
        if (layoutCenter != null) {
            layoutCenter.setAlpha(0f);
            layoutCenter.animate().alpha(1f).setDuration(450).start();
        }

        handler.postDelayed(navegarRunnable, 1800);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navegarRunnable);
        super.onDestroy();
    }
}
