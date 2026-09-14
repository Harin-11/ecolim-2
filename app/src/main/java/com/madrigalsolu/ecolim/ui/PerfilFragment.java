package com.madrigalsolu.ecolim.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.madrigalsolu.ecolim.LoginActivity;
import com.madrigalsolu.ecolim.R;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;

/**
 * Fragment de Perfil de Operario (Tab 4).
 * Muestra información del usuario, estadísticas operativas y control de sesión.
 */
public class PerfilFragment extends Fragment {

    private TextView tvNombre, tvCorreo, tvDni, tvStatsResumen;
    private Button btnCerrarSesion;
    private ResiduoDBHelper dbHelper;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        tvNombre = view.findViewById(R.id.tv_nombre);
        tvCorreo = view.findViewById(R.id.tv_correo);
        tvDni = view.findViewById(R.id.tv_dni);
        tvStatsResumen = view.findViewById(R.id.tv_stats_resumen);
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion);

        dbHelper = new ResiduoDBHelper(requireContext());
        prefs = requireActivity().getSharedPreferences("ecolim_prefs", Context.MODE_PRIVATE);

        cargarDatosUsuario();
        actualizarEstadisticas();

        btnCerrarSesion.setOnClickListener(v -> confirmarCerrarSesion());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarEstadisticas();
    }

    private void cargarDatosUsuario() {
        String correo = prefs.getString("correo_usuario", "operario@ecolim.pe");
        String nombre = prefs.getString("nombre_usuario", "");
        if (nombre.isEmpty()) {
            nombre = dbHelper.obtenerNombreCompleto(correo);
        }
        String dni = prefs.getString("dni_usuario", "");
        if (dni.isEmpty()) {
            dni = dbHelper.obtenerDni(correo);
        }

        tvNombre.setText(nombre.isEmpty() ? "Operario Ecolim" : nombre);
        tvCorreo.setText(correo);
        tvDni.setText(dni.isEmpty() ? "DNI no registrado" : "DNI: " + dni);
    }

    private void actualizarEstadisticas() {
        if (!isAdded() || getContext() == null) return;
        int total = dbHelper.obtenerConteoTotal();
        double totalKg = dbHelper.obtenerTotalKg();

        tvStatsResumen.setText(String.format(java.util.Locale.getDefault(),
                "Total recolectado: %.2f kg\nRegistros realizados: %d",
                totalKg, total));
    }

    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Deseas cerrar tu sesión de operario en este dispositivo?")
                .setPositiveButton("Sí, Salir", (dialog, which) -> {
                    prefs.edit().clear().apply();
                    try {
                        FirebaseAuth.getInstance().signOut();
                    } catch (Exception ignored) {
                    }
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
