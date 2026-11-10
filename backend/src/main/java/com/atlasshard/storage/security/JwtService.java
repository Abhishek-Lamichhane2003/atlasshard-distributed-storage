package com.atlasshard.storage.security;

import com.atlasshard.storage.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String ISSUER = "atlasshard";
    private final SecretKey key;

    public JwtService(AppProperties props) {
        byte[] secret = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String create(String userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(ISSUER)
            .subject(userId)
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(86400)))
            .signWith(key)
            .compact();
    }

    public String subject(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!ISSUER.equals(claims.getIssuer())) {
            throw new IllegalArgumentException("invalid token issuer");
        }
        return claims.getSubject();
    }
}
