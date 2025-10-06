package com.snow.popin.global.exception;

import com.snow.popin.global.constant.ErrorCode;

public class RewardException extends RuntimeException {

    // 리워드 옵션 없음
    public static class OptionNotFound extends GeneralException {
        public OptionNotFound() {
            super(ErrorCode.NOT_FOUND, "리워드 옵션을 찾을 수 없습니다.");
        }
    }

    // 다른 미션셋에 속한 옵션
    public static class OptionNotInMissionSet extends GeneralException {
        public OptionNotInMissionSet() {
            super(ErrorCode.CONFLICT, "해당 미션셋에 속하지 않은 리워드 옵션입니다.");
        }
    }

    // 재고 부족
    public static class OutOfStock extends GeneralException {
        public OutOfStock() {
            super(ErrorCode.CONFLICT, "리워드 재고가 모두 소진되었습니다.");
        }
    }

    // 이미 발급받음
    public static class AlreadyClaimed extends GeneralException {
        public AlreadyClaimed() {
            super(ErrorCode.CONFLICT, "이미 리워드를 발급받았습니다.");
        }
    }

    // 발급 내역 없음
    public static class NotIssued extends GeneralException {
        public NotIssued() {
            super(ErrorCode.NOT_FOUND, "아직 발급되지 않은 리워드입니다.");
        }
    }

    // PIN 없음
    public static class NoStaffPin extends GeneralException {
        public NoStaffPin() {
            super(ErrorCode.NOT_FOUND, "해당 미션셋에는 PIN이 설정되어 있지 않습니다.");
        }
    }

    // PIN 인증 실패
    public static class InvalidStaffPin extends GeneralException {
        public InvalidStaffPin() {
            super(ErrorCode.UNAUTHORIZED, "PIN 인증에 실패했습니다.");
        }
    }
}
