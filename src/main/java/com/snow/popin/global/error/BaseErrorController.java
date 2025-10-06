package com.snow.popin.global.error;


import com.snow.popin.global.constant.ErrorCode;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
public class BaseErrorController implements ErrorController {

    @RequestMapping(path = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletResponse response) {
        HttpStatus httpStatus = getHttpStatus(response);
        ErrorCode errorCode = ErrorCode.fromHttpStatus(httpStatus);

        return new ModelAndView(
                "error/error",
                Map.of(
                        "statusCode", httpStatus.value(),
                        "errorCode", errorCode,
                        "message", errorCode.withMessage(httpStatus.getReasonPhrase())
                ),
                httpStatus
        );
    }

    @RequestMapping("/error")
    public ResponseEntity<ApiErrorResponse> error(HttpServletResponse response) {
        HttpStatus httpStatus = getHttpStatus(response);
        ErrorCode errorCode = ErrorCode.fromHttpStatus(httpStatus);

        return ResponseEntity
                .status(httpStatus)
                .body(ApiErrorResponse.of(errorCode));
    }

    private HttpStatus getHttpStatus(HttpServletResponse response) {
        HttpStatus httpStatus = HttpStatus.valueOf(response.getStatus());

        // 정상 상태 코드지만 예외가 전달된 경우 방어 처리
        if (httpStatus == HttpStatus.OK) {
            httpStatus = HttpStatus.FORBIDDEN;
        }

        return httpStatus;
    }
}