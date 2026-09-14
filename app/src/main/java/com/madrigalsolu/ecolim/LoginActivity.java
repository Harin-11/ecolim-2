package com.madrigalsolu.ecolim;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;

/**
 * Pantalla de Inicio de Sesión de Operario.
 * Utiliza las convenciones de IDs de Gestion502: etcorreo, etpassword, btn_ingresar, txt_registrar.
 * Compatible tanto con Firebase Auth (online) como con SQLite local (offline).
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etcorreo, etpassword;
    private Button btn_ingresar;
    private TextView txt_registrar;

    private FirebaseAuth firebaseAuth;
    private ResiduoDBHelper dbHelper;
    private SharedPreferences prefs;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etcorreo = findViewById(R.id.etcorreo);
        etpassword = findViewById(R.id.etpassword);
        btn_ingresar = findViewById(R.id.btn_ingresar);
        txt_registrar = findViewById(R.id.txt_registrar);

        firebaseAuth = FirebaseAuth.getInstance();
        dbHelper = new ResiduoDBHelper(this);
        prefs = getSharedPreferences("ecolim_prefs", MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Iniciando sesión");
        progressDialog.setMessage("Validando credenciales...");
        progressDialog.setCancelable(false);

        txt_registrar.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
        });

        btn_ingresar.setOnClickListener(v -> validarDatos());
    }

    private void validarDatos() {
        String correo = etcorreo.getText().toString().trim();
        String password = etpassword.getText().toString().trim();

        if (TextUtils.isEmpty(correo)) {
            etcorreo.setError("Ingrese su correo electrónico");
            etcorreo.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etcorreo.setError("Ingrese un correo válido");
            etcorreo.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etpassword.setError("Ingrese su contraseña");
            etpassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etpassword.setError("La contraseña debe tener al menos 6 caracteres");
            etpassword.requestFocus();
            return;
        }

        iniciarSesion(correo, password);
    }

    private void iniciarSesion(String correo, String password) {
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(correo, password)
                .addOnSuccessListener(authResult -> {
                    progressDialog.dismiss();
                    guardarSesion(correo);
                    Toast.makeText(LoginActivity.this, "Bienvenido a ECOLIM", Toast.LENGTH_SHORT).show();
                    navegarAlMain();
                })
                .addOnFailureListener(e -> {
                    // Si falla Firebase (por ejemplo, sin conexión a internet), validar en SQLite local
                    if (dbHelper.validarCredenciales(correo, password)) {
                        progressDialog.dismiss();
                        guardarSesion(correo);
                        Toast.makeText(LoginActivity.this, "Sesión iniciada en modo local (sin conexión)", Toast.LENGTH_SHORT).show();
                        navegarAlMain();
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Credenciales incorrectas o usuario no registrado", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarSesion(String correo) {
        String nombre = dbHelper.obtenerNombreCompleto(correo);
        String dni = dbHelper.obtenerDni(correo);

        prefs.edit()
                .putBoolean("sesion_iniciada", true)
                .putString("correo_usuario", correo)
                .putString("nombre_usuario", nombre)
                .putString("dni_usuario", dni)
                .apply();
    }

    private void navegarAlMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}
