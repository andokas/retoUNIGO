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
        // Lee el archivo private_key.pem de res/raw
        InputStream is = context.getResources().openRawResource(
                context.getResources().getIdentifier("private_key", "raw", context.getPackageName()));

        String key = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            key = new String(is.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
        }

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}