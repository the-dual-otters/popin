package com.snow.popin.global.error;

import com.snow.popin.global.constant.ErrorCode;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiErrorResponse {

    private final Boolean success;
    private final Integer errorCode;
    private final String message;
    private final String timestamp;

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), getCurrentTimestamp());
    }

    public static ApiErrorResponse of(ErrorCode errorCode, Exception e) {
        return new ApiErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), getCurrentTimestamp());
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String customMessage) {
        return new ApiErrorResponse(false, errorCode.getCode(), errorCode.withMessage(customMessage), getCurrentTimestamp());
    }

    public static ApiErrorResponse of(Boolean success, Integer errorCode, String message) {
        return new ApiErrorResponse(success, errorCode, message, getCurrentTimestamp());
    }

    public static ApiErrorResponse success() {
        return new ApiErrorResponse(true, ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), getCurrentTimestamp());
    }

    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
