package com.snow.popin.global.exception;

import com.snow.popin.global.constant.ErrorCode;

public class NotificationException extends RuntimeException {

    public static class InvalidNotificationType extends GeneralException {
        public InvalidNotificationType(String type) {
            super(ErrorCode.VALIDATION_ERROR, "유효하지 않은 알림 타입입니다: " + type);
        }
    }
}
