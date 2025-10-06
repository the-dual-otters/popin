package com.snow.popin.global.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@DisplayName("JWT 필터 단위 테스트")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtFilterTest {

    @InjectMocks
    private JwtFilter jwtFilter;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private UserDetailsService userDetailsService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    private final String testToken = "valid.jwt.token";
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이 있으면 인증이 설정된다")
    void givenValidTokenInHeader_whenFilter_thenSetsAuthentication() throws Exception {
        // Given
        UserDetails userDetails = User.builder()
                .username(testEmail)
                .password("password")
                .authorities(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        given(jwtTokenResolver.resolve(request)).willReturn(testToken);
        given(jwtUtil.validateToken(testToken)).willReturn(true);
        given(jwtUtil.getEmail(testToken)).willReturn(testEmail);
        given(applicationContext.getBean(UserDetailsService.class)).willReturn(userDetailsService);
        given(userDetailsService.loadUserByUsername(anyString())).willReturn(userDetails);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(testEmail);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("토큰이 없으면 인증이 설정되지 않는다")
    void givenNoToken_whenFilter_thenNoAuthentication() throws Exception {
        // Given
        given(jwtTokenResolver.resolve(request)).willReturn(null);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증이 설정되지 않는다")
    void givenInvalidToken_whenFilter_thenNoAuthentication() throws Exception {
        // Given
        given(jwtTokenResolver.resolve(request)).willReturn(testToken);
        given(jwtUtil.validateToken(testToken)).willReturn(false);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("토큰에서 이메일을 추출할 수 없으면 인증이 설정되지 않는다")
    void givenTokenWithoutEmail_whenFilter_thenNoAuthentication() throws Exception {
        // Given
        given(jwtTokenResolver.resolve(request)).willReturn(testToken);
        given(jwtUtil.validateToken(testToken)).willReturn(true);
        given(jwtUtil.getEmail(testToken)).willReturn(null);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 에러 응답을 반환한다")
    void givenUserNotFound_whenFilter_thenSendsErrorResponse() throws Exception {
        // Given
        given(jwtTokenResolver.resolve(request)).willReturn(testToken);
        given(jwtUtil.validateToken(testToken)).willReturn(true);
        given(jwtUtil.getEmail(testToken)).willReturn(testEmail);
        given(applicationContext.getBean(UserDetailsService.class)).willReturn(userDetailsService);
        given(userDetailsService.loadUserByUsername(anyString()))
                .willThrow(new UsernameNotFoundException("User not found"));

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        // 에러 응답 확인
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    @DisplayName("정적 리소스는 필터를 통과하지 않는다")
    void givenStaticResource_whenShouldNotFilter_thenReturnsTrue() {
        // Given & When & Then
        assertThat(jwtFilter.shouldNotFilter(createRequest("/css/style.css"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/js/app.js"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/images/logo.png"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/favicon.ico"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/api/auth/login"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/api/auth/signup"))).isTrue();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/api/auth/check-email"))).isTrue();
    }

    @Test
    @DisplayName("보호된 API는 필터를 통과한다")
    void givenProtectedApi_whenShouldNotFilter_thenReturnsFalse() {
        // Given & When & Then
        assertThat(jwtFilter.shouldNotFilter(createRequest("/api/users"))).isFalse();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/api/spaces"))).isFalse();
        assertThat(jwtFilter.shouldNotFilter(createRequest("/admin/dashboard"))).isFalse();
    }

    // Helper method
    private MockHttpServletRequest createRequest(String requestURI) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(requestURI);
        return req;
    }
}