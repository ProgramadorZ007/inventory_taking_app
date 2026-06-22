package com.procesadoraperu.inventario.core.utils;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

/**
 * Utilidad para el procesamiento y extracción de información de tokens JWT.
 *
 * Esta clase se encarga de decodificar la sección 'Payload' de un JSON Web Token.
 * Es fundamental para la seguridad y personalización de la aplicación, ya que
 * permite obtener los 'Claims' (datos del usuario) de forma local y eficiente.
 */
public class JwtDecoder {

    private static final String TAG = "JwtDecoder";

    /**
     * Decodifica la carga útil (Payload) de un token JWT.
     *
     * El proceso consiste en separar las partes del token, decodificar la sección
     * central usando Base64 bajo el estándar URL_SAFE y transformar el resultado
     * en un formato JSON manejable por la aplicación.
     *
     * @param jwtToken El token de acceso completo (formato: Header.Payload.Signature).
     * @return Un {@link JSONObject} con los datos (claims) del token, o null si el token es inválido.
     */
    public static JSONObject decodePayload(String jwtToken) {
        // Validación de entrada para evitar excepciones por nulos o vacíos
        if (jwtToken == null || jwtToken.isEmpty()) {
            return null;
        }

        try {
            // 1. DIVISIÓN DEL TOKEN:
            // Un JWT estándar se compone de 3 partes separadas por puntos.
            String[] parts = jwtToken.split("\\.");

            // Verificamos que la estructura sea correcta (mínimo Header y Payload)
            if (parts.length < 2) {
                Log.e(TAG, "Estructura de JWT inválida detectada.");
                return null;
            }

            // 2. EXTRACCIÓN DEL PAYLOAD:
            // El Payload (datos del usuario) siempre se ubica en el índice 1.
            String payloadEncoded = parts[1];

            // 3. DECODIFICACIÓN BASE64:
            // Se utiliza URL_SAFE ya que los JWT están diseñados para ser transmitidos vía URL.
            byte[] decodedBytes = Base64.decode(payloadEncoded, Base64.URL_SAFE);
            String payloadDecoded = new String(decodedBytes, "UTF-8");

            // 4. PARSEO A JSON:
            // Retorna los datos estructurados para su consumo en la lógica de negocio.
            return new JSONObject(payloadDecoded);

        } catch (Exception e) {
            // Registro de error en Logcat para facilitar el seguimiento técnico
            Log.e(TAG, "Fallo crítico al procesar el Payload del JWT: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifica si un token JWT ha expirado basándose en el claim "exp".
     *
     * @param jwtToken El token de acceso completo.
     * @return true si el token expiró o no se pudo leer; false si aún es válido.
     */
    public static boolean isTokenExpired(String jwtToken) {
        JSONObject payload = decodePayload(jwtToken);
        if (payload == null) {
            return true;
        }

        // El claim "exp" es el timestamp en segundos desde epoch
        long exp = payload.optLong("exp", 0);
        if (exp == 0) {
            // Si no tiene claim "exp", asumimos que no expira (servidor lo maneja)
            return false;
        }

        // Comparar con la hora actual del dispositivo (en segundos)
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        return currentTimeSeconds >= exp;
    }
}