package com.madrigalsolu.ecolim.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.FirebaseDatabase;
import com.madrigalsolu.ecolim.R;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;
import com.madrigalsolu.ecolim.model.Residuo;

import java.util.Locale;

/**
 * Pantalla de Detalle y Eliminación de Residuo.
 */
public class DetalleResiduoActivity extends AppCompatActivity {

    public static final String EXTRA_RESIDUO = "extra_residuo";

    private Residuo residuo;
    private ResiduoDBHelper dbHelper;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new ResiduoDBHelper(this);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            residuo = getIntent().getSerializableExtra(EXTRA_RESIDUO, Residuo.class);
        } else {
            //noinspection deprecation
            residuo = (Residuo) getIntent().getSerializableExtra(EXTRA_RESIDUO);
        }

        TextView tvTipo = findViewById(R.id.tv_detalle_tipo);
        TextView tvCantidad = findViewById(R.id.tv_detalle_cantidad);
        TextView tvUbicacion = findViewById(R.id.tv_detalle_ubicacion);
        TextView tvFechaHora = findViewById(R.id.tv_detalle_fecha_hora);
        TextView tvObservaciones = findViewById(R.id.tv_detalle_observaciones);
        TextView tvEstado = findViewById(R.id.tv_detalle_estado);
        Button btnEliminar = findViewById(R.id.btn_eliminar);

        if (residuo != null) {
            tvTipo.setText(residuo.getTipoResiduo());
            tvCantidad.setText(String.format(Locale.getDefault(), "%.2f kg", residuo.getCantidadKg()));
            tvUbicacion.setText(residuo.getUbicacion());
            tvFechaHora.setText(residuo.getFecha() + " - " + residuo.getHora());
            tvObservaciones.setText(
                    residuo.getObservaciones() == null || residuo.getObservaciones().isEmpty()
                            ? "Sin observaciones adicionales." : residuo.getObservaciones());

            if (residuo.getSincronizado() == 1) {
                tvEstado.setText("Estado: Sincronizado con la nube");
                tvEstado.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.badge_synced));
            } else {
                tvEstado.setText("Estado: Pendiente de sincronización local");
                tvEstado.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.badge_pending));
            }
        }

        btnEliminar.setOnClickListener(v -> confirmarEliminar());
    }

    private void confirmarEliminar() {
        if (residuo == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Registro")
                .setMessage("¿Estás seguro de que deseas eliminar este registro de recolección?")
                .setPositiveButton("Sí, Eliminar", (dialog, which) -> {
                    // Eliminar de SQLite local
                    dbHelper.eliminarResiduo(residuo.getId());

                    // Eliminar de Firebase si tiene key
                    if (residuo.getFirebaseKey() != null && !residuo.getFirebaseKey().isEmpty()) {
                        try {
                            FirebaseDatabase.getInstance().getReference("Registros")
                                    .child(residuo.getFirebaseKey()).removeValue();
                        } catch (Exception ignored) {
                        }
                    }

                    Toast.makeText(this, "Registro eliminado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
