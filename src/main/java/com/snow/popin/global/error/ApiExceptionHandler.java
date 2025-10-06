package com.snow.popin.global.error;

import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.exception.PopupNotFoundException;

import com.snow.popin.global.exception.ReviewException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@RestControllerAdvice(basePackages = "com.snow.popin")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        return handleExceptionInternal(e, ErrorCode.VALIDATION_ERROR, request);
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> general(GeneralException e, WebRequest request) {
        return handleExceptionInternal(e, e.getErrorCode(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        return handleExceptionInternal(e, ErrorCode.INTERNAL_ERROR, request);
    }

    @ExceptionHandler(PopupNotFoundException.class)
    public ResponseEntity<Object> handlePopupNotFound(PopupNotFoundException e, WebRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        ApiErrorResponse errorResponse = ApiErrorResponse.of(errorCode, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * 중복 리뷰 예외 처리 (409 Conflict)
     */
    @ExceptionHandler(ReviewException.DuplicateReview.class)
    public ResponseEntity<Object> handleDuplicateReview(ReviewException.DuplicateReview e) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(ErrorCode.CONFLICT, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * 리뷰 관련 리소스(팝업, 사용자)를 찾을 수 없는 예외 처리 (404 Not Found)
     */
    @ExceptionHandler({ReviewException.PopupNotFound.class, ReviewException.UserNotFound.class})
    public ResponseEntity<Object> handleReviewResourceNotFound(RuntimeException e) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(ErrorCode.NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * 차단된 리뷰 접근 예외 처리 (403 Forbidden)
     */
    @ExceptionHandler(ReviewException.BlockedReview.class)
    public ResponseEntity<Object> handleBlockedReview(ReviewException.BlockedReview e) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(ErrorCode.ACCESS_DENIED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleExceptionInternal(ex, ErrorCode.fromHttpStatus(status), headers, status, request);
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorCode errorCode, WebRequest request) {
        return handleExceptionInternal(e, errorCode, HttpHeaders.EMPTY, errorCode.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternal(
            Exception e, ErrorCode errorCode, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return super.handleExceptionInternal(
                e,
                ApiErrorResponse.of(errorCode, e),
                headers,
                status,
                request
        );
    }
}


