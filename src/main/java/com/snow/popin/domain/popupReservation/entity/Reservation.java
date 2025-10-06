package com.snow.popin.domain.popupReservation.entity;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservation_user_id", columnList = "user_id"),
        @Index(name = "idx_reservation_popup_id", columnList = "popup_id"),
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_payment_status", columnList = "payment_Status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    // 예약 희망일시
    @Column(name = "reservation_date")
    private LocalDateTime reservationDate;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    // ===== 결제 관련 필드들 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_amount")
    private Integer paymentAmount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_tid", length = 100)
    private String paymentTid;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_fail_reason", length = 500)
    private String paymentFailReason;

    // ===== 결제 상태 Enum =====

    public enum PaymentStatus {
        PENDING("결제 대기"),
        COMPLETED("결제 완료"),
        FAILED("결제 실패"),
        CANCELLED("결제 취소"),
        REFUNDED("환불 완료");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ===== 예약 상태 관련 메서드들 =====

    public boolean canCancel() {
        return status == ReservationStatus.RESERVED;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("취소할 수 없는 예약입니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == ReservationStatus.RESERVED;
    }

    public void markAsVisited() {
        this.status = ReservationStatus.VISITED;
    }

    // ===== 결제 관련 메서드들 =====

    /**
     * 결제 금액 설정 (PaymentService에서 사용)
     */
    public void setPaymentAmount(Integer amount) {
        this.paymentAmount = amount;
    }

    /**
     * 결제 TID 설정 (PaymentService에서 사용)
     */
    public void setPaymentTid(String tid) {
        this.paymentTid = tid;
    }

    /**
     * 결제 완료 처리
     */
    public void markAsPaid(String paymentMethod, String tid) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentMethod = paymentMethod;
        this.paymentTid = tid;
        this.paidAt = LocalDateTime.now();
        this.paymentFailReason = null; // 실패 사유 초기화
    }

    /**
     * 결제 실패 처리
     */
    public void markPaymentFailed(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.paymentFailReason = reason;
        this.paidAt = null;
    }

    /**
     * 결제 대기 상태로 설정
     */
    public void markPaymentPending() {
        this.paymentStatus = PaymentStatus.PENDING;
        this.paymentFailReason = null;
        this.paidAt = null;
    }

    /**
     * 결제 취소 처리
     */
    public void markPaymentCancelled() {
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.paymentFailReason = "사용자 취소";
        this.paidAt = null;
    }

    /**
     * 환불 처리
     */
    public void markPaymentRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.paidAt = null;
    }

    // ===== 결제 상태 확인 메서드들 =====

    /**
     * 결제가 필요한지 확인
     */
    public boolean isPaymentRequired() {
        return paymentAmount != null && paymentAmount > 0;
    }

    /**
     * 결제가 완료되었는지 확인
     */
    public boolean isPaymentCompleted() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    /**
     * 결제가 실패했는지 확인
     */
    public boolean isPaymentFailed() {
        return paymentStatus == PaymentStatus.FAILED;
    }

    /**
//     * 결제 대기 중인지 확인
//     */
    public boolean isPaymentPending() {
        return paymentStatus == PaymentStatus.PENDING;
    }

    // ===== 정적 팩토리 메서드 =====

    /**
     * 예약 생성
     */
    public static Reservation create(Popup popup, User user, String name, String phone, Integer partySize, LocalDateTime reservationDate) {
        return Reservation.builder()
                .popup(popup)
                .user(user)
                .name(name)
                .phone(phone)
                .partySize(partySize)
                .reservationDate(reservationDate)
                .status(ReservationStatus.RESERVED)
                .reservedAt(LocalDateTime.now())
                .paymentStatus(PaymentStatus.PENDING) // 기본값으로 결제 대기 상태
                .build();
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Integer getPaymentAmount() {
        return paymentAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentTid() {
        return paymentTid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getPaymentFailReason() {
        return paymentFailReason;
    }
}