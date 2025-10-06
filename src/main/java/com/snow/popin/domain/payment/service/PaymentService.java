package com.snow.popin.domain.payment.service;

import com.snow.popin.domain.payment.dto.PaymentResponseDto;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final ReservationRepository reservationRepository;
    private final RestTemplate restTemplate;

    @Value("${kakao.pay.admin.key}")
    private String kakaoPayAdminKey;

    @Value("${naver.pay.client.id:}")
    private String naverPayClientId;

    @Value("${naver.pay.client.secret:}")
    private String naverPayClientSecret;

    /**
     * 카카오페이 결제 준비
     */
    public PaymentResponseDto prepareKakaoPayment(Long reservationId) {
        // 예약 정보 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 입장료 계산
        Integer entryFee = reservation.getPopup().getEntryFee();
        if (entryFee == null || entryFee <= 0) {
            throw new IllegalArgumentException("무료 팝업은 결제가 필요하지 않습니다.");
        }

        int totalAmount = entryFee * reservation.getPartySize();

        // 예약에 결제 정보 설정
        reservation.setPaymentAmount(totalAmount);
        reservationRepository.save(reservation);

        // 카카오페이 API 호출
        String url = "https://kapi.kakao.com/v1/payment/ready";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoPayAdminKey);
        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, Object> params = new HashMap<>();
        params.put("cid", "TC0ONETIME"); // 테스트용 CID
        params.put("partner_order_id", "popup_reservation_" + reservationId);
        params.put("partner_user_id", "user_" + reservation.getUser().getId());
        params.put("item_name",
                "[POPIN] 예약이 완료되었습니다!\n" +
                        "[팝업명] : " + reservation.getPopup().getTitle() + "\n" +
                        "[예약자] : " + reservation.getName() + "\n" +
                        "[인원] : " + reservation.getPartySize() + "명\n" +
                        "[예약 날짜] : " + reservation.getReservationDate().toLocalDate() + "\n" +
                        "[결제 금액] : " + reservation.getPaymentAmount() + "원"
        );
        params.put("quantity", 1);
        params.put("total_amount", totalAmount);
        params.put("vat_amount", totalAmount / 11); // 부가세입니다
        params.put("tax_free_amount", 0);
        params.put("approval_url", "http://localhost:8080/api/popup-reservations/payment/kakao/success");
        params.put("fail_url", "http://localhost:8080/api/popup-reservations/payment/kakao/fail");
        params.put("cancel_url", "http://localhost:8080/api/popup-reservations/payment/kakao/cancel");

        String body = buildFormData(params);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            String tid = (String) result.get("tid");
            reservation.setPaymentTid(tid);
            reservationRepository.save(reservation);

            return PaymentResponseDto.success(
                    (String) result.get("next_redirect_pc_url"),
                    tid,
                    "카카오페이 결제 준비 완료"
            );

        } catch (Exception e) {
            log.error("카카오페이 결제 준비 실패", e);
            reservation.markPaymentFailed(e.getMessage());
            reservationRepository.save(reservation);

            return PaymentResponseDto.failure("카카오페이 결제 준비 실패: " + e.getMessage());
        }
    }

    /**
     * 네이버페이 결제 준비
     */
    public PaymentResponseDto prepareNaverPayment(Long reservationId) {
        log.info("네이버페이 결제 준비 시작 - 예약 ID: {}", reservationId);

        // 네이버페이 설정 확인
        if (naverPayClientId == null || naverPayClientId.isEmpty() ||
                naverPayClientSecret == null || naverPayClientSecret.isEmpty()) {
            log.error("네이버페이 설정이 완료되지 않았습니다.");
            return PaymentResponseDto.failure("네이버페이 설정이 완료되지 않았습니다.");
        }

        // 예약 정보 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        Integer entryFee = reservation.getPopup().getEntryFee();
        if (entryFee == null || entryFee <= 0) {
            throw new IllegalArgumentException("무료 팝업은 결제가 필요하지 않습니다.");
        }

        int totalAmount = entryFee * reservation.getPartySize();

        // 예약에 결제 정보 설정
        reservation.setPaymentAmount(totalAmount);
        reservationRepository.save(reservation);

        try {
            String url = "https://dev.apis.naver.com/naverpay-partner/naverpay/payments/v2.2/reserve";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json;charset=UTF-8");
            headers.set("X-Naver-Client-Id", naverPayClientId);
            headers.set("X-Naver-Client-Secret", naverPayClientSecret);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("merchantPayKey", "popup_reservation_" + reservationId);
            requestBody.put("productName", reservation.getPopup().getTitle() + " 입장료");
            requestBody.put("totalPayAmount", totalAmount);
            requestBody.put("taxScopeAmount", totalAmount);
            requestBody.put("taxExScopeAmount", 0);
            requestBody.put("returnUrl", "http://localhost:8080/api/popup-reservations/payment/naver/success");

            Map<String, Object> productItem = new HashMap<>();
            productItem.put("categoryType", "ETC");
            productItem.put("categoryId", "ETC");
            productItem.put("uid", "popup_" + reservation.getPopup().getId());
            productItem.put("name", reservation.getPopup().getTitle() + " 입장료");
            productItem.put("payReferrer", "popup_reservation");
            productItem.put("count", 1);
            productItem.put("sellPrice", entryFee);
            productItem.put("payAmount", totalAmount);

            requestBody.put("productItems", new Object[]{productItem});

            Map<String, Object> buyerInfo = new HashMap<>();
            buyerInfo.put("buyerId", "user_" + reservation.getUser().getId());
            buyerInfo.put("buyerName", reservation.getName());
            buyerInfo.put("buyerTel", reservation.getPhone());
            requestBody.put("buyerInfo", buyerInfo);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            if (result != null && "Success".equals(result.get("code"))) {
                Map<String, Object> body = (Map<String, Object>) result.get("body");
                String paymentId = (String) body.get("paymentId");
                String paymentUrl = (String) body.get("paymentUrl");

                // 네이버페이는 paymentId 사용
                reservation.setPaymentTid(paymentId);
                reservationRepository.save(reservation);

                log.info("네이버페이 결제 준비 완료 - 예약 ID: {}, Payment ID: {}", reservationId, paymentId);

                return PaymentResponseDto.success(
                        paymentUrl,
                        paymentId,
                        "네이버페이 결제 준비 완료"
                );
            } else {
                String errorMessage = result != null ? (String) result.get("message") : "알 수 없는 오류";
                log.error("네이버페이 API 오류: {}", errorMessage);

                reservation.markPaymentFailed(errorMessage);
                reservationRepository.save(reservation);

                return PaymentResponseDto.failure("네이버페이 결제 준비 실패: " + errorMessage);
            }

        } catch (Exception e) {
            log.error("네이버페이 결제 준비 실패", e);
            reservation.markPaymentFailed(e.getMessage());
            reservationRepository.save(reservation);

            return PaymentResponseDto.failure("네이버페이 결제 준비 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오페이 결제 승인
     */
    public boolean approveKakaoPayment(String tid, String pgToken, Long reservationId) {
        try {
            // 예약 조회
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

            // 카카오페이 승인 API 호출
            String url = "https://kapi.kakao.com/v1/payment/approve";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoPayAdminKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            Map<String, Object> params = new HashMap<>();
            params.put("cid", "TC0ONETIME");
            params.put("tid", tid);
            params.put("partner_order_id", "popup_reservation_" + reservationId);
            params.put("partner_user_id", "user_" + reservation.getUser().getId());
            params.put("pg_token", pgToken);

            String body = buildFormData(params);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            // 결제 완료 처리
            reservation.markAsPaid("KAKAO_PAY", tid);
            reservationRepository.save(reservation);

            log.info("결제 승인 완료: 예약 ID {}, TID {}", reservationId, tid);
            return true;

        } catch (Exception e) {
            log.error("카카오페이 결제 승인 실패", e);

            // 예약 조회 후 실패 처리
            reservationRepository.findById(reservationId).ifPresent(reservation -> {
                reservation.markPaymentFailed(e.getMessage());
                reservationRepository.save(reservation);
            });

            return false;
        }
    }

    /**
     * 네이버페이 결제 승인
     */
    public boolean approveNaverPayment(String paymentId, Long reservationId) {
        log.info("네이버페이 결제 승인 시작 - 예약 ID: {}, Payment ID: {}", reservationId, paymentId);

        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

            // 네이버페이 결제 승인 API 호출
            String url = "https://dev.apis.naver.com/naverpay-partner/naverpay/payments/v2.2/apply/payment";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json;charset=UTF-8");
            headers.set("X-Naver-Client-Id", naverPayClientId);
            headers.set("X-Naver-Client-Secret", naverPayClientSecret);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentId", paymentId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            if (result != null && "Success".equals(result.get("code"))) {
                // 결제 완료 처리
                reservation.markAsPaid("NAVER_PAY", paymentId);
                reservationRepository.save(reservation);

                log.info("네이버페이 결제 승인 완료: 예약 ID {}, Payment ID {}", reservationId, paymentId);
                return true;
            } else {
                String errorMessage = result != null ? (String) result.get("message") : "알 수 없는 오류";
                log.error("네이버페이 승인 실패: {}", errorMessage);

                reservation.markPaymentFailed(errorMessage);
                reservationRepository.save(reservation);

                return false;
            }

        } catch (Exception e) {
            log.error("네이버페이 결제 승인 실패", e);

            // 예약 조회 후 실패 처리
            reservationRepository.findById(reservationId).ifPresent(reservation -> {
                reservation.markPaymentFailed(e.getMessage());
                reservationRepository.save(reservation);
            });

            return false;
        }
    }
    /**
     * 카카오페이 환불 처리
     */
    public boolean refundKakaoPayment(Long reservationId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

            if (!reservation.isPaymentCompleted()) {
                log.warn("결제가 완료되지 않은 예약입니다. reservationId: {}", reservationId);
                return false;
            }

            String tid = reservation.getPaymentTid();
            if (tid == null || tid.isEmpty()) {
                log.error("결제 TID가 없습니다. reservationId: {}", reservationId);
                return false;
            }

            // 카카오페이 환불 API 호출
            String url = "https://kapi.kakao.com/v1/payment/cancel";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoPayAdminKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            Map<String, Object> params = new HashMap<>();
            params.put("cid", "TC0ONETIME");
            params.put("tid", tid);
            params.put("cancel_amount", reservation.getPaymentAmount());
            params.put("cancel_tax_free_amount", 0);
            params.put("cancel_vat_amount", reservation.getPaymentAmount() / 11);

            String body = buildFormData(params);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            // 카카오페이 API 응답 검증
            if (result != null && result.get("aid") != null) {
                // 환불 성공 - aid가 있으면 성공
                String aid = (String) result.get("aid");

                reservation.markPaymentRefunded();
                reservationRepository.save(reservation);

                log.info("카카오페이 환불 완료: reservationId={}, tid={}, aid={}, amount={}",
                        reservationId, tid, aid, reservation.getPaymentAmount());
                return true;

            } else {
                // 환불 실패 - API 응답 이상
                log.error("카카오페이 환불 API 응답 이상: reservationId={}, response={}",
                        reservationId, result);
                return false;
            }

        } catch (Exception e) {
            log.error("카카오페이 환불 실패: reservationId={}", reservationId, e);
            return false;
        }
    }


    /**
     * 네이버페이 환불 처리
     */
    public boolean refundNaverPayment(Long reservationId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

            if (!reservation.isPaymentCompleted()) {
                log.warn("결제가 완료되지 않은 예약입니다. reservationId: {}", reservationId);
                return false;
            }

            String paymentId = reservation.getPaymentTid();
            if (paymentId == null || paymentId.isEmpty()) {
                log.error("결제 ID가 없습니다. reservationId: {}", reservationId);
                return false;
            }

            // 네이버페이 환불 API 호출
            String url = "https://dev.apis.naver.com/naverpay-partner/naverpay/payments/v2.2/apply/cancel";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json;charset=UTF-8");
            headers.set("X-Naver-Client-Id", naverPayClientId);
            headers.set("X-Naver-Client-Secret", naverPayClientSecret);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentId", paymentId);
            requestBody.put("cancelAmount", reservation.getPaymentAmount());
            requestBody.put("cancelReason", "사용자 예약 취소");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            if (result != null && "Success".equals(result.get("code"))) {
                // 환불 성공 처리
                reservation.markPaymentRefunded();
                reservationRepository.save(reservation);

                log.info("네이버페이 환불 완료: reservationId={}, paymentId={}, amount={}",
                        reservationId, paymentId, reservation.getPaymentAmount());
                return true;
            } else {
                String errorMessage = result != null ? (String) result.get("message") : "알 수 없는 오류";
                log.error("네이버페이 환불 실패: reservationId={}, error={}", reservationId, errorMessage);
                return false;
            }

        } catch (Exception e) {
            log.error("네이버페이 환불 실패: reservationId={}", reservationId, e);
            return false;
        }
    }

    /**
     * 통합 환불 처리 (결제 방법에 따라 자동 분기)
     */
    public boolean processRefund(Long reservationId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

            if (!reservation.isPaymentCompleted()) {
                log.info("결제되지 않은 예약이므로 환불할 필요가 없습니다. reservationId: {}", reservationId);
                return true;
            }

            String paymentMethod = reservation.getPaymentMethod();
            if (paymentMethod == null) {
                log.error("결제 방법 정보가 없습니다. reservationId: {}", reservationId);
                return false;
            }

            switch (paymentMethod.toUpperCase()) {
                case "KAKAO_PAY":
                    return refundKakaoPayment(reservationId);
                case "NAVER_PAY":
                    return refundNaverPayment(reservationId);
                default:
                    log.error("지원하지 않는 결제 방법입니다. reservationId={}, paymentMethod={}",
                            reservationId, paymentMethod);
                    return false;
            }

        } catch (Exception e) {
            log.error("환불 처리 중 오류 발생: reservationId={}", reservationId, e);
            return false;
        }
    }

    private String buildFormData(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}