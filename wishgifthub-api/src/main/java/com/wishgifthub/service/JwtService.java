package com.wishgifthub.service;

import com.wishgifthub.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {
    @Value("${wishgifthub.jwt.secret}")
    private String jwtSecret;

    @Value("${wishgifthub.jwt.expiration}")
    private long jwtExpirationMs;

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("isAdmin", user.isAdmin())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(User user, List<UUID> groupIds) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        List<String> groupIdsAsString = groupIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("isAdmin", user.isAdmin())
                .claim("groupIds", groupIdsAsString)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        String subject = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return UUID.fromString(subject);
    }

    public boolean isAdminFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Boolean.TRUE.equals(Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("isAdmin", Boolean.class));
    }

    public List<UUID> getGroupIdsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        List<String> groupIdsAsString = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("groupIds", List.class);
        if (groupIdsAsString == null) {
            return List.of();
        }
        return groupIdsAsString.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}

