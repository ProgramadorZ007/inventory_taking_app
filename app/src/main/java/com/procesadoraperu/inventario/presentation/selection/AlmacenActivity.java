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
import com.procesadoraperu.inventario.presentation.home.HomeActivity;

public class AlmacenActivity extends AppCompatActivity {

    private SelectionViewModel viewModel;
    private AlmacenAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion);

        TextView tvTitulo = findViewById(R.id.tvTituloSeleccion);
        EditText etBuscar = findViewById(R.id.etBuscar);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewOpciones);

        tvTitulo.setText("Seleccionar Almacén");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlmacenAdapter(almacen -> {
            // Al hacer clic, guardamos el almacén y vamos al HOME
            viewModel.guardarAlmacenSeleccionado(almacen);
            Intent intent = new Intent(this, HomeActivity.class);
            // Limpiamos el historial de pantallas para que no pueda retroceder al login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(SelectionViewModel.class);

        viewModel.getAlmacenes().observe(this, almacenes -> {
            adapter.setList(almacenes);
        });

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filtrarAlmacenes(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // AVISO: Aquí debes pasar el ID de la sucursal activa.
        // Por ahora lo puedes sacar de SharedPreferences directamente o pasarlo por Intent.
        String idSucursalActiva = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("ACTIVE_SUCURSAL_ID", "");
        viewModel.cargarAlmacenes(idSucursalActiva);
    }
}