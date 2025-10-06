package com.snow.popin.global.exception;

import com.snow.popin.global.constant.ErrorCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final ErrorCode errorCode;

    public GeneralException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GeneralException(ErrorCode errorCode, String message) {
        super(errorCode.withMessage(message));
        this.errorCode = errorCode;
    }

    public GeneralException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    // 기본 ErrorCode 사용하는 경우
    public GeneralException(String message) {
        super(ErrorCode.INTERNAL_ERROR.withMessage(message));
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }
}

