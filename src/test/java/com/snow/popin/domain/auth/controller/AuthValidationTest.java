package com.snow.popin.domain.auth.controller;

import com.snow.popin.domain.auth.dto.LoginRequest;
import com.snow.popin.domain.auth.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("인증 요청 유효성 검증 테스트")
class AuthValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ================ 회원가입 검증 테스트 ================

    @DisplayName("유효한 회원가입 요청 - 검증 통과 (관심사 없음)")
    @Test
    void givenValidSignupRequest_whenValidate_thenNoViolations() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @DisplayName("유효한 회원가입 요청 - 검증 통과 (관심사 포함)")
    @Test
    void givenValidSignupRequestWithInterests_whenValidate_thenNoViolations() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .interests(Arrays.asList("패션", "게임", "여행"))
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @DisplayName("회원가입 - 관심사 개수 제한 검증 (최대 10개)")
    @Test
    void givenTooManyInterests_whenValidate_thenHasViolations() {
        // Given - 11개의 관심사 (제한 초과)
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .interests(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("interests") &&
                        v.getMessage().contains("10개"));
    }

    @DisplayName("회원가입 - 관심사 빈 리스트는 허용")
    @Test
    void givenEmptyInterestsList_whenValidate_thenNoViolations() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .interests(Collections.emptyList())
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @DisplayName("회원가입 - 필수 필드가 null이거나 빈 값일 때 검증 실패")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void givenInvalidSignupFields_whenValidate_thenHasViolations(String invalidValue) {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email(invalidValue) // 필수 필드에 유효하지 않은 값
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @DisplayName("회원가입 - 잘못된 이메일 형식일 때 검증 실패")
    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test.example.com", "test@.com"})
    void givenInvalidEmailFormat_whenSignupValidate_thenHasViolations(String invalidEmail) {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email(invalidEmail)
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @DisplayName("회원가입 - 비밀번호 패턴 검증")
    @ParameterizedTest
    @ValueSource(strings = {
            "password", // 영문만
            "12345678", // 숫자만
            "@@@@@@@@", // 특수문자만
            "Pass123",  // 8자 미만
            "password123", // 특수문자 없음
            "PASSWORD123!", // 소문자 없음
    })
    void givenInvalidPasswordPattern_whenSignupValidate_thenHasViolations(String invalidPassword) {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password(invalidPassword)
                .passwordConfirm(invalidPassword)
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("password") &&
                        v.getMessage().contains("영문, 숫자, 특수문자"));
    }

    @DisplayName("회원가입 - 닉네임 패턴 검증")
    @ParameterizedTest
    @ValueSource(strings = {
            "a", // 너무 짧음
            "abcdefghijklmnopqrstuvwxyz", // 너무 긺
            "nick@name", // 특수문자 포함
            "nick name", // 공백 포함
            "닉네임!", // 특수문자 포함
    })
    void givenInvalidNicknamePattern_whenSignupValidate_thenHasViolations(String invalidNickname) {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname(invalidNickname)
                .phone("010-1234-5678")
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @DisplayName("회원가입 - 전화번호 패턴 검증")
    @ParameterizedTest
    @ValueSource(strings = {
            "01012345678", // 하이픈 없음
            "010-12345-678", // 잘못된 형식
            "011-1234-5678", // 010이 아님
            "010-123-5678", // 자릿수 부족
    })
    void givenInvalidPhonePattern_whenSignupValidate_thenHasViolations(String invalidPhone) {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone(invalidPhone)
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @DisplayName("회원가입 - 이름 길이 제한 검증")
    @ParameterizedTest
    @ValueSource(strings = {
            "a", // 너무 짧음 (2자 미만)
            "abcdefghijklmnopqrstuvwxyz" // 너무 긺 (10자 초과)
    })
    void givenInvalidNameLength_whenSignupValidate_thenHasViolations(String invalidName) {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name(invalidName)
                .nickname("테스터123")
                .phone("010-1234-5678")
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    // ================ 로그인 검증 테스트 ================

    @DisplayName("유효한 로그인 요청 - 검증 통과")
    @Test
    void givenValidLoginRequest_whenValidate_thenNoViolations() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @DisplayName("로그인 - 이메일이 null이거나 빈 값일 때 검증 실패")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void givenInvalidEmail_whenLoginValidate_thenHasViolations(String invalidEmail) {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(invalidEmail);
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @DisplayName("로그인 - 비밀번호가 null이거나 빈 값일 때 검증 실패")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void givenInvalidPassword_whenLoginValidate_thenHasViolations(String invalidPassword) {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword(invalidPassword);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @DisplayName("로그인 - 잘못된 이메일 형식일 때 검증 실패")
    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test..test@example.com"})
    void givenInvalidEmailFormat_whenLoginValidate_thenHasViolations(String invalidEmail) {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(invalidEmail);
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    // ================ 복합 검증 테스트 ================

    @DisplayName("회원가입 - 모든 필드가 유효하지 않을 때 다중 검증 실패")
    @Test
    void givenAllInvalidFields_whenSignupValidate_thenHasMultipleViolations() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("invalid-email")
                .password("weak") // 패턴 불일치
                .passwordConfirm("different") // 비밀번호와 다름
                .name("") // 빈 값
                .nickname("@#$") // 패턴 불일치
                .phone("123-456-789") // 패턴 불일치
                .interests(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")) // 개수 초과
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.size()).isGreaterThan(1); // 다중 검증 실패

        // 각 필드별로 검증 실패가 있는지 확인
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("interests"));
    }

    @DisplayName("회원가입 - 경계값 테스트 (정확히 10개 관심사)")
    @Test
    void givenExactlyTenInterests_whenValidate_thenNoViolations() {
        // Given - 정확히 10개의 관심사
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .nickname("테스터123")
                .phone("010-1234-5678")
                .interests(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @DisplayName("회원가입 - 경계값 테스트 (최소/최대 길이)")
    @Test
    void givenBoundaryValues_whenValidate_thenNoViolations() {
        // Given - 각 필드의 최소/최대 경계값
        SignupRequest request = SignupRequest.builder()
                .email("a@b.co") // 최소 길이
                .password("Aa1@1234") // 최소 길이 (8자)
                .passwordConfirm("Aa1@1234")
                .name("김김") // 최소 길이 (2자)
                .nickname("닉닉") // 최소 길이 (2자)
                .phone("010-1234-5678") // 표준 형식
                .build();

        // When
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }
}