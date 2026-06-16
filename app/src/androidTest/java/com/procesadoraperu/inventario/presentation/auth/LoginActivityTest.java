package com.procesadoraperu.inventario.presentation.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.procesadoraperu.inventario.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    // Regla que levanta la actividad de Login antes de cada prueba
    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testSimulacionInteraccionUI() {
        // En base a la imagen: "Automatizan la interacción con la interfaz (formularios)"
        // 1. Ingresamos texto en el campo de Usuario y cerramos el teclado
        onView(withId(R.id.etUsername))
                .perform(typeText("usuario_prueba"), closeSoftKeyboard());

        // 2. Ingresamos texto en el campo de Contraseña y cerramos el teclado
        onView(withId(R.id.etPassword))
                .perform(typeText("123456"), closeSoftKeyboard());

        // En base a la imagen: "Automatizan la interacción con la interfaz (toques)"
        // 3. Simulamos un toque en el botón de Iniciar Sesión
        onView(withId(R.id.btnLogin))
                .perform(click());
    }
}
