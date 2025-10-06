package com.snow.popin.global.util;

import com.snow.popin.domain.auth.constant.AuthProvider;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@DisplayName("UserUtil 테스트")
@ExtendWith(MockitoExtension.class)
class UserUtilTest {

    @InjectMocks
    private UserUtil userUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private com.snow.popin.global.jwt.JwtTokenResolver jwtTokenResolver;

    private User testUser;
    private UserDetails userDetails;
    private final String testEmail = "test@example.com";
    private final String testName = "테스트유저";
    private final Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email(testEmail)
                .password("encodedPassword")
                .name(testName)
                .nickname("테스터")
                .phone("010-1234-5678")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();

        // 이 라인을 추가하세요
        setUserId(testUser, testUserId);

        // UserDetails 생성
        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testEmail)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 사용자의 ID를 정상적으로 반환한다")
    void givenAuthenticatedUser_whenGetCurrentUserId_thenReturnsUserId() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        Long userId = userUtil.getCurrentUserId();

        // Then
        assertThat(userId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("인증된 사용자의 이메일을 정상적으로 반환한다")
    void givenAuthenticatedUser_whenGetCurrentUserEmail_thenReturnsEmail() {
        // Given
        setUpAuthentication();

        // When
        String email = userUtil.getCurrentUserEmail();

        // Then
        assertThat(email).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("인증된 사용자의 User 엔티티를 정상적으로 반환한다")
    void givenAuthenticatedUser_whenGetCurrentUser_thenReturnsUser() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        User user = userUtil.getCurrentUser();

        // Then
        assertThat(user).isEqualTo(testUser);
        assertThat(user.getId()).isEqualTo(testUserId);
        assertThat(user.getEmail()).isEqualTo(testEmail);
        assertThat(user.getName()).isEqualTo(testName);
    }

    @Test
    @DisplayName("인증된 사용자의 이름을 정상적으로 반환한다")
    void givenAuthenticatedUser_whenGetCurrentUserName_thenReturnsName() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        String name = userUtil.getCurrentUserName();

        // Then
        assertThat(name).isEqualTo(testName);
    }

    @Test
    @DisplayName("인증된 사용자의 역할을 정상적으로 반환한다")
    void givenAuthenticatedUser_whenGetCurrentUserRole_thenReturnsRole() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        String role = userUtil.getCurrentUserRole();

        // Then
        assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("사용자가 특정 역할을 가지고 있는지 정확히 확인한다")
    void givenAuthenticatedUser_whenHasRole_thenReturnsCorrectResult() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When & Then
        assertThat(userUtil.hasRole("USER")).isTrue();
        assertThat(userUtil.hasRole("ADMIN")).isFalse();
        assertThat(userUtil.hasRole("PROVIDER")).isFalse();
        assertThat(userUtil.hasRole("HOST")).isFalse();
    }

    @Test
    @DisplayName("인증된 상태에서 isAuthenticated가 true를 반환한다")
    void givenAuthenticatedUser_whenIsAuthenticated_thenReturnsTrue() {
        // Given
        setUpAuthentication();

        // When
        boolean isAuthenticated = userUtil.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isTrue();
    }

    @Test
    @DisplayName("인증되지 않은 상태에서 isAuthenticated가 false를 반환한다")
    void givenUnauthenticatedUser_whenIsAuthenticated_thenReturnsFalse() {
        // Given - SecurityContext가 비어있음

        // When
        boolean isAuthenticated = userUtil.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isFalse();
    }

    @Test
    @DisplayName("리소스 소유자인 경우 isOwner가 true를 반환한다")
    void givenOwnerUser_whenIsOwner_thenReturnsTrue() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        boolean isOwner = userUtil.isOwner(testUserId);

        // Then
        assertThat(isOwner).isTrue();
    }

    @Test
    @DisplayName("리소스 소유자가 아닌 경우 isOwner가 false를 반환한다")
    void givenNonOwnerUser_whenIsOwner_thenReturnsFalse() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        boolean isOwner = userUtil.isOwner(999L); // 다른 사용자 ID

        // Then
        assertThat(isOwner).isFalse();
    }

    @Test
    @DisplayName("ADMIN 역할 사용자의 isAdmin이 true를 반환한다")
    void givenAdminUser_whenIsAdmin_thenReturnsTrue() {
        // Given
        User adminUser = User.builder()
                .email(testEmail)
                .name(testName)
                .role(Role.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .build();
        setUserId(adminUser, testUserId);

        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(adminUser));

        // When
        boolean isAdmin = userUtil.isAdmin();

        // Then
        assertThat(isAdmin).isTrue();
    }

    @Test
    @DisplayName("PROVIDER 역할 사용자의 isProvider가 true를 반환한다")
    void givenProviderUser_whenIsProvider_thenReturnsTrue() {
        // Given
        User providerUser = User.builder()
                .email(testEmail)
                .name(testName)
                .role(Role.PROVIDER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        setUserId(providerUser, testUserId);

        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(providerUser));

        // When
        boolean isProvider = userUtil.isProvider();

        // Then
        assertThat(isProvider).isTrue();
    }

    @Test
    @DisplayName("현재 사용자 정보를 Map 형태로 정상적으로 반환한다")
    void givenAuthenticatedUser_whenGetCurrentUserInfo_thenReturnsUserInfoMap() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));

        // When
        Map<String, Object> userInfo = userUtil.getCurrentUserInfo();

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.get("id")).isEqualTo(testUserId);
        assertThat(userInfo.get("email")).isEqualTo(testEmail);
        assertThat(userInfo.get("name")).isEqualTo(testName);
        assertThat(userInfo.get("nickname")).isEqualTo("테스터");
        assertThat(userInfo.get("role")).isEqualTo("USER");
        assertThat(userInfo.get("authProvider")).isEqualTo("LOCAL");
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 getCurrentUserId 호출 시 예외가 발생한다")
    void givenUnauthenticatedUser_whenGetCurrentUserId_thenThrowsException() {
        // Given - SecurityContext가 비어있음

        // When & Then
        assertThatThrownBy(() -> userUtil.getCurrentUserId())
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("존재하지 않는 사용자에 대해 getCurrentUser 호출 시 예외가 발생한다")
    void givenNonExistentUser_whenGetCurrentUser_thenThrowsException() {
        // Given
        setUpAuthentication();
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userUtil.getCurrentUser())
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("익명 사용자가 정보 조회 시 예외가 발생한다")
    void givenAnonymousUser_whenGetCurrentUserId_thenThrowsException() {
        // Given - 익명 사용자 인증 설정
        UsernamePasswordAuthenticationToken anonymousAuth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

        // When & Then
        assertThatThrownBy(() -> userUtil.getCurrentUserId())
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("역할 확인 시 예외 발생하면 false를 반환한다")
    void givenExceptionDuringRoleCheck_whenHasRole_thenReturnsFalse() {
        // Given - 인증되지 않은 상태

        // When
        boolean hasRole = userUtil.hasRole("USER");

        // Then
        assertThat(hasRole).isFalse();
    }

    @Test
    @DisplayName("소유권 확인 시 예외 발생하면 false를 반환한다")
    void givenExceptionDuringOwnershipCheck_whenIsOwner_thenReturnsFalse() {
        // Given - 인증되지 않은 상태

        // When
        boolean isOwner = userUtil.isOwner(testUserId);

        // Then
        assertThat(isOwner).isFalse();
    }

    /**
     * 테스트용 인증 설정
     */
    private void setUpAuthentication() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 테스트용 User 엔티티에 ID 설정 (리플렉션 사용)
    private void setUserId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 ID 설정 실패", e);
        }
    }

}