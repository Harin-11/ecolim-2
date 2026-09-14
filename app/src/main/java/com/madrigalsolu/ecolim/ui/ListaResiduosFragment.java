package com.madrigalsolu.ecolim.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.madrigalsolu.ecolim.R;
import com.madrigalsolu.ecolim.adapter.ResiduoAdapter;
import com.madrigalsolu.ecolim.db.ResiduoDBHelper;
import com.madrigalsolu.ecolim.model.Residuo;

import java.util.List;

/**
 * Fragment de Historial de Registros (Tab 2).
 * Lista todos los residuos registrados localmente y su estado de sincronización.
 */
public class ListaResiduosFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvVacio;
    private ResiduoDBHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista, container, false);
        recyclerView = view.findViewById(R.id.recycler_residuos);
        tvVacio = view.findViewById(R.id.tv_vacio);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbHelper = new ResiduoDBHelper(requireContext());
        cargarDatos();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarDatos();
    }

    public void cargarDatos() {
        if (!isAdded() || getContext() == null) return;
        List<Residuo> lista = dbHelper.listarTodos();
        if (lista.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvVacio.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            ResiduoAdapter adapter = new ResiduoAdapter(lista, residuo -> {
                Intent intent = new Intent(requireContext(), DetalleResiduoActivity.class);
                intent.putExtra(DetalleResiduoActivity.EXTRA_RESIDUO, residuo);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }
    }
}
