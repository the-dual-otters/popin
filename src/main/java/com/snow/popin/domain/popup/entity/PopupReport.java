package com.snow.popin.domain.popup.entity;


import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(
        name = "popup_reports",
        indexes = {
                @Index(name = "idx_popup_reports_status_created", columnList = "status, created_at"),
                @Index(name = "idx_popup_reports_reporter", columnList = "reporter_user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopupReport extends BaseEntity {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporter;

    @Column(name = "brand_name", length = 150)
    private String brandName;

    @Column(name = "popup_name", length = 200, nullable = false)
    private String popupName;

    @Column(length = 500, nullable = false)
    private String address;


    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;

    @ElementCollection
    @CollectionTable(name = "popup_report_images", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "image_url", length = 500, nullable = false)
    private List<String> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;


    @Builder
    public PopupReport(User reporter,
                       String brandName,
                       String popupName,
                       String address,
                       String periodText,
                       LocalDate startDate,
                       LocalDate endDate,
                       String extraInfo,
                       List<String> images) {
        this.reporter = reporter;
        this.brandName = brandName;
        this.popupName = popupName;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.extraInfo = extraInfo;
        if (images != null) this.images.addAll(images);
        this.status = Status.PENDING;
    }

    /** 승인 처리 */
    public void approve() {
        this.status = Status.APPROVED;
    }

    /** 반려 처리 */
    public void reject() {
        this.status = Status.REJECTED;
    }

    /** 제보자 본인 여부 */
    public boolean isReporter(User user) {
        return this.reporter != null && user != null && this.reporter.getId().equals(user.getId());
    }

    /** 이미지 목록 교체 */
    public void replaceImages(List<String> newImages) {
        this.images.clear();
        if (newImages != null) this.images.addAll(newImages);
    }
}
