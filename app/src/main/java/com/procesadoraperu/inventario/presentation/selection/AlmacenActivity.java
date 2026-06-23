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

        String idSucursalActiva = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("ACTIVE_SUCURSAL_ID", null);
        if (idSucursalActiva == null || idSucursalActiva.isEmpty()) {
            Toast.makeText(this, "Selecciona una sucursal primero", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel.cargarAlmacenes(idSucursalActiva);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showErrorDialog(String errorMessage) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Inventario_Dialog)
                .setCancelable(false)
                .create();

        View view = getLayoutInflater().inflate(R.layout.dialog_download_error, null);

        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            tvMessage.setText(errorMessage);
        }

        view.findViewById(R.id.btnDialogRetry).setOnClickListener(v -> {
            dialog.dismiss();
            if (lastSelectedAlmacen != null) {
                viewModel.downloadCatalogAndNavigate(
                        lastSelectedAlmacen.getIdSucursal(),
                        lastSelectedAlmacen.getIdAlmacen()
                );
            }
        });

        view.findViewById(R.id.btnDialogContinue).setOnClickListener(v -> {
            dialog.dismiss();
            navigateToHome();
        });

        dialog.setView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void showEmptyDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Inventario_Dialog)
                .setCancelable(false)
                .create();

        View view = getLayoutInflater().inflate(R.layout.dialog_download_empty, null);

        view.findViewById(R.id.btnEmptyContinue).setOnClickListener(v -> {
            dialog.dismiss();
            navigateToHome();
        });

        view.findViewById(R.id.btnEmptySelectOther).setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.setView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }
}
