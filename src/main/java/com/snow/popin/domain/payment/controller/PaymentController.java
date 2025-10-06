package com.snow.popin.domain.payment.controller;

import com.snow.popin.domain.payment.dto.PaymentRequestDto;
import com.snow.popin.domain.payment.dto.PaymentResponseDto;
import com.snow.popin.domain.payment.service.PaymentService;
import com.snow.popin.domain.popupReservation.service.ReservationService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.constraints.Positive;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/popup-reservations/payment")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final ReservationService reservationService;
    private final UserUtil userUtil;

    /**
     * 팝업 예약 결제 준비
     */
    @PostMapping("/ready")
    public ResponseEntity<PaymentResponseDto> preparePayment(@RequestBody PaymentRequestDto request, HttpSession session) {
        try {
            PaymentResponseDto result;

            // 결제 수단에 따른 분기 처리
            if ("kakao".equals(request.getPaymentMethod())) {
                result = paymentService.prepareKakaoPayment(request.getReservationId());
            } else if ("naver".equals(request.getPaymentMethod())) {
                result = paymentService.prepareNaverPayment(request.getReservationId());
            } else {
                // paymentMethod가 없거나 잘못된 경우 기본적으로 카카오페이
                log.warn("알 수 없는 결제 수단: {}, 카카오페이로 처리합니다.", request.getPaymentMethod());
                result = paymentService.prepareKakaoPayment(request.getReservationId());
            }

            if (result.getSuccess()) {
                session.setAttribute("payment_tid", result.getTid());
                session.setAttribute("payment_reservation_id", request.getReservationId());
                session.setAttribute("payment_method", request.getPaymentMethod());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("결제 준비 실패", e);
            return ResponseEntity.badRequest().body(
                    PaymentResponseDto.failure(e.getMessage())
            );
        }
    }

    // ===== 카카오페이 콜백 =====

    /**
     * 카카오페이 결제 성공 콜백
     */
    @GetMapping("/kakao/success")
    @ResponseBody
    public ResponseEntity<String> kakaoPaySuccess(
            @RequestParam("pg_token") String pgToken,
            HttpSession session) {

        try {
            String tid = (String) session.getAttribute("payment_tid");
            Long reservationId = (Long) session.getAttribute("payment_reservation_id");

            if (tid == null || reservationId == null) {
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentError('세션 정보가 없습니다.'); window.close();</script>"
                );
            }

            boolean success = paymentService.approveKakaoPayment(tid, pgToken, reservationId);

            if (success) {
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentSuccess(" + reservationId + "); window.close();</script>"
                );
            } else {
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentError('결제 승인 실패'); window.close();</script>"
                );
            }

        } catch (Exception e) {
            log.error("결제 승인 처리 실패", e);
            return ResponseEntity.ok(
                    "<script>window.opener.handlePaymentError('결제 처리 중 오류 발생'); window.close();</script>"
            );
        }
    }

    /**
     * 카카오페이 결제 실패/취소 콜백
     */
    @GetMapping("/kakao/fail")
    @ResponseBody
    public ResponseEntity<String> kakaoPayFail() {
        return ResponseEntity.ok(
                "<script>window.opener.handlePaymentError('결제가 실패했습니다.'); window.close();</script>"
        );
    }

    @GetMapping("/kakao/cancel")
    @ResponseBody
    public ResponseEntity<String> kakaoPayCancel() {
        return ResponseEntity.ok(
                "<script>window.opener.handlePaymentError('결제가 취소되었습니다.'); window.close();</script>"
        );
    }

    // ===== 네이버페이 콜백 =====

    /**
     * 네이버페이 결제 성공 콜백
     */
    @PostMapping("/naver/success")
    @ResponseBody
    public ResponseEntity<String> naverPaySuccess(
            @RequestParam("paymentId") String paymentId,
            HttpSession session) {

        try {
            Long reservationId = (Long) session.getAttribute("payment_reservation_id");
            String sessionPaymentId = (String) session.getAttribute("payment_tid");

            log.info("네이버페이 성공 콜백 - Payment ID: {}, Reservation ID: {}", paymentId, reservationId);

            if (sessionPaymentId == null || reservationId == null) {
                log.error("세션 정보 없음 - Payment ID: {}, Reservation ID: {}", sessionPaymentId, reservationId);
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentError('세션 정보가 없습니다.'); window.close();</script>"
                );
            }

            // paymentId 일치 확인
            if (!paymentId.equals(sessionPaymentId)) {
                log.error("Payment ID 불일치 - 세션: {}, 콜백: {}", sessionPaymentId, paymentId);
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentError('결제 정보가 일치하지 않습니다.'); window.close();</script>"
                );
            }

            boolean success = paymentService.approveNaverPayment(paymentId, reservationId);

            if (success) {
                log.info("네이버페이 결제 성공 - Reservation ID: {}", reservationId);
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentSuccess(" + reservationId + "); window.close();</script>"
                );
            } else {
                log.error("네이버페이 승인 실패 - Payment ID: {}", paymentId);
                return ResponseEntity.ok(
                        "<script>window.opener.handlePaymentError('결제 승인 실패'); window.close();</script>"
                );
            }

        } catch (Exception e) {
            log.error("네이버페이 승인 처리 실패", e);
            return ResponseEntity.ok(
                    "<script>window.opener.handlePaymentError('결제 처리 중 오류 발생'); window.close();</script>"
            );
        }
    }

    /**
     * 네이버페이 결제 실패 콜백
     */
    @PostMapping("/naver/fail")
    @ResponseBody
    public ResponseEntity<String> naverPayFail(
            @RequestParam(value = "code", required = false) String errorCode,
            @RequestParam(value = "message", required = false) String errorMessage) {

        log.warn("네이버페이 결제 실패 - Code: {}, Message: {}", errorCode, errorMessage);

        String message = errorMessage != null ? errorMessage : "네이버페이 결제가 실패했습니다.";
        return ResponseEntity.ok(
                "<script>window.opener.handlePaymentError('" + message + "'); window.close();</script>"
        );
    }

    /**
     * 네이버페이 결제 취소 콜백
     */
    @PostMapping("/naver/cancel")
    @ResponseBody
    public ResponseEntity<String> naverPayCancel() {
        log.info("네이버페이 결제 취소");
        return ResponseEntity.ok(
                "<script>window.opener.handlePaymentError('네이버페이 결제가 취소되었습니다.'); window.close();</script>"
        );
    }

    // ===== GET 방식 네이버페이 콜백  =====

    @GetMapping("/naver/success")
    @ResponseBody
    public ResponseEntity<String> naverPaySuccessGet(
            @RequestParam("paymentId") String paymentId,
            HttpSession session) {

        return naverPaySuccess(paymentId, session);
    }

    @GetMapping("/naver/fail")
    @ResponseBody
    public ResponseEntity<String> naverPayFailGet(
            @RequestParam(value = "code", required = false) String errorCode,
            @RequestParam(value = "message", required = false) String errorMessage) {
        return naverPayFail(errorCode, errorMessage);
    }

    @GetMapping("/naver/cancel")
    @ResponseBody
    public ResponseEntity<String> naverPayCancelGet() {
        return naverPayCancel();
    }
    /**
     * 결제 환불 처리
     */
    @PostMapping("/refund/{reservationId}")
    public ResponseEntity<?> processRefund(
            @PathVariable @Positive Long reservationId)
    {

        try {
            log.info("환불 요청: reservationId={}", reservationId);

            boolean success = paymentService.processRefund(reservationId);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "환불이 완료되었습니다.",
                        "reservationId", reservationId
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "환불 처리에 실패했습니다.",
                        "reservationId", reservationId
                ));
            }

        } catch (Exception e) {
            log.error("환불 처리 중 오류 발생: reservationId={}", reservationId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "환불 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    "reservationId", reservationId
            ));
        }
    }

    /**
     * 환불 가능 여부 확인
     */
    @GetMapping("/refund/check/{reservationId}")
    public ResponseEntity<?> checkRefundable(@PathVariable @Positive Long reservationId) {
        try {
            User currentUser = userUtil.getCurrentUser();
            boolean isRefundable = reservationService.isRefundable(reservationId, currentUser);

            return ResponseEntity.ok(Map.of(
                    "refundable", isRefundable,
                    "reservationId", reservationId
            ));

        } catch (Exception e) {
            log.error("환불 가능 여부 확인 중 오류: reservationId={}", reservationId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "refundable", false,
                    "message", e.getMessage()
            ));
        }
    }
}