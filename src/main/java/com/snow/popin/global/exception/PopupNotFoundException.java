package com.snow.popin.global.exception;

import com.snow.popin.global.constant.ErrorCode;

public class PopupNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Long popupId;

    public PopupNotFoundException(Long popupId) {
        super("팝업을 찾을 수 없습니다: " + popupId);
        this.errorCode = ErrorCode.POPUP_NOT_FOUND; // ErrorCode enum에 추가 필요
        this.popupId = popupId;
    }

    public PopupNotFoundException(String message) {
        super(message);
        this.errorCode = ErrorCode.POPUP_NOT_FOUND;
        this.popupId = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Long getPopupId() {
        return popupId;
    }
}
