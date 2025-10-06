package com.snow.popin.global.error;

import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@ControllerAdvice(assignableTypes = {Controller.class})
public class BaseExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ModelAndView general(GeneralException e) {
        ErrorCode errorCode = e.getErrorCode();
        return createErrorView(errorCode.getHttpStatus(), errorCode, errorCode.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView exception(Exception e, HttpServletResponse response) {
        HttpStatus httpStatus = getHttpStatus(response);
        ErrorCode errorCode = ErrorCode.fromHttpStatus(httpStatus);
        return createErrorView(httpStatus, errorCode, errorCode.getMessage());
    }

    private HttpStatus getHttpStatus(HttpServletResponse response) {
        HttpStatus httpStatus = HttpStatus.valueOf(response.getStatus());
        return httpStatus == HttpStatus.OK ? HttpStatus.FORBIDDEN : httpStatus;
    }

    private ModelAndView createErrorView(HttpStatus httpStatus, ErrorCode errorCode, String message) {
        return new ModelAndView(
                "error/error",
                Map.of(
                        "statusCode", httpStatus.value(),
                        "errorCode", errorCode,
                        "message", message
                ),
                httpStatus
        );
    }
}