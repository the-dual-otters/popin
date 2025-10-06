package com.snow.popin.domain.spacereservation.entity;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.global.common.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "space_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpaceReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private boolean isHidden = false;

    public void hide() {
        this.isHidden = true;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    // Status 관련 메서드
    public void accept() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 승인할 수 있습니다.");
        }
        this.status = ReservationStatus.ACCEPTED;
    }

    public void reject() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 거절할 수 있습니다.");
        }
        this.status = ReservationStatus.REJECTED;
    }

    public void cancel() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        if (this.status == ReservationStatus.REJECTED) {
            throw new IllegalStateException("거절된 예약은 취소할 수 없습니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isOwner(User user) {
        return this.host.getId().equals(user.getId());
    }

    public boolean isSpaceOwner(User user) {
        return this.space.getOwner().getId().equals(user.getId());
    }

    public boolean isPending() {
        return this.status == ReservationStatus.PENDING;
    }
}
