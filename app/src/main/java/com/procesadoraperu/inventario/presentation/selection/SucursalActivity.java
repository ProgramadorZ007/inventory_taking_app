package com.procesadoraperu.inventario.presentation.selection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;

public class SucursalActivity extends AppCompatActivity {

    private SelectionViewModel viewModel;
    private SucursalAdapter adapter; // Necesitarás crear este Adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion); // Usamos el layout unificado

        TextView tvTitulo = findViewById(R.id.tvTituloSeleccion);
        EditText etBuscar = findViewById(R.id.etBuscar);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewOpciones);

        tvTitulo.setText("Seleccionar Sucursal");

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SucursalAdapter(sucursal -> {
            // Al hacer clic, guardamos la sucursal y vamos a elegir el Almacén
            viewModel.guardarSucursalSeleccionada(sucursal);
            Intent intent = new Intent(this, AlmacenActivity.class);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Configurar ViewModel
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(SelectionViewModel.class);

        // Observar datos
        viewModel.getSucursales().observe(this, sucursales -> {
            adapter.setList(sucursales);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        // Configurar Buscador
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filtrarSucursales(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Cargar datos iniciales
        viewModel.cargarSucursales();
    }
}