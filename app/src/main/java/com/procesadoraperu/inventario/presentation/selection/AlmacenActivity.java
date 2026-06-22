package com.procesadoraperu.inventario.presentation.selection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.domain.model.DownloadResult;
import com.procesadoraperu.inventario.domain.model.almacen.Almacen;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.home.HomeActivity;

public class AlmacenActivity extends AppCompatActivity {

    private SelectionViewModel viewModel;
    private AlmacenAdapter adapter;
    private RecyclerView recyclerView;
    private FrameLayout downloadOverlay;

    // Almacén seleccionado para soporte de reintentos
    private Almacen lastSelectedAlmacen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion);

        // Manejo de Insets
        View mainContainer = findViewById(R.id.main_selection_container);
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        initViews();
        setupViewModel();
    }

    private void initViews() {
        TextView tvTitulo = findViewById(R.id.tvTituloSeleccion);
        EditText etBuscar = findViewById(R.id.etBuscar);
        recyclerView = findViewById(R.id.recyclerViewOpciones);
        downloadOverlay = findViewById(R.id.downloadOverlay);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarSeleccion);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitulo.setText("Seleccionar Almacén");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlmacenAdapter(almacen -> {
            lastSelectedAlmacen = almacen;
            viewModel.guardarAlmacenSeleccionado(almacen);
            viewModel.downloadCatalogAndNavigate(almacen.getIdSucursal(), almacen.getIdAlmacen());
        });
        recyclerView.setAdapter(adapter);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filtrarAlmacenes(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(SelectionViewModel.class);

        viewModel.getAlmacenes().observe(this, almacenes -> adapter.setList(almacenes));
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        // Observar estado de descarga
        viewModel.getIsDownloading().observe(this, isDownloading -> {
            if (isDownloading != null && isDownloading) {
                downloadOverlay.setVisibility(View.VISIBLE);
                recyclerView.setEnabled(false);
            } else {
                downloadOverlay.setVisibility(View.GONE);
                recyclerView.setEnabled(true);
            }
        });

        // Observar resultado de descarga
        viewModel.getDownloadResult().observe(this, result -> {
            if (result == null) return;

            switch (result.getStatus()) {
                case SUCCESS:
                    navigateToHome();
                    break;
                case ERROR:
                    showErrorDialog(result.getErrorMessage());
                    break;
                case EMPTY:
                    showEmptyDialog();
                    break;
            }
        });

        String idSucursalActiva = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("ACTIVE_SUCURSAL_ID", "");
        viewModel.cargarAlmacenes(idSucursalActiva);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Error de descarga")
                .setMessage(errorMessage != null ? errorMessage : "Ocurrió un error al descargar el catálogo")
                .setPositiveButton("Reintentar", (dialog, which) -> {
                    dialog.dismiss();
                    if (lastSelectedAlmacen != null) {
                        viewModel.downloadCatalogAndNavigate(
                                lastSelectedAlmacen.getIdSucursal(),
                                lastSelectedAlmacen.getIdAlmacen()
                        );
                    }
                })
                .setNegativeButton("Continuar sin catálogo", (dialog, which) -> {
                    dialog.dismiss();
                    navigateToHome();
                })
                .setCancelable(false)
                .show();
    }

    private void showEmptyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sin productos")
                .setMessage("No se encontraron productos para este almacén")
                .setPositiveButton("Continuar", (dialog, which) -> {
                    dialog.dismiss();
                    navigateToHome();
                })
                .setNegativeButton("Seleccionar otro", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
}
