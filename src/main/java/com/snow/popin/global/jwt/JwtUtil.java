package com.snow.popin.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs = 6 * 60 * 60 * 1000; // 6시간

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long for HS256");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String createToken(String email, String name, String role){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createToken(Long userId, String email, String name, String role){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("name", name)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰도 payload(claims)는 꺼낼 수 있으니 필요하면 반환
            return e.getClaims();
        }
    }


    public String getEmail(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (Exception e) {
            log.error("토큰에서 email 추출 실패 : {}", e.getMessage());
            return null;
        }
    }

    public String getName(String token) {
        try {
            return extractClaims(token).get("name", String.class);
        } catch (Exception e) {
            log.error("토큰에서 name 추출 실패 : {}", e.getMessage());
            return null;
        }
    }

    public String getRole(String token) {
        try {
            return extractClaims(token).get("role", String.class);
        } catch (Exception e) {
            log.error("토큰에서 role 추출 실패 : {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token){
        try{
            Object userIdObj = extractClaims(token).get("userId");
            if (userIdObj == null){
                return null;
            }

            Long userId = Long.valueOf(userIdObj.toString());
            log.debug("토큰에서 추출한 userId: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("토큰에서 userId 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            boolean expired = expiration.before(new Date());
            log.debug("토큰 만료 확인 : {}", expired);
            return expired;
        } catch (ExpiredJwtException e) {
            log.debug("토큰이 만료됨 : {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("토큰 만료 확인 중 오류 : {}", e.getMessage());
            return true; // 오류 시 만료된 것으로 처리
        }
    }

    public boolean validateToken(String token){
        try {
            if (token == null || token.trim().isEmpty()) {
                log.debug("토큰이 null이거나 비어있음");
                return false;
            }

            boolean expired = isTokenExpired(token);
            boolean valid = !expired;
            log.debug("토큰 검증 결과 : {}", valid);
            return valid;
        } catch (Exception e) {
            log.error("토큰 검증 실패 : {}", e.getMessage());
            return false;
        }
    }

}
