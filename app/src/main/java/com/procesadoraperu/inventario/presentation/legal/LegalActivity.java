package com.procesadoraperu.inventario.presentation.legal;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.procesadoraperu.inventario.R;

/**
 * LegalActivity — Muestra Términos & Condiciones o Política de Privacidad.
 * Se reutiliza la misma Activity cambiando el tipo vía Intent extra.
 */
public class LegalActivity extends AppCompatActivity {

    public static final String EXTRA_TIPO      = "legal_tipo";
    public static final int    TIPO_TERMINOS   = 1;
    public static final int    TIPO_PRIVACIDAD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tvTitulo    = findViewById(R.id.tvLegalTitulo);
        TextView tvContenido = findViewById(R.id.tvLegalContenido);

        int tipo = getIntent().getIntExtra(EXTRA_TIPO, TIPO_TERMINOS);

        if (tipo == TIPO_PRIVACIDAD) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Política de Privacidad");
            tvTitulo.setText("Política de Privacidad");
            tvContenido.setText(getTextPrivacidad());
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Términos y Condiciones");
            tvTitulo.setText("Términos y Condiciones");
            tvContenido.setText(getTextTerminos());
        }
    }

    private String getTextTerminos() {
        return
                "TÉRMINOS Y CONDICIONES DE USO\n" +
                        "Última actualización: Mayo 2026\n\n" +
                        "1. ACEPTACIÓN DE LOS TÉRMINOS\n" +
                        "Al usar esta aplicación, usted acepta estos términos. Si no está de acuerdo, no utilice la aplicación.\n\n" +
                        "2. USO DE LA APLICACIÓN\n" +
                        "Esta aplicación es de uso exclusivo para operarios autorizados de Procesadora Perú S.A.C. para el registro y control de inventarios en almacenes asignados.\n\n" +
                        "3. CREDENCIALES DE ACCESO\n" +
                        "Cada usuario es responsable de mantener la confidencialidad de sus credenciales de acceso. No está permitido compartir cuentas entre operarios.\n\n" +
                        "4. DATOS REGISTRADOS\n" +
                        "Los registros de inventario realizados a través de esta aplicación constituyen información oficial de la empresa. El usuario asume responsabilidad por la exactitud de los datos ingresados.\n\n" +
                        "5. PROPIEDAD INTELECTUAL\n" +
                        "Esta aplicación y todo su contenido son propiedad de Procesadora Perú S.A.C. Queda prohibida su reproducción o distribución sin autorización expresa.\n\n" +
                        "6. LIMITACIÓN DE RESPONSABILIDAD\n" +
                        "Procesadora Perú S.A.C. no se responsabiliza por interrupciones del servicio debidas a fallas de conectividad, mantenimiento del servidor o eventos de fuerza mayor.\n\n" +
                        "7. MODIFICACIONES\n" +
                        "Nos reservamos el derecho de modificar estos términos en cualquier momento. Los cambios entran en vigor al ser publicados en la aplicación.\n\n" +
                        "8. CONTACTO\n" +
                        "Para consultas: sistemas@procesadoraperu.com";
    }

    private String getTextPrivacidad() {
        return
                "POLÍTICA DE PRIVACIDAD\n" +
                        "Última actualización: Mayo 2026\n\n" +
                        "1. INFORMACIÓN QUE RECOPILAMOS\n" +
                        "Para el correcto funcionamiento de la aplicación, recopilamos:\n" +
                        "• Datos de ubicación GPS (latitud y longitud) al registrar inventarios.\n" +
                        "• Dirección IP del dispositivo en la red local.\n" +
                        "• Modelo y fabricante del dispositivo.\n" +
                        "• Nombre de usuario y registros de inventario realizados.\n\n" +
                        "2. USO DE LA INFORMACIÓN\n" +
                        "Los datos recopilados se utilizan exclusivamente para:\n" +
                        "• Identificar el lugar físico donde se realizó el conteo de inventario.\n" +
                        "• Garantizar la trazabilidad y auditoría de las operaciones.\n" +
                        "• Mejorar la seguridad del sistema.\n\n" +
                        "3. ALMACENAMIENTO\n" +
                        "Los datos se almacenan en los servidores de Procesadora Perú S.A.C. y en la base de datos local del dispositivo (SQLite). Los datos locales se sincronizan con el servidor cuando hay conexión.\n\n" +
                        "4. UBICACIÓN GPS\n" +
                        "La aplicación solicita permiso de ubicación para registrar las coordenadas GPS en cada toma de inventario. Este dato es parte del registro de auditoría y no se usa para ningún otro fin.\n\n" +
                        "5. ACCESO A LOS DATOS\n" +
                        "Solo el personal autorizado de Sistemas y Auditoría de Procesadora Perú S.A.C. tiene acceso a los datos registrados.\n\n" +
                        "6. RETENCIÓN DE DATOS\n" +
                        "Los registros de inventario se conservan según las políticas internas de la empresa. Los logs de integración locales se pueden limpiar desde la aplicación.\n\n" +
                        "7. DERECHOS DEL USUARIO\n" +
                        "Para solicitar acceso, corrección o eliminación de sus datos, contacte a: sistemas@procesadoraperu.com\n\n" +
                        "8. CAMBIOS EN ESTA POLÍTICA\n" +
                        "Notificaremos cualquier cambio significativo a través de la aplicación.\n\n" +
                        "9. CONTACTO\n" +
                        "Procesadora Perú S.A.C.\n" +
                        "sistemas@procesadoraperu.com";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}