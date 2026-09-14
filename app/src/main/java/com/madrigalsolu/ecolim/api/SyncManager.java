package com.madrigalsolu.ecolim.api;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;
import com.madrigalsolu.ecolim.model.Residuo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestador de sincronización en la nube (Firebase Realtime Database).
 * Recorre los registros locales pendientes (sincronizado = 0) y los envía.
 * Si el envío es exitoso, marca el registro local como sincronizado.
 * Si no hay conexión o falla, el dato queda intacto en SQLite para el próximo reintento.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    public interface SyncCallback {
        void onProgreso(int enviados, int total);
        void onFinalizado(int enviadosOk, int fallidos);
    }

    public static void sincronizarPendientes(Context context, SyncCallback callback) {
        ResiduoDBHelper dbHelper = new ResiduoDBHelper(context);
        List<Residuo> pendientes = dbHelper.listarPendientesSincronizar();

        if (pendientes == null || pendientes.isEmpty()) {
            if (callback != null) {
                callback.onFinalizado(0, 0);
            }
            return;
        }

        DatabaseReference dbRef;
        try {
            dbRef = FirebaseDatabase.getInstance().getReference("Registros");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar Firebase Database", e);
            if (callback != null) {
                callback.onFinalizado(0, pendientes.size());
            }
            return;
        }

        final int total = pendientes.size();
        final int[] enviadosOk = {0};
        final int[] fallidos = {0};
        final int[] procesados = {0};

        for (Residuo residuo : pendientes) {
            DatabaseReference pushRef = dbRef.push();
            String key = pushRef.getKey();
            residuo.setFirebaseKey(key);

            Map<String, Object> map = new HashMap<>();
            map.put("idLocal", residuo.getId());
            map.put("tipoResiduo", residuo.getTipoResiduo());
            map.put("cantidadKg", residuo.getCantidadKg());
            map.put("ubicacion", residuo.getUbicacion());
            map.put("fecha", residuo.getFecha());
            map.put("hora", residuo.getHora());
            map.put("observaciones", residuo.getObservaciones());
            map.put("sincronizado", 1);
            map.put("timestamp", System.currentTimeMillis());

            pushRef.setValue(map)
                    .addOnSuccessListener(aVoid -> {
                        dbHelper.marcarComoSincronizado(residuo.getId(), key);
                        enviadosOk[0]++;
                        procesados[0]++;
                        if (callback != null) {
                            callback.onProgreso(procesados[0], total);
                            if (procesados[0] >= total) {
                                callback.onFinalizado(enviadosOk[0], fallidos[0]);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        fallidos[0]++;
                        procesados[0]++;
                        Log.w(TAG, "Fallo al sincronizar residuo id=" + residuo.getId(), e);
                        if (callback != null) {
                            callback.onProgreso(procesados[0], total);
                            if (procesados[0] >= total) {
                                callback.onFinalizado(enviadosOk[0], fallidos[0]);
                            }
                        }
                    });
        }
    }
}
