package com.procesadoraperu.inventario.core.utils;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

public class JwtDecoder {

    private static final String TAG = "JwtDecoder";

    /**
     * Decodifica la carga útil (payload) de un token JWT.
     *
     * @param jwtToken El token de acceso completo (Header.Payload.Signature)
     * @return Un JSONObject con los datos (claims) del token, o null si falla.
     */
    public static JSONObject decodePayload(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return null;
        }

        try {
            // Un JWT estándar tiene 3 partes separadas por puntos (Header.Payload.Signature)
            String[] parts = jwtToken.split("\\.");

            // Validamos que al menos tenga el Header y el Payload
            if (parts.length < 2) {
                Log.e(TAG, "Formato de JWT inválido.");
                return null;
            }

            // El Payload es la segunda parte (índice 1)
            String payloadEncoded = parts[1];

            // Decodificamos usando URL_SAFE (estándar para JWT)
            byte[] decodedBytes = Base64.decode(payloadEncoded, Base64.URL_SAFE);
            String payloadDecoded = new String(decodedBytes, "UTF-8");

            // Convertimos el String decodificado a un objeto JSON
            return new JSONObject(payloadDecoded);

        } catch (Exception e) {
            Log.e(TAG, "Error al decodificar el token JWT: " + e.getMessage());
            return null;
        }
    }
}