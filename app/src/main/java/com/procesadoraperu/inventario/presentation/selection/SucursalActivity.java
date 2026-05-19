package com.procesadoraperu.inventario.presentation.selection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class SucursalActivity extends AppCompatActivity {

    private SelectionViewModel viewModel;
    private SucursalAdapter adapter;

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
        RecyclerView recyclerView = findViewById(R.id.recyclerViewOpciones);

        tvTitulo.setText("Seleccionar Sucursal");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SucursalAdapter(sucursal -> {
            viewModel.guardarSucursalSeleccionada(sucursal);
            startActivity(new Intent(this, AlmacenActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        recyclerView.setAdapter(adapter);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filtrarSucursales(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(SelectionViewModel.class);

        viewModel.getSucursales().observe(this, sucursales -> adapter.setList(sucursales));
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        viewModel.cargarSucursales();
    }
}