package com.snow.popin.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.function.Predicate;


// TODO: 코드 체계가 일관되지 않고 규칙이 섞여있음.
//       (10000: Client/Request, 20000: Server/Internal, 40xxx: Domain 등)
//       추후 정리 필요
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    OK(0, HttpStatus.OK, "정상"),

    BAD_REQUEST(10000, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    SPRING_BAD_REQUEST(10001, HttpStatus.BAD_REQUEST, "스프링 오류: 잘못된 요청입니다."),
    VALIDATION_ERROR(10002, HttpStatus.BAD_REQUEST, "유효성 검사 오류입니다."),
    NOT_FOUND(10003, HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(10004, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(10005, HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_EMAIL(10006,  HttpStatus.BAD_REQUEST, "중복된 이메일입니다."),

    // 역할 승격 관련 에러 코드들
    DUPLICATE_ROLE_UPGRADE_REQUEST(10007, HttpStatus.BAD_REQUEST, "이미 제출된 계정 전환 요청이 존재합니다."),
    INVALID_ROLE_UPGRADE_REQUEST(10008, HttpStatus.BAD_REQUEST, "이미 해당 역할이거나 더 높은 역할을 보유하고 있습니다."),
    ROLE_UPGRADE_REQUEST_NOT_FOUND(10009, HttpStatus.NOT_FOUND, "계정 전환 요청을 찾을 수 없습니다."),
    INVALID_REQUEST_STATUS(10010, HttpStatus.BAD_REQUEST, "처리할 수 없는 요청 상태입니다."),


    INTERNAL_ERROR(20000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    SPRING_INTERNAL_ERROR(20001, HttpStatus.INTERNAL_SERVER_ERROR, "스프링 오류: 서버 내부 오류입니다."),
    DATA_ACCESS_ERROR(20002, HttpStatus.INTERNAL_SERVER_ERROR, "데이터 접근 오류입니다."),

    INVALID_TOKEN(20003, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(20004, HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
    UNAUTHORIZED(20005, HttpStatus.UNAUTHORIZED, "권한이 없습니다."),
    CONFLICT(20006, HttpStatus.CONFLICT, "리소스 충돌이 발생했습니다."),

    FILE_UPLOAD_ERROR(20007, HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),

    POPUP_NOT_FOUND(40404, HttpStatus.NOT_FOUND, "팝업을 찾을 수 없습니다"),
    POPUP_ACCESS_DENIED(40304, HttpStatus.FORBIDDEN, "팝업 접근 권한이 없습니다"),
    POPUP_ALREADY_ENDED(40004, HttpStatus.BAD_REQUEST, "이미 종료된 팝업입니다"),
    ;

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;


    public String withMessage(String customMessage) {
        return Optional.ofNullable(customMessage)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.message);
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", this.name(), this.getCode());
    }

    public static ErrorCode fromHttpStatus(HttpStatus httpStatus) {
        if (httpStatus == HttpStatus.BAD_REQUEST) {
            return BAD_REQUEST;
        } else if (httpStatus == HttpStatus.NOT_FOUND) {
            return NOT_FOUND;
        } else if (httpStatus == HttpStatus.UNAUTHORIZED) {
            return UNAUTHORIZED;
        } else if (httpStatus == HttpStatus.FORBIDDEN) {
            return ACCESS_DENIED;
        } else if (httpStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
            return INTERNAL_ERROR;
        } else {
            return INTERNAL_ERROR; // 기본값
        }
    }
}
