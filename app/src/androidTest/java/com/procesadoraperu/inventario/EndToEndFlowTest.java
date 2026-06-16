package com.procesadoraperu.inventario;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static org.hamcrest.Matchers.containsString;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.procesadoraperu.inventario.core.network.ApiClient;
import com.procesadoraperu.inventario.presentation.auth.LoginActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

@RunWith(AndroidJUnit4.class)
public class EndToEndFlowTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setup() {
        ApiClient.mockInterceptor = chain -> {
            String method = chain.request().method();
            String path = chain.request().url().encodedPath();

            // Si es una petición para registrar/enviar algo (ej. POST de inventarios),
            // simulamos una respuesta exitosa para no alterar la base de datos real.
            // PERO dejamos pasar peticiones reales a 'login' y 'producto-stock'.
            if (method.equalsIgnoreCase("POST") && !path.contains("login") && !path.contains("producto-stock")) {
                return new Response.Builder()
                        .code(200)
                        .message("OK")
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create("{\"exito\":true, \"mensaje\":\"OK\"}", MediaType.parse("application/json")))
                        .build();
            }

            // Dejamos pasar todo lo demás: Login, Obtener Sucursales, Obtener Almacenes, etc.
            return chain.proceed(chain.request());
        };
        ApiClient.reset();
    }

    @After
    public void teardown() {
        ApiClient.mockInterceptor = null;
        ApiClient.reset();
    }

    @Test
    public void testFlujoCompletoDeUsuario() throws InterruptedException {
        onView(withId(R.id.etUsername)).perform(replaceText("USAT"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(replaceText("95492129"), closeSoftKeyboard());
        
        onView(withId(R.id.btnLogin)).perform(click());

        // Aumentamos el tiempo a 8 segundos. Las APIs reales pueden tardar
        // en responder dependiendo del internet del emulador.
        Thread.sleep(8000);

        onView(withText("Seleccionar Sucursal")).check(matches(isDisplayed()));
        
        Thread.sleep(2000);

        // Buscar y hacer clic en la sucursal "001"
        onView(withId(R.id.recyclerViewOpciones))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(containsString("001"))), click()));

        Thread.sleep(2000);
        
        onView(withText("Seleccionar Almacén")).check(matches(isDisplayed()));

        // Buscar y hacer clic en el almacén "002"
        onView(withId(R.id.recyclerViewOpciones))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(containsString("002"))), click()));

        // --- 4. HOME (MENÚ PRINCIPAL) ---
        Thread.sleep(1500);

        onView(withId(R.id.btnRealizarToma)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRealizarToma)).perform(click());

        // --- 5. TOMA DE INVENTARIO ---
        Thread.sleep(1500);

        // Interactuar con el formulario final (buscar producto)
        onView(withId(R.id.etCodigoManual)).perform(replaceText("252600200007"), closeSoftKeyboard());
        onView(withId(R.id.btnBuscarManual)).perform(click());

        Thread.sleep(1500);

        // Ingresar cantidad contada
        onView(withId(R.id.etCantidadContada)).perform(replaceText("50"), closeSoftKeyboard());
        
        // Simular clic en registrar (nuestro interceptor bloqueará el envío real)
        onView(withId(R.id.btnRegistrar)).perform(click());

        Thread.sleep(2000);
    }
}
