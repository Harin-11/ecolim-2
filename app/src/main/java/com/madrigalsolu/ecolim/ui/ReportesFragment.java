package com.madrigalsolu.ecolim.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.madrigalsolu.ecolim.R;
import com.madrigalsolu.ecolim.adapter.ResiduoAdapter;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;
import com.madrigalsolu.ecolim.model.Residuo;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment de Reportes y Estadísticas (Tab 3).
 * Filtra por tipo de residuo y rango de fechas, calculando volumen acumulado en kg.
 */
public class ReportesFragment extends Fragment {

    private Spinner spinnerTipo;
    private EditText etDesde, etHasta;
    private Button btnFiltrar, btnCompartir;
    private TextView tvResumen;
    private RecyclerView recyclerReportes;
    private ResiduoDBHelper dbHelper;

    private List<Residuo> ultimoResultado;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        spinnerTipo = view.findViewById(R.id.spinner_filtro_tipo);
        etDesde = view.findViewById(R.id.et_fecha_desde);
        etHasta = view.findViewById(R.id.et_fecha_hasta);
        btnFiltrar = view.findViewById(R.id.btn_filtrar);
        btnCompartir = view.findViewById(R.id.btn_compartir);
        tvResumen = view.findViewById(R.id.tv_resumen);
        recyclerReportes = view.findViewById(R.id.recycler_reportes);
        recyclerReportes.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbHelper = new ResiduoDBHelper(requireContext());

        String[] tiposFiltro = getResources().getStringArray(R.array.tipos_residuo_filtro);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tiposFiltro);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);

        etDesde.setOnClickListener(v -> mostrarDatePicker(etDesde));
        etHasta.setOnClickListener(v -> mostrarDatePicker(etHasta));

        btnFiltrar.setOnClickListener(v -> generarReporte());
        btnCompartir.setOnClickListener(v -> compartirReporte());

        generarReporte();
        return view;
    }

    private void mostrarDatePicker(EditText destino) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> destino.setText(
                        String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void generarReporte() {
        if (!isAdded() || getContext() == null) return;
        String tipo = spinnerTipo.getSelectedItem() != null ? spinnerTipo.getSelectedItem().toString() : "Todos";
        String desde = etDesde.getText().toString().trim();
        String hasta = etHasta.getText().toString().trim();

        ultimoResultado = dbHelper.listarConFiltro(tipo, desde, hasta);

        double totalKg = 0;
        for (Residuo r : ultimoResultado) {
            totalKg += r.getCantidadKg();
        }

        tvResumen.setText(String.format(Locale.getDefault(),
                "Registros: %d   |   Volumen total: %.2f kg", ultimoResultado.size(), totalKg));

        ResiduoAdapter reportAdapter = new ResiduoAdapter(ultimoResultado, residuo -> {
            Intent intent = new Intent(requireContext(), DetalleResiduoActivity.class);
            intent.putExtra(DetalleResiduoActivity.EXTRA_RESIDUO, residuo);
            startActivity(intent);
        });
        recyclerReportes.setAdapter(reportAdapter);
    }

    private void compartirReporte() {
        if (ultimoResultado == null || ultimoResultado.isEmpty()) {
            Toast.makeText(requireContext(), "No hay datos para compartir en este filtro", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalKg = 0;
        for (Residuo r : ultimoResultado) {
            totalKg += r.getCantidadKg();
        }

        String tipo = spinnerTipo.getSelectedItem() != null ? spinnerTipo.getSelectedItem().toString() : "Todos";
        StringBuilder sb = new StringBuilder();
        sb.append("*REPORTE OPERATIVO ECOLIM S.A.C.* \n");
        sb.append("Filtro: ").append(tipo).append("\n");
        sb.append("Total de recolecciones: ").append(ultimoResultado.size()).append("\n");
        sb.append("Volumen acumulado: ").append(String.format(Locale.getDefault(), "%.2f kg", totalKg)).append("\n\n");
        sb.append("Generado según NTP 900.058:2019.");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporte Ecolim");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Compartir reporte vía"));
    }
}
