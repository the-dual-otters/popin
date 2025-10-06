package com.snow.popin.global.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("에러 처리 - 에러 코드")
class ErrorCodeTest {

    @DisplayName("예외를 받으면, 기본 메시지 + 예외 메시지를 연결한 상세 메시지를 반환한다")
    @MethodSource
    @ParameterizedTest(name = "[{index}] {0}")
    void givenException_whenGetDetailedMessage_thenReturnsFormatted(ErrorCode sut) {
        // Given
        Exception e = new Exception("This is test message.");

        // When
        String actual = sut.getMessage();

        // Then
        assertThat(actual).isEqualTo(sut.getMessage() + " " + e.getMessage());
    }

    static Stream<Arguments> givenException_whenGetDetailedMessage_thenReturnsFormatted() {
        return Stream.of(
                arguments(ErrorCode.OK),
                arguments(ErrorCode.BAD_REQUEST),
                arguments(ErrorCode.NOT_FOUND),
                arguments(ErrorCode.UNAUTHORIZED),
                arguments(ErrorCode.ACCESS_DENIED),
                arguments(ErrorCode.INTERNAL_ERROR)
        );
    }

    @DisplayName("커스텀 메시지를 주면 withMessage가 공백/널 처리 후 반환한다")
    @MethodSource
    @ParameterizedTest(name = "[{index}] input=\"{0}\" => expected=\"{1}\"")
    void givenCustomMessage_whenWithMessage_thenReturnsExpected(String input, String expected) {
        // When
        String actual = ErrorCode.INTERNAL_ERROR.withMessage(input);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> givenCustomMessage_whenWithMessage_thenReturnsExpected() {
        return Stream.of(
                arguments(null, ErrorCode.INTERNAL_ERROR.getMessage()),
                arguments("", ErrorCode.INTERNAL_ERROR.getMessage()),
                arguments("   ", ErrorCode.INTERNAL_ERROR.getMessage()),
                arguments("테스트 메시지", "테스트 메시지")
        );
    }

    @DisplayName("toString() 포맷은 'NAME (code)' 이다")
    @Test
    void givenErrorCode_whenToString_thenFormatted() {
        String result = ErrorCode.INTERNAL_ERROR.toString();
        assertThat(result).isEqualTo("INTERNAL_ERROR (20000)");
    }

    @DisplayName("HttpStatus를 ErrorCode로 매핑한다 (fromHttpStatus)")
    @MethodSource
    @ParameterizedTest(name = "[{index}] {0} => {1}")
    void givenHttpStatus_whenFromHttpStatus_thenReturnsCode(HttpStatus httpStatus, ErrorCode expected) {
        ErrorCode actual = ErrorCode.fromHttpStatus(httpStatus);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> givenHttpStatus_whenFromHttpStatus_thenReturnsCode() {
        return Stream.of(
                arguments(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST),
                arguments(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND),
                arguments(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED),
                arguments(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED),
                arguments(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR),
                // 정의 밖 값 및 null은 기본값 INTERNAL_ERROR
                arguments(HttpStatus.ACCEPTED, ErrorCode.INTERNAL_ERROR),
                arguments(null, ErrorCode.INTERNAL_ERROR)
        );
    }

}