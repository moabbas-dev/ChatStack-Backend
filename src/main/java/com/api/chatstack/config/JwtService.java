package com.api.chatstack.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PublicKey;

@Service
public class JwtService {
    private final String SECRET_KEY = "a3f44c1b25951104d34e4b1e04a2e649a2fd480d04d32b86286da598500ce811";

    public String extractUserEmail(String token) {
        return null;
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigninKey() {
        return null;
    }

}
