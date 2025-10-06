package com.snow.popin.global.error;

import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("핸들러 - API 에러 처리")
class ApiExceptionHandlerTest {

    private ApiExceptionHandler sut;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        sut = new ApiExceptionHandler();
        webRequest = new ServletWebRequest(new MockHttpServletRequest());
    }


    @DisplayName("[API] 검증 예외 처리 - 제약조건 위반 응답")
    @Test
    void givenValidationException_whenHandlingApiException_thenReturnsResponseEntity() {
        // Given: Hibernate 제약조건 위반 예외 (DB 유니크 등)
        ConstraintViolationException e =
                new ConstraintViolationException("constraint violation", new SQLException("duplicate"), "uk_test");

        // When
        ResponseEntity<Object> response = sut.validation(e, webRequest);

        // Then
        assertThat((ApiErrorResponse) response.getBody())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, e));
        assertThat(response)
                .hasFieldOrPropertyWithValue("headers", HttpHeaders.EMPTY)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }


    @DisplayName("[API] 일반 예외 처리 - 프로젝트 오류 응답")
    @Test
    void givenGeneralException_whenHandlingApiException_thenReturnsResponseEntity() {
        // Given
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        GeneralException e = new GeneralException(errorCode);

        // When
        ResponseEntity<Object> response = sut.general(e, webRequest);

        // Then
        assertThat((ApiErrorResponse) response.getBody())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(ApiErrorResponse.of(errorCode, e));
        assertThat(response)
                .hasFieldOrPropertyWithValue("headers", HttpHeaders.EMPTY)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DisplayName("[API] 스프링 예외 처리 - 프레임워크 오류 응답")
    @MethodSource
    @ParameterizedTest(name = "[{index}] {0} => {1}")
    void givenSpringException_whenHandlingApiException_thenReturnsResponseEntity(Exception e, HttpStatus httpStatus) {
        // Given
        HttpHeaders headers = HttpHeaders.EMPTY;
        ErrorCode errorCode = ErrorCode.fromHttpStatus(httpStatus);

        // When: ResponseEntityExceptionHandler의 protected 메서드를 경유
        ResponseEntity<Object> response =
                sut.handleExceptionInternal(e, null, headers, httpStatus, webRequest);

        // Then
        assertThat((ApiErrorResponse) response.getBody())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(ApiErrorResponse.of(errorCode, e));
        assertThat(response)
                .hasFieldOrPropertyWithValue("headers", headers)
                .hasFieldOrPropertyWithValue("statusCode", httpStatus)
                .extracting(ResponseEntity::getBody)
                .hasFieldOrPropertyWithValue("message", errorCode.getMessage());
    }

    static Stream<Arguments> givenSpringException_whenHandlingApiException_thenReturnsResponseEntity() {
        String msg = "test message";

        return Stream.of(
                // 메서드 미지원
                arguments(new HttpRequestMethodNotSupportedException(HttpMethod.POST.name(), new String[]{"GET"}), HttpStatus.METHOD_NOT_ALLOWED),
                // 미디어 타입 관련
                arguments(new HttpMediaTypeNotSupportedException(msg), HttpStatus.UNSUPPORTED_MEDIA_TYPE),
                // Not Acceptable (버전에 따라 생성자가 다를 수 있어 문자열 생성자 사용)
                arguments(new HttpMediaTypeNotAcceptableException(msg), HttpStatus.NOT_ACCEPTABLE),
                // 요청 바인딩 오류
                arguments(new ServletRequestBindingException(msg), HttpStatus.BAD_REQUEST),
                // 메시지 변환/쓰기 오류
                arguments(new HttpMessageNotWritableException(msg), HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }


    @DisplayName("[API] 기타 예외 처리 - 알 수 없는 오류 응답 데이터 정의")
    @Test
    void givenOtherException_whenHandlingApiException_thenReturnsResponseEntity() {
        // Given
        Exception e = new Exception("unknown");

        // When
        ResponseEntity<Object> response = sut.exception(e, webRequest);

        // Then
        assertThat((ApiErrorResponse) response.getBody())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, e));
        assertThat(response)
                .hasFieldOrPropertyWithValue("headers", HttpHeaders.EMPTY)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}