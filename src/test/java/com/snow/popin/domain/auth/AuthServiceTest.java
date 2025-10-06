package com.snow.popin.domain.auth;

import com.snow.popin.domain.auth.constant.AuthProvider;
import com.snow.popin.domain.auth.dto.*;
import com.snow.popin.domain.auth.service.AuthService;
import com.snow.popin.domain.category.entity.Category;
import com.snow.popin.domain.category.repository.CategoryRepository;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.jwt.JwtTokenResolver;
import com.snow.popin.global.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("AuthService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    private User mockUser;
    private Category mockCategory1;
    private Category mockCategory2;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .name("테스트유저")
                .nickname("테스터")
                .phone("010-1234-5678")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();

        // ID 필드 설정 (리플렉션 사용)
        setUserId(mockUser, 1L);

        // 테스트용 카테고리 생성
        mockCategory1 = createMockCategory(1L, "음식");
        mockCategory2 = createMockCategory(2L, "여행");
    }

    // ================ 회원가입 테스트 ================

    @DisplayName("회원가입 성공 시 사용자 정보를 저장하고 응답을 반환한다")
    @Test
    void givenValidSignupRequest_whenSignup_thenReturnsSignupResponse() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .build();

        User savedUser = User.builder()
                .email(request.getEmail())
                .password("encodedPassword")
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        setUserId(savedUser, 1L);

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // When
        SignupResponse response = authService.signup(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("회원가입이 완료되었습니다.");
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getName()).isEqualTo("신규유저");
        assertThat(response.getNickname()).isEqualTo("뉴비");

        verify(userRepository).save(any(User.class));
    }

    @DisplayName("관심사와 함께 회원가입 성공 시 관심사도 함께 저장된다")
    @Test
    void givenSignupRequestWithInterests_whenSignup_thenSavesUserWithInterests() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .interests(Arrays.asList("음식", "여행"))
                .build();

        User savedUser = User.builder()
                .email(request.getEmail())
                .password("encodedPassword")
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();
        setUserId(savedUser, 1L);

        Set<Category> mockCategories = new HashSet<>();
        mockCategories.add(mockCategory1);
        mockCategories.add(mockCategory2);

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(categoryRepository.findByNameIn(Arrays.asList("음식", "여행"))).willReturn(mockCategories);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // When
        SignupResponse response = authService.signup(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        verify(categoryRepository).findByNameIn(Arrays.asList("음식", "여행"));
        verify(userRepository).save(any(User.class));
    }

    @DisplayName("관심사가 10개를 초과할 때 BAD_REQUEST 예외를 던진다")
    @Test
    void givenTooManyInterests_whenSignup_thenThrowsBadRequestException() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .interests(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("관심사는 최대 10개까지");
    }

    @DisplayName("비밀번호가 일치하지 않을 때 BAD_REQUEST 예외를 던진다")
    @Test
    void givenMismatchedPasswords_whenSignup_thenThrowsBadRequestException() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("differentPassword")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .build();

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @DisplayName("이미 존재하는 이메일로 회원가입 시 DUPLICATE_EMAIL 예외를 던진다")
    @Test
    void givenDuplicateEmail_whenSignup_thenThrowsDuplicateEmailException() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("duplicate@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("중복유저")
                .nickname("중복")
                .phone("010-1234-5678")
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @DisplayName("이미 존재하는 닉네임으로 회원가입 시 BAD_REQUEST 예외를 던진다")
    @Test
    void givenDuplicateNickname_whenSignup_thenThrowsBadRequestException() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("중복닉네임")
                .phone("010-1234-5678")
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                .hasMessageContaining("이미 사용 중인 닉네임");
    }

    // ================ 로그인 테스트 ================

    @DisplayName("로그인 성공 시 토큰과 사용자 정보를 반환한다")
    @Test
    void givenValidRequest_whenLogin_thenReturnsLoginResponse() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(mockUser.getEmail());
        request.setPassword("rawPassword");

        given(userRepository.findByEmail(mockUser.getEmail()))
                .willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("rawPassword", mockUser.getPassword()))
                .willReturn(true);
        given(jwtUtil.createToken(anyLong(), anyString(), anyString(), any()))
                .willReturn("jwt-access-token");

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(mockUser.getId());
        assertThat(response.getEmail()).isEqualTo(mockUser.getEmail());
        assertThat(response.getName()).isEqualTo(mockUser.getName());
        assertThat(response.getRole()).isEqualTo(mockUser.getRole().name());
    }

    @DisplayName("존재하지 않는 이메일로 로그인 시 LOGIN_FAILED 예외를 던진다")
    @Test
    void givenNonExistentEmail_whenLogin_thenThrowsLoginFailedException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        given(userRepository.findByEmail(request.getEmail()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @DisplayName("잘못된 비밀번호로 로그인 시 LOGIN_FAILED 예외를 던진다")
    @Test
    void givenWrongPassword_whenLogin_thenThrowsLoginFailedException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(mockUser.getEmail());
        request.setPassword("wrongPassword");

        given(userRepository.findByEmail(mockUser.getEmail()))
                .willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("wrongPassword", mockUser.getPassword()))
                .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    // ================ 중복 확인 테스트 ================

    @DisplayName("이메일 존재 확인")
    @Test
    void givenEmail_whenEmailExists_thenReturnsBoolean() {
        // Given
        String email = "test@example.com";
        given(userRepository.existsByEmail(email)).willReturn(true);

        // When
        boolean exists = authService.emailExists(email);

        // Then
        assertThat(exists).isTrue();
    }

    @DisplayName("닉네임 존재 확인")
    @Test
    void givenNickname_whenNicknameExists_thenReturnsBoolean() {
        // Given
        String nickname = "테스터";
        given(userRepository.existsByNickname(nickname)).willReturn(true);

        // When
        boolean exists = authService.nicknameExists(nickname);

        // Then
        assertThat(exists).isTrue();
    }

    // ================ 헬퍼 메서드 ================

    /**
     * 테스트용으로 User 엔티티의 ID를 설정하는 헬퍼 메서드
     * @param user 사용자 엔티티
     * @param id 설정할 ID
     */
    private void setUserId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 ID 설정 실패", e);
        }
    }

    /**
     * 테스트용 Category 생성 헬퍼 메서드
     * @param id 카테고리 ID
     * @param name 카테고리 이름
     * @return Category 객체
     */
    private Category createMockCategory(Long id, String name) {
        Category category = Category.of(name, name.toLowerCase());
        try {
            java.lang.reflect.Field idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Category ID 설정 실패", e);
        }
        return category;
    }
}