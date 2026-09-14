package com.madrigalsolu.ecolim;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.madrigalsolu.ecolim.api.SyncManager;
import com.madrigalsolu.ecolim.ui.ListaResiduosFragment;
import com.madrigalsolu.ecolim.ui.PerfilFragment;
import com.madrigalsolu.ecolim.ui.RegistroFragment;
import com.madrigalsolu.ecolim.ui.ReportesFragment;

/**
 * Activity Principal Contenedora.
 * Administra la navegación nativa entre los 4 fragments principales mediante BottomNavigationView.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    private final Fragment fragmentRegistro = new RegistroFragment();
    private final Fragment fragmentLista = new ListaResiduosFragment();
    private final Fragment fragmentReportes = new ReportesFragment();
    private final Fragment fragmentPerfil = new PerfilFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Fragment inicial por defecto (Registrar)
        if (savedInstanceState == null) {
            cambiarFragment(fragmentRegistro, "Registrar Residuo");
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_registro) {
                cambiarFragment(fragmentRegistro, "Registrar Residuo");
                return true;
            } else if (id == R.id.nav_lista) {
                cambiarFragment(fragmentLista, "Historial de Recolección");
                return true;
            } else if (id == R.id.nav_reportes) {
                cambiarFragment(fragmentReportes, "Reportes");
                return true;
            } else if (id == R.id.nav_perfil) {
                cambiarFragment(fragmentPerfil, "Perfil de Operario");
                return true;
            }
            return false;
        });
    }

    private void cambiarFragment(Fragment fragment, String titulo) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titulo);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sincronización automática silenciosa en segundo plano
        sincronizarEnSegundoPlano();
    }

    private void sincronizarEnSegundoPlano() {
        SyncManager.sincronizarPendientes(this, new SyncManager.SyncCallback() {
            @Override
            public void onProgreso(int enviados, int total) {
            }

            @Override
            public void onFinalizado(int enviadosOk, int fallidos) {
                if (enviadosOk > 0) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof ListaResiduosFragment) {
                            ((ListaResiduosFragment) current).cargarDatos();
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            sincronizarAhora();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sincronizarAhora() {
        Toast.makeText(this, "Sincronizando con Firebase...", Toast.LENGTH_SHORT).show();
        SyncManager.sincronizarPendientes(this, new SyncManager.SyncCallback() {
            @Override
            public void onProgreso(int enviados, int total) {
            }

            @Override
            public void onFinalizado(int enviadosOk, int fallidos) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Sincronización finalizada: " + enviadosOk + " subidos, " + fallidos + " pendientes",
                            Toast.LENGTH_LONG).show();

                    // Notificar al fragment actual si es ListaResiduos o Perfil
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof ListaResiduosFragment) {
                        ((ListaResiduosFragment) current).cargarDatos();
                    }
                });
            }
        });
    }
}
