package com.procesadoraperu.inventario.presentation.profile;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.procesadoraperu.inventario.R;
import com.procesadoraperu.inventario.presentation.ViewModelFactory;
import com.procesadoraperu.inventario.presentation.selection.SucursalActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private UserProfileViewModel viewModel;

    private TextView tvNombreCompleto, tvUsername, tvCodigo;
    private TextView tvSucursal, tvAlmacen, tvIdAlmacen;
    private TextView tvUltimoAcceso, tvDispositivo, tvAndroidVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Manejo de Insets para pantalla completa
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.appbar).setPadding(0, systemBars.top, 0, 0);
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Vistas
        tvNombreCompleto = findViewById(R.id.tvNombreCompleto);
        tvUsername       = findViewById(R.id.tvUsername);
        tvCodigo         = findViewById(R.id.tvCodigo);
        tvSucursal       = findViewById(R.id.tvSucursal);
        tvAlmacen        = findViewById(R.id.tvAlmacen);
        tvIdAlmacen      = findViewById(R.id.tvIdAlmacen);
        tvUltimoAcceso   = findViewById(R.id.tvUltimoAcceso);
        tvDispositivo    = findViewById(R.id.tvDispositivo);
        tvAndroidVersion = findViewById(R.id.tvAndroidVersion);

        // Información del Dispositivo
        String deviceBrand = Build.MANUFACTURER;
        String deviceModel = Build.MODEL;
        tvDispositivo.setText(capitalize(deviceBrand) + " " + deviceModel);
        tvAndroidVersion.setText("Android " + Build.VERSION.RELEASE);

        // Fecha de acceso simulada
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy\nHH:mm a", Locale.getDefault());
        tvUltimoAcceso.setText(sdf.format(new Date()));

        // Botón cambiar ubicación
        findViewById(R.id.btnCambiarUbicacion).setOnClickListener(v -> {
            Intent intent = new Intent(this, SucursalActivity.class);
            intent.putExtra("CAMBIO_UBICACION", true);
            startActivity(intent);
            finish();
        });

        // ViewModel
        ViewModelFactory factory = new ViewModelFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(UserProfileViewModel.class);

        viewModel.getProfileData().observe(this, data -> {
            tvNombreCompleto.setText(data.nombres.toUpperCase());
            tvUsername.setText("@" + data.username);
            tvCodigo.setText(data.idCodigoGeneral);
            tvSucursal.setText(data.sucursal);
            tvAlmacen.setText(data.almacen);
            tvIdAlmacen.setText("Cód: " + data.idAlmacen);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        viewModel.cargarPerfil();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}