package com.example.bismart.network;

import android.content.Context;
import android.os.Build;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class EuskalmetTokenManager {

    // Duración del token: 1 hora
    private static final long EXPIRATION_MS = 60 * 60 * 1000;

    private static String cachedToken = null;
    private static long tokenExpiry = 0;

    /**
     * Devuelve un JWT válido, generando uno nuevo si ha expirado.
     * @param context contexto para leer el archivo de clave privada
     * @param email email con el que te registraste en Euskalmet
     */
    public static String getToken(Context context, String email) {
        // Si el token sigue siendo válido, lo reutilizamos
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry) {
            return "Bearer " + cachedToken;
        }

        try {
            PrivateKey privateKey = loadPrivateKey(context);

            long now = System.currentTimeMillis();
            cachedToken = Jwts.builder()
                    .setIssuer(email)
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(now + EXPIRATION_MS))
                    .claim("email", email)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            tokenExpiry = now + EXPIRATION_MS - 60000; // Renovar 1 min antes de expirar
            return "Bearer " + cachedToken;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static PrivateKey loadPrivateKey(Context context) throws Exception {
        // 1. Buscamos el archivo de forma segura
        int resId = context.getResources().getIdentifier("private_key", "raw", context.getPackageName());

        if (resId == 0) {
            throw new Exception("ERROR CRÍTICO: No se encuentra el archivo 'private_key' en la carpeta res/raw. Asegúrate de que el nombre esté todo en minúsculas y sin espacios.");
        }

        // 2. Leemos el archivo
        InputStream is = context.getResources().openRawResource(resId);
        java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
        String key = scanner.hasNext() ? scanner.next() : "";
        is.close();

        // 3. Super-limpieza: Eliminamos TODOS los tipos de cabeceras posibles y los espacios
        key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\r", "")
                .replaceAll("\\s", "");

        // 4. Decodificamos y generamos la llave
        byte[] keyBytes = android.util.Base64.decode(key, android.util.Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}