package com.api.chatstack.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public String extractUserEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, Date expiration) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(expiration)
                .signWith(getSigninKey())
                .compact();
    }

    public String generateAccessToken(UserDetails userDetails) {
        Date expiration = new Date(System.currentTimeMillis() + accessTokenExpiration);
        return generateToken(new HashMap<>(), userDetails, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Date expiration = new Date(System.currentTimeMillis() + refreshTokenExpiration);
        return generateToken(new HashMap<>(), userDetails, expiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userEmail = extractUserEmail(token);
        return userDetails.getUsername().equals(userEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claim = extractAllClaims(token);
        return claimsResolver.apply(claim);
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
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
