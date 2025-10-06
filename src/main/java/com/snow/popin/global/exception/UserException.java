package com.snow.popin.global.exception;

import com.snow.popin.global.constant.ErrorCode;

public class UserException extends RuntimeException{

    // 유저를 찾을 수 없음
    public static class UserNotFound extends GeneralException {
        public UserNotFound(Long userId) {
            super(ErrorCode.NOT_FOUND, "해당 유저를 찾을 수 없습니다. ID=" + userId);
        }

        public UserNotFound(String username) {
            super(ErrorCode.NOT_FOUND, "해당 유저를 찾을 수 없습니다. username=" + username);
        }
    }

    // 인증되지 않은 요청
    public static class Unauthorized extends GeneralException {
        public Unauthorized() {
            super(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다.");
        }

        public Unauthorized(String message) {
            super(ErrorCode.UNAUTHORIZED, message);
        }
    }

    // 접근 권한 없음
    public static class Forbidden extends GeneralException {
        public Forbidden() {
            super(ErrorCode.ACCESS_DENIED, "접근 권한이 없습니다.");
        }

        public Forbidden(String message) {
            super(ErrorCode.ACCESS_DENIED, message);
        }
    }

    // 중복된 사용자 (이메일/닉네임 등)
    public static class DuplicateUser extends GeneralException {
        public DuplicateUser(String field, String value) {
            super(ErrorCode.CONFLICT, String.format("이미 존재하는 사용자입니다. (%s=%s)", field, value));
        }
    }
}
