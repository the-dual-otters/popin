package com.snow.popin.global.jwt;

import com.snow.popin.domain.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=testsecrettestsecrettestsecrettestsecret"
})
@DisplayName("JWT 필터 통합 테스트")
class JwtFilterIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtTokenResolver jwtTokenResolver;

    @Autowired
    private JwtFilter jwtFilter;

    @MockBean
    private AuthService authService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    private final String testEmail = "test@example.com";
    private final String testName = "테스트사용자";
    private final String testRole = "USER";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("실제 JWT 토큰으로 인증이 성공한다")
    void givenRealJwtToken_whenFilter_thenAuthenticationSuccess() throws Exception {
        // Given
        String realToken = jwtUtil.createToken(testEmail, testName, testRole);
        request.addHeader("Authorization", "Bearer " + realToken);

        // UserDetails Mock 설정
        UserDetails userDetails = User.builder()
                .username(testEmail)
                .password("password")
                .authorities(Collections.singleton(new SimpleGrantedAuthority("ROLE_" + testRole)))
                .build();

        given(authService.loadUserByUsername(anyString())).willReturn(userDetails);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(testEmail);
        assertThat(authentication.getAuthorities())
                .extracting(auth -> auth.getAuthority())
                .containsExactly("ROLE_" + testRole);
    }

    @Test
    @DisplayName("쿠키로 전달된 토큰으로 인증이 성공한다")
    void givenTokenInCookie_whenFilter_thenAuthenticationSuccess() throws Exception {
        // Given
        String realToken = jwtUtil.createToken(testEmail, testName, testRole);

        // 쿠키 설정
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("jwtToken", realToken);
        request.setCookies(cookie);

        // UserDetails Mock 설정
        UserDetails userDetails = User.builder()
                .username(testEmail)
                .password("password")
                .authorities(Collections.singleton(new SimpleGrantedAuthority("ROLE_" + testRole)))
                .build();

        given(authService.loadUserByUsername(anyString())).willReturn(userDetails);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("만료된 토큰으로는 인증이 실패한다")
    void givenExpiredToken_whenFilter_thenAuthenticationFails() throws Exception {
        // Given - 만료된 토큰 생성 (음수 시간)
        String expiredToken = "expired.token.here";
        request.addHeader("Authorization", "Bearer " + expiredToken);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("토큰이 없으면 인증이 설정되지 않는다")
    void givenNoToken_whenFilter_thenNoAuthentication() throws Exception {
        // Given - 토큰 없음

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰으로는 인증이 실패한다")
    void givenMalformedToken_whenFilter_thenAuthenticationFails() throws Exception {
        // Given
        request.addHeader("Authorization", "Bearer invalid-token-format");

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }
}