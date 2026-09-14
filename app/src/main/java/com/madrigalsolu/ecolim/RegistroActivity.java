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
import com.google.firebase.database.FirebaseDatabase;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Pantalla de Registro de Operario.
 * Utiliza las convenciones de IDs de Gestion502: etnombres, etdni, etcorreo, etpassword, etconfirpassword, btn_registrar.
 * Registra en Firebase Auth + Realtime Database y almacena en SQLite local.
 */
public class RegistroActivity extends AppCompatActivity {

    private EditText etnombres, etdni, etcorreo, etpassword, etconfirpassword;
    private Button btn_registrar;
    private TextView txt_login;

    private FirebaseAuth firebaseAuth;
    private ResiduoDBHelper dbHelper;
    private SharedPreferences prefs;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etnombres = findViewById(R.id.etnombres);
        etdni = findViewById(R.id.etdni);
        etcorreo = findViewById(R.id.etcorreo);
        etpassword = findViewById(R.id.etpassword);
        etconfirpassword = findViewById(R.id.etconfirpassword);
        btn_registrar = findViewById(R.id.btn_registrar);
        txt_login = findViewById(R.id.txt_login);

        firebaseAuth = FirebaseAuth.getInstance();
        dbHelper = new ResiduoDBHelper(this);
        prefs = getSharedPreferences("ecolim_prefs", MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Registrando cuenta");
        progressDialog.setMessage("Creando perfil de operario...");
        progressDialog.setCancelable(false);

        txt_login.setOnClickListener(v -> finish());

        btn_registrar.setOnClickListener(v -> validarDatos());
    }

    private void validarDatos() {
        String nombres = etnombres.getText().toString().trim();
        String dni = etdni.getText().toString().trim();
        String correo = etcorreo.getText().toString().trim();
        String password = etpassword.getText().toString().trim();
        String confirmPassword = etconfirpassword.getText().toString().trim();

        if (TextUtils.isEmpty(nombres)) {
            etnombres.setError("Ingrese sus nombres");
            etnombres.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(dni) || dni.length() < 8) {
            etdni.setError("Ingrese un DNI válido de 8 dígitos");
            etdni.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(correo)) {
            etcorreo.setError("Ingrese su correo");
            etcorreo.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etcorreo.setError("Ingrese un correo válido");
            etcorreo.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etpassword.setError("La contraseña debe tener al menos 6 caracteres");
            etpassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etconfirpassword.setError("Las contraseñas no coinciden");
            etconfirpassword.requestFocus();
            return;
        }

        registrarUsuario(nombres, dni, correo, password);
    }

    private void registrarUsuario(String nombres, String dni, String correo, String password) {
        progressDialog.show();

        // 1. Guardar siempre en SQLite local (offline-first)
        dbHelper.registrarUsuario(nombres, "", dni, correo, password);

        // 2. Intentar crear en Firebase Auth
        firebaseAuth.createUserWithEmailAndPassword(correo, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null ? authResult.getUser().getUid() : "";

                    // Guardar datos adicionales en Firebase Realtime Database
                    if (!uid.isEmpty()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", uid);
                        userMap.put("nombres", nombres);
                        userMap.put("dni", dni);
                        userMap.put("correo", correo);
                        userMap.put("empresa", "ECOLIM S.A.C.");
                        userMap.put("timestamp", System.currentTimeMillis());

                        try {
                            FirebaseDatabase.getInstance().getReference("Usuarios")
                                    .child(uid).setValue(userMap);
                        } catch (Exception ignored) {
                        }
                    }

                    progressDialog.dismiss();
                    guardarSesion(correo, nombres, dni);
                    Toast.makeText(RegistroActivity.this, "Cuenta registrada exitosamente", Toast.LENGTH_SHORT).show();
                    navegarAlMain();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    // Si falló por red, permitir continuar en modo local
                    guardarSesion(correo, nombres, dni);
                    Toast.makeText(RegistroActivity.this, "Registro guardado localmente (sin conexión a internet)", Toast.LENGTH_LONG).show();
                    navegarAlMain();
                });
    }

    private void guardarSesion(String correo, String nombre, String dni) {
        prefs.edit()
                .putBoolean("sesion_iniciada", true)
                .putString("correo_usuario", correo)
                .putString("nombre_usuario", nombre)
                .putString("dni_usuario", dni)
                .apply();
    }

    private void navegarAlMain() {
        Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
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
