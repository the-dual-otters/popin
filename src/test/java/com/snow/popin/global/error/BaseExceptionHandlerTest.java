package com.snow.popin.global.error;

import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

@DisplayName("핸들러 - 기본 에러 처리")
class BaseExceptionHandlerTest {

    private BaseExceptionHandler sut;

    @BeforeEach
    void setUp() {
        sut = new BaseExceptionHandler();
    }


    @DisplayName("[View] 일반 예외 처리 - 프로젝트 오류 응답")
    @Test
    void givenGeneralException_whenHandlingException_thenReturnsModelAndView() {
        // Given
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        GeneralException e = new GeneralException(errorCode);

        // When
        ModelAndView result = sut.general(e);

        // Then
        assertThat(result)
                .hasFieldOrPropertyWithValue("viewName", "error")
                .extracting(ModelAndView::getModel, as(MAP))
                .containsEntry("statusCode", errorCode.getHttpStatus().value())
                .containsEntry("errorCode", errorCode)
                .containsEntry("message", errorCode.getMessage());
    }


    @DisplayName("[View] 기타 예외 처리 - 알 수 없는 오류 응답")
    @Test
    void givenOtherException_whenHandlingException_thenReturnsModelAndView() {
        // Given
        Exception e = new Exception("This is error message.");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.FORBIDDEN.value());

        // When
        ModelAndView result = sut.exception(e, response);

        // Then
        ErrorCode expectedErrorCode = ErrorCode.fromHttpStatus(HttpStatus.FORBIDDEN);
        assertThat(result)
                .hasFieldOrPropertyWithValue("viewName", "error")
                .extracting(ModelAndView::getModel, as(MAP))
                .containsEntry("statusCode", HttpStatus.FORBIDDEN.value())
                .containsEntry("errorCode", expectedErrorCode)
                .containsEntry("message", expectedErrorCode.getMessage());
    }

}