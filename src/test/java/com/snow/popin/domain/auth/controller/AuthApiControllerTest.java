package com.snow.popin.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.auth.dto.*;
import com.snow.popin.domain.auth.service.AuthService;
import com.snow.popin.domain.category.dto.CategoryResponseDto;
import com.snow.popin.domain.category.service.CategoryService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.jwt.JwtUtil;
import com.snow.popin.global.config.SecurityConfig;
import com.snow.popin.global.jwt.JwtFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API 컨트롤러 - 인증 (로그인/로그아웃/회원가입)")
@WebMvcTest(
        controllers = AuthApiController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtUtil.class)
        }
)
class AuthApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtUtil jwtUtil;

    private LoginRequest validLoginRequest;
    private LoginResponse mockLoginResponse;
    private SignupRequest validSignupRequest;
    private SignupRequest signupRequestWithInterests;
    private SignupResponse mockSignupResponse;
    private List<CategoryResponseDto> mockCategories;

    @BeforeEach
    void setUp() {
        // 로그인 테스트 데이터
        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("password123");

        mockLoginResponse = LoginResponse.builder()
                .accessToken("jwt-access-token")
                .tokenType("Bearer")
                .userId(1L)
                .email("test@example.com")
                .name("테스트유저")
                .role("USER")
                .build();

        // 회원가입 테스트 데이터 (관심사 없음)
        validSignupRequest = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .build();

        // 회원가입 테스트 데이터 (관심사 포함)
        signupRequestWithInterests = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .interests(Arrays.asList("음식", "여행"))
                .build();

        mockSignupResponse = SignupResponse.builder()
                .success(true)
                .message("회원가입이 완료되었습니다.")
                .email("newuser@example.com")
                .name("신규유저")
                .nickname("뉴비")
                .build();

        // 카테고리 테스트 데이터
        mockCategories = Arrays.asList(
                CategoryResponseDto.builder().id(1L).name("패션").slug("fashion").build(),
                CategoryResponseDto.builder().id(2L).name("반려동물").slug("pet").build(),
                CategoryResponseDto.builder().id(3L).name("게임").slug("game").build()
        );
    }

    // ================ 회원가입 테스트 ================

    @DisplayName("[API] 회원가입 성공 - 관심사 없음")
    @Test
    void givenValidSignupRequest_whenSignup_thenReturnsSuccess() throws Exception {
        // Given
        given(authService.signup(any(SignupRequest.class)))
                .willReturn(mockSignupResponse);

        // When & Then
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.name").value("신규유저"))
                .andExpect(jsonPath("$.nickname").value("뉴비"));
    }

    @DisplayName("[API] 회원가입 성공 - 관심사 포함")
    @Test
    void givenSignupRequestWithInterests_whenSignup_thenReturnsSuccess() throws Exception {
        // Given
        given(authService.signup(any(SignupRequest.class)))
                .willReturn(mockSignupResponse);

        // When & Then
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestWithInterests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @DisplayName("[API] 회원가입 실패 - 이메일 중복")
    @Test
    void givenDuplicateEmail_whenSignup_thenReturnsBadRequest() throws Exception {
        // Given
        willThrow(new GeneralException(ErrorCode.DUPLICATE_EMAIL))
                .given(authService)
                .signup(any(SignupRequest.class));

        // When & Then
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.DUPLICATE_EMAIL.getCode()));
    }

    @DisplayName("[API] 회원가입 실패 - 관심사 개수 초과")
    @Test
    void givenTooManyInterests_whenSignup_thenReturnsBadRequest() throws Exception {
        // Given
        SignupRequest requestWithTooManyInterests = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .interests(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                .build();

        willThrow(new GeneralException(ErrorCode.BAD_REQUEST, "관심사는 최대 10개까지 선택할 수 있습니다."))
                .given(authService)
                .signup(any(SignupRequest.class));

        // When & Then
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithTooManyInterests)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.getCode()));
    }

    @DisplayName("[API] 회원가입 실패 - 비밀번호 불일치")
    @Test
    void givenMismatchedPasswords_whenSignup_thenReturnsBadRequest() throws Exception {
        // Given
        SignupRequest invalidRequest = SignupRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .passwordConfirm("differentPassword")
                .name("신규유저")
                .nickname("뉴비")
                .phone("010-1234-5678")
                .build();

        willThrow(new GeneralException(ErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다."))
                .given(authService)
                .signup(any(SignupRequest.class));

        // When & Then
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.getCode()));
    }

    @DisplayName("[API] 이메일 중복 확인 - 사용 가능")
    @Test
    void givenAvailableEmail_whenCheckEmailDuplicate_thenReturnsAvailable() throws Exception {
        // Given
        given(authService.emailExists(anyString())).willReturn(false);

        // When & Then
        mvc.perform(get("/api/auth/check-email")
                        .param("email", "available@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.exists").value(false));
    }

    @DisplayName("[API] 이메일 중복 확인 - 이미 사용 중")
    @Test
    void givenExistingEmail_whenCheckEmailDuplicate_thenReturnsNotAvailable() throws Exception {
        // Given
        given(authService.emailExists(anyString())).willReturn(true);

        // When & Then
        mvc.perform(get("/api/auth/check-email")
                        .param("email", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.exists").value(true));
    }

    @DisplayName("[API] 닉네임 중복 확인 - 사용 가능")
    @Test
    void givenAvailableNickname_whenCheckNicknameDuplicate_thenReturnsAvailable() throws Exception {
        // Given
        given(authService.nicknameExists(anyString())).willReturn(false);

        // When & Then
        mvc.perform(get("/api/auth/check-nickname")
                        .param("nickname", "사용가능닉네임"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.exists").value(false));
    }

    @DisplayName("[API] 닉네임 중복 확인 - 이미 사용 중")
    @Test
    void givenExistingNickname_whenCheckNicknameDuplicate_thenReturnsNotAvailable() throws Exception {
        // Given
        given(authService.nicknameExists(anyString())).willReturn(true);

        // When & Then
        mvc.perform(get("/api/auth/check-nickname")
                        .param("nickname", "이미사용중"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.exists").value(true));
    }

    // ================ 카테고리 테스트 ================

    @DisplayName("[API] 전체 카테고리 목록 조회 성공")
    @Test
    void whenGetAllCategories_thenReturnsCategoryList() throws Exception {
        // Given
        given(categoryService.getAllCategories()).willReturn(mockCategories);

        // When & Then
        mvc.perform(get("/api/auth/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("패션"))
                .andExpect(jsonPath("$.data[0].slug").value("fashion"))
                .andExpect(jsonPath("$.data[1].name").value("반려동물"))
                .andExpect(jsonPath("$.data[2].name").value("게임"));
    }

    @DisplayName("[API] 카테고리 목록 조회 실패")
    @Test
    void whenGetAllCategoriesFails_thenReturnsError() throws Exception {
        // Given
        willThrow(new RuntimeException("카테고리 조회 실패"))
                .given(categoryService)
                .getAllCategories();

        // When & Then
        mvc.perform(get("/api/auth/categories"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ================ 로그인 테스트 ================

    @DisplayName("[API] 로그인 성공")
    @Test
    void givenValidLoginRequest_whenLogin_thenReturnsLoginResponse() throws Exception {
        // Given
        given(authService.login(any(LoginRequest.class)))
                .willReturn(mockLoginResponse);

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트유저"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @DisplayName("[API] 로그인 실패 - 존재하지 않는 사용자")
    @Test
    void givenNonExistentUser_whenLogin_thenReturnsLoginFailed() throws Exception {
        // Given
        willThrow(new GeneralException(ErrorCode.LOGIN_FAILED))
                .given(authService)
                .login(any(LoginRequest.class));

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.LOGIN_FAILED.getCode()));
    }

    @DisplayName("[API] 로그인 실패 - 잘못된 비밀번호")
    @Test
    void givenWrongPassword_whenLogin_thenReturnsLoginFailed() throws Exception {
        // Given
        LoginRequest wrongPasswordRequest = new LoginRequest();
        wrongPasswordRequest.setEmail("test@example.com");
        wrongPasswordRequest.setPassword("wrongPassword");

        willThrow(new GeneralException(ErrorCode.LOGIN_FAILED))
                .given(authService)
                .login(any(LoginRequest.class));

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.LOGIN_FAILED.getCode()));
    }

    // ================ 로그아웃 테스트 ================

    @DisplayName("[API] 로그아웃 성공")
    @Test
    void givenValidToken_whenLogout_thenReturnsSuccess() throws Exception {
        // Given
        LogoutRequest logoutRequest = new LogoutRequest("valid-token", null);
        LogoutResponse logoutResponse = LogoutResponse.success("로그아웃이 완료되었습니다.");

        given(authService.logout(any(LogoutRequest.class), any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .willReturn(logoutResponse);

        // When & Then
        mvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));
    }

    @DisplayName("[API] 로그아웃 성공 - 빈 요청")
    @Test
    void givenEmptyRequest_whenLogout_thenReturnsSuccess() throws Exception {
        // Given
        LogoutResponse logoutResponse = LogoutResponse.success("로그아웃이 완료되었습니다.");

        given(authService.logout(any(LogoutRequest.class), any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .willReturn(logoutResponse);

        // When & Then
        mvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ================ 헬퍼 메서드 ================

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