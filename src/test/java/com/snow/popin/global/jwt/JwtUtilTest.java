package com.snow.popin.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWT 유틸리티 테스트")
class JwtUtilTest {

    private JwtUtil sut;
    private final String testSecret = "testsecrettestsecrettestsecrettestsecret";
    private final String testEmail = "test@example.com";
    private final String testName = "테스트유저";
    private final String testRole = "USER";
    private final Long testUserId = 1L;

    @BeforeEach
    void setUp(){
        sut = new JwtUtil(testSecret);
    }

    @DisplayName("유효한 정보로 토큰을 생성하면 JWT 토큰이 반환된다")
    @Test
    void givenValidInfo_whenCreateToken_thenReturnsJwtToken() {
        // When
        String token = sut.createToken(testEmail, testName, testRole);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3개 부분으로 구성

        // 토큰에서 정보 추출 검증
        assertThat(sut.getEmail(token)).isEqualTo(testEmail);
        assertThat(sut.getName(token)).isEqualTo(testName);
        assertThat(sut.getRole(token)).isEqualTo(testRole);
    }

    @DisplayName("새로 생성된 토큰은 만료되지 않은 상태이다")
    @Test
    void givenFreshToken_whenCheckExpired_thenReturnsFalse() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        boolean isExpired = sut.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @DisplayName("유효한 토큰에서 이메일을 추출하면 올바른 이메일을 반환한다")
    @Test
    void givenValidToken_whenGetEmail_thenReturnsCorrectEmail() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        String extractedEmail = sut.getEmail(token);

        // Then
        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @DisplayName("유효한 토큰에서 이름을 추출하면 올바른 이름을 반환한다")
    @Test
    void givenValidToken_whenGetName_thenReturnsCorrectName() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        String extractedName = sut.getName(token);

        // Then
        assertThat(extractedName).isEqualTo(testName);
    }

    @DisplayName("유효한 토큰에서 역할을 추출하면 올바른 역할을 반환한다")
    @Test
    void givenValidToken_whenGetRole_thenReturnsCorrectRole() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        String extractedRole = sut.getRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(testRole);
    }

    @DisplayName("잘못된 토큰에서 정보를 추출하려 하면 null을 반환한다")
    @Test
    void givenInvalidToken_whenExtractInfo_thenReturnsNull() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThat(sut.getEmail(invalidToken)).isNull();
        assertThat(sut.getName(invalidToken)).isNull();
        assertThat(sut.getRole(invalidToken)).isNull();
        assertThat(sut.getUserId(invalidToken)).isNull();
    }

    @DisplayName("유효한 토큰의 만료 여부를 확인하면 false를 반환한다")
    @Test
    void givenValidToken_whenCheckExpired_thenReturnsFalse() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        boolean isExpired = sut.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @DisplayName("만료된 토큰의 만료 여부를 확인하면 true를 반환한다")
    @Test
    void givenExpiredToken_whenCheckExpired_thenReturnsTrue() {
        // Given - 만료된 토큰 생성 (과거 시간으로 설정)
        SecretKey secretKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        Date past = new Date(System.currentTimeMillis() - 1000); // 1초 전
        String expiredToken = Jwts.builder()
                .setSubject(testEmail)
                .claim("name", testName)
                .claim("role", testRole)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(past)
                .signWith(secretKey)
                .compact();

        // When
        boolean isExpired = sut.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @DisplayName("유효한 토큰을 검증하면 true를 반환한다")
    @Test
    void givenValidToken_whenValidate_thenReturnsTrue() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        boolean isValid = sut.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @DisplayName("잘못된 형식의 토큰은 검증에 실패한다")
    @ParameterizedTest
    @ValueSource(strings = {"invalid.token", "not.a.jwt.token", "completely-wrong-format"})
    void givenInvalidToken_whenValidate_thenReturnsFalse(String invalidToken) {
        // When
        boolean isValid = sut.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @DisplayName("토큰 만료 시간이 6시간으로 설정되는지 확인한다")
    @Test
    void givenToken_whenCheckExpiration_thenExpiresInSixHours() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);
        Claims claims = sut.extractClaims(token);

        // When
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        long diffInMillis = expiration.getTime() - issuedAt.getTime();
        long diffInHours = diffInMillis / (1000 * 60 * 60);

        // Then
        assertThat(diffInHours).isEqualTo(6); // 6시간
    }

    @DisplayName("만료된 토큰을 검증하면 false를 반환한다")
    @Test
    void givenExpiredToken_whenValidate_thenReturnsFalse() {
        // Given - 만료된 토큰 생성
        SecretKey secretKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        Date past = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = Jwts.builder()
                .setSubject(testEmail)
                .setExpiration(past)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // When
        boolean isValid = sut.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @DisplayName("Claims를 추출하면 올바른 정보가 반환된다")
    @Test
    void givenValidToken_whenExtractClaims_thenReturnsCorrectClaims() {
        // Given
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        Claims claims = sut.extractClaims(token);

        // Then
        assertThat(claims.getSubject()).isEqualTo(testEmail);
        assertThat(claims.get("name", String.class)).isEqualTo(testName);
        assertThat(claims.get("role", String.class)).isEqualTo(testRole);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @DisplayName("null이나 빈 토큰에 대해 적절히 처리한다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void givenNullOrEmptyToken_whenProcessToken_thenHandlesGracefully(String token) {
        // When & Then
        assertThat(sut.getEmail(token)).isNull();
        assertThat(sut.getName(token)).isNull();
        assertThat(sut.getRole(token)).isNull();
        assertThat(sut.getUserId(token)).isNull();
        assertThat(sut.validateToken(token)).isFalse();
    }

    @DisplayName("userId가 포함된 토큰에서 userId를 추출할 수 있다")
    @Test
    void givenTokenWithUserId_whenGetUserId_thenReturnsUserId() {
        // Given - userId가 실제로 토큰에 저장됨
        String token = sut.createToken(testUserId, testEmail, testName, testRole);

        // When
        Long extractedUserId = sut.getUserId(token);

        // Then - 실제 구현에서는 userId가 토큰에 저장되어 정상 반환
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @DisplayName("userId가 없는 토큰에서 userId를 추출하면 null을 반환한다")
    @Test
    void givenTokenWithoutUserId_whenGetUserId_thenReturnsNull() {
        // Given - userId 파라미터가 없는 메서드로 토큰 생성
        String token = sut.createToken(testEmail, testName, testRole);

        // When
        Long extractedUserId = sut.getUserId(token);

        // Then - userId가 없는 토큰에서는 null 반환
        assertThat(extractedUserId).isNull();
    }

    @DisplayName("특수문자가 포함된 정보로도 토큰 생성이 가능하다")
    @Test
    void givenSpecialCharacters_whenCreateToken_thenWorksCorrectly() {
        // Given
        String specialEmail = "test+special@sub-domain.co.kr";
        String specialName = "테스트 유저 (특수문자 #@!)";
        String specialRole = "ADMIN";

        // When
        String token = sut.createToken(specialEmail, specialName, specialRole);

        // Then
        assertThat(token).isNotNull();
        assertThat(sut.getEmail(token)).isEqualTo(specialEmail);
        assertThat(sut.getName(token)).isEqualTo(specialName);
        assertThat(sut.getRole(token)).isEqualTo(specialRole);
        assertThat(sut.validateToken(token)).isTrue();
    }

}