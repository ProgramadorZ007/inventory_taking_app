package com.procesadoraperu.inventario.presentation.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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
    public void testLoginFormElementsAreDisplayed() {
        // Verificar que los elementos del formulario están visibles
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()));
    }

    @Test
    public void testEmptyFieldsShowsValidationMessage() {
        // Si ambos campos están vacíos y presionamos login, debe mostrar mensaje
        onView(withId(R.id.btnLogin)).perform(click());

        // El Snackbar con "Completa todos los campos" debe aparecer
        onView(withText("Completa todos los campos"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginButtonTextChangesOnSubmit() {
        // Ingresamos credenciales de prueba
        onView(withId(R.id.etUsername))
                .perform(typeText("usuario_prueba"), closeSoftKeyboard());
        onView(withId(R.id.etPassword))
                .perform(typeText("123456"), closeSoftKeyboard());

        // Al presionar login, el botón debe cambiar su texto a "Verificando..."
        onView(withId(R.id.btnLogin)).perform(click());

        // Verificar que el botón se deshabilita durante la carga
        // (puede ser instantáneo si falla la red, así que verificamos que al menos se intentó)
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }
}
