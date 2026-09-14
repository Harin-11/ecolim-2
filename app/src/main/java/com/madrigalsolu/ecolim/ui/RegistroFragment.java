package com.madrigalsolu.ecolim.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.madrigalsolu.ecolim.R;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;
import com.madrigalsolu.ecolim.model.Residuo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment de Registro de Recolección de Residuos (Tab 1).
 * Almacena en SQLite local (offline-first) e intenta sincronizar de inmediato con Firebase.
 */
public class RegistroFragment extends Fragment {

    private Spinner spinnerTipoResiduo, spinnerUbicacion;
    private EditText etCantidad, etObservaciones;
    private Button btnGuardar;
    private ResiduoDBHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);

        spinnerTipoResiduo = view.findViewById(R.id.spinner_tipo_residuo);
        spinnerUbicacion = view.findViewById(R.id.spinner_ubicacion);
        etCantidad = view.findViewById(R.id.et_cantidad);
        etObservaciones = view.findViewById(R.id.et_observaciones);
        btnGuardar = view.findViewById(R.id.btn_guardar);

        dbHelper = new ResiduoDBHelper(requireContext());

        String[] tipos = getResources().getStringArray(R.array.tipos_residuo);
        ArrayAdapter<String> adapterTipos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tipos);
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoResiduo.setAdapter(adapterTipos);

        String[] ubicaciones = getResources().getStringArray(R.array.ubicaciones_empresa);
        ArrayAdapter<String> adapterUbicacion = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, ubicaciones);
        adapterUbicacion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUbicacion.setAdapter(adapterUbicacion);

        btnGuardar.setOnClickListener(v -> guardarRegistro());

        return view;
    }

    private void guardarRegistro() {
        String tipo = spinnerTipoResiduo.getSelectedItem() != null
                ? spinnerTipoResiduo.getSelectedItem().toString() : "Orgánico";
        String cantidadTxt = etCantidad.getText().toString().trim();
        String ubicacion = spinnerUbicacion.getSelectedItem() != null
                ? spinnerUbicacion.getSelectedItem().toString() : "Planta Central - Módulo 1";
        String observaciones = etObservaciones.getText().toString().trim();

        if (TextUtils.isEmpty(cantidadTxt)) {
            etCantidad.setError("Ingresa la cantidad en kg");
            etCantidad.requestFocus();
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadTxt);
            if (cantidad <= 0) {
                etCantidad.setError("La cantidad debe ser mayor a 0");
                etCantidad.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etCantidad.setError("Cantidad inválida");
            etCantidad.requestFocus();
            return;
        }

        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date ahora = new Date();
        String fecha = sdfFecha.format(ahora);
        String hora = sdfHora.format(ahora);

        Residuo residuo = new Residuo();
        residuo.setTipoResiduo(tipo);
        residuo.setCantidadKg(cantidad);
        residuo.setUbicacion(ubicacion);
        residuo.setFecha(fecha);
        residuo.setHora(hora);
        residuo.setObservaciones(observaciones);
        residuo.setSincronizado(0);

        // Guardar primero en SQLite local (offline-first seguro)
        long idGenerado = dbHelper.insertarResiduo(residuo);
        if (idGenerado == -1) {
            Toast.makeText(requireContext(), "Error al guardar en base de datos local", Toast.LENGTH_SHORT).show();
            return;
        }
        residuo.setId(idGenerado);

        // Intentar sincronizar en la nube de fondo (Firebase)
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Registros");
            DatabaseReference pushRef = dbRef.push();
            String pushKey = pushRef.getKey();

            Map<String, Object> map = new HashMap<>();
            map.put("idLocal", idGenerado);
            map.put("tipoResiduo", tipo);
            map.put("cantidadKg", cantidad);
            map.put("ubicacion", ubicacion);
            map.put("fecha", fecha);
            map.put("hora", hora);
            map.put("observaciones", observaciones);
            map.put("sincronizado", 1);
            map.put("timestamp", System.currentTimeMillis());

            pushRef.setValue(map)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded() && getContext() != null) {
                            dbHelper.marcarComoSincronizado(idGenerado, pushKey);
                            Toast.makeText(requireContext(), "Registro guardado y sincronizado con Firebase", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "Guardado localmente en SQLite (sin conexión)", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Guardado localmente en SQLite", Toast.LENGTH_SHORT).show();
        }

        limpiarFormulario();
    }

    private void limpiarFormulario() {
        etCantidad.setText("");
        etObservaciones.setText("");
        if (spinnerTipoResiduo != null) {
            spinnerTipoResiduo.setSelection(0);
        }
        if (spinnerUbicacion != null) {
            spinnerUbicacion.setSelection(0);
        }
        etCantidad.clearFocus();
        etObservaciones.clearFocus();
    }
}
