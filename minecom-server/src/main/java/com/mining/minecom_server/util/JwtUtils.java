package com.mining.minecom_server.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    // Méthode pour générer un token
    public String generateJwtToken(String username) {
        return Jwts.builder()
                .subject(username) // Le sujet (souvent le nom d'utilisateur)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256) // Signature avec la clé secrète
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Méthode pour obtenir le nom d'utilisateur à partir du token
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Méthode pour valider le token
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            // Token JWT mal formé
        } catch (ExpiredJwtException e) {
            // Token JWT expiré
        } catch (UnsupportedJwtException e) {
            // Token JWT non supporté
        } catch (IllegalArgumentException e) {
            // Chaîne de revendications JWT vide
        } catch (SignatureException e) {
            // Signature invalide
        }
        return false;
    }
}