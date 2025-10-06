package com.snow.popin.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ReviewException extends RuntimeException {

    public ReviewException(String message) {
        super(message);
    }

    public ReviewException(String message, Throwable cause) {
        super(message, cause);
    }

    // 리뷰를 찾을 수 없는 경우
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ReviewNotFound extends ReviewException {
        public ReviewNotFound(Long reviewId) {
            super(String.format("리뷰를 찾을 수 없습니다. (ID: %d)", reviewId));
        }
    }

    // 리뷰 수정/삭제 권한이 없는 경우
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDenied extends ReviewException {
        public AccessDenied() {
            super("리뷰에 대한 권한이 없습니다.");
        }
    }

    // 중복 리뷰 작성 시도
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateReview extends ReviewException {
        public DuplicateReview(Long popupId) {
            super(String.format("이미 해당 팝업에 리뷰를 작성했습니다. (팝업ID: %d)", popupId));
        }
    }

    // 차단된 리뷰에 대한 작업 시도
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class BlockedReview extends ReviewException {
        public BlockedReview() {
            super("차단된 리뷰는 수정하거나 삭제할 수 없습니다.");
        }
    }

    // 존재하지 않는 팝업에 리뷰 작성 시도
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class PopupNotFound extends ReviewException {
        public PopupNotFound(Long popupId) {
            super(String.format("존재하지 않는 팝업입니다. (ID: %d)", popupId));
        }
    }

    // 존재하지 않는 사용자
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFound extends ReviewException {
        public UserNotFound(Long userId) {
            super(String.format("존재하지 않는 사용자입니다. (ID: %d)", userId));
        }
    }
}