package com.snow.popin.global.exception;

import com.snow.popin.global.constant.ErrorCode;


public class MissionException extends RuntimeException {

  // 인증 실패
  public static class Unauthorized extends GeneralException {
    public Unauthorized(String message) {
      super(ErrorCode.UNAUTHORIZED, message);
    }
  }

  // 미션을 찾을 수 없음
  public static class MissionNotFound extends GeneralException {
    public MissionNotFound() {
      super(ErrorCode.NOT_FOUND, "해당 미션을 찾을 수 없습니다.");
    }
  }

  // 미션셋을 찾을 수 없음
  public static class MissionSetNotFound extends GeneralException {
    public MissionSetNotFound() {
      super(ErrorCode.NOT_FOUND, "해당 미션셋을 찾을 수 없습니다.");
    }
  }

  // 미션 정답 불일치
  public static class InvalidAnswer extends GeneralException {
    public InvalidAnswer() {
      super(ErrorCode.VALIDATION_ERROR, "제출한 답이 올바르지 않습니다.");
    }
  }

  // 필수 미션을 완료하지 않음
  public static class MissionNotCleared extends GeneralException {
    public MissionNotCleared() {
      super(ErrorCode.VALIDATION_ERROR, "필수 미션을 완료하지 않았습니다.");
    }
  }

  // 종료된 미션셋
  public static class MissionSetDisabled extends GeneralException {
    public MissionSetDisabled() {
      super(ErrorCode.NOT_FOUND, "종료된 미션은 이용하실수 없습니다.");
    }
  }
}
