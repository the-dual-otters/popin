package com.snow.popin.domain.popup.entity;

import com.snow.popin.domain.category.entity.Category;
import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterRequestDto;
import com.snow.popin.global.common.BaseEntity;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "popups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Popup extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id")
    private Long brandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "title_search")
    private String titleSearch;

    private String title;
    private String summary;
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private PopupStatus status;

    @Column(name = "entry_fee")
    private Integer entryFee = 0;

    @Column(name = "reservation_available")
    private Boolean reservationAvailable = false;

    @Column(name = "reservation_link")
    private String reservationLink;

    @Column(name = "waitlist_available")
    private Boolean waitlistAvailable = false;

    private String notice;

    @Column(name = "main_image_url")
    private String mainImageUrl;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "view_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long viewCount = 0L;

    @OneToMany(mappedBy = "popup", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 50)
    @OrderBy("sortOrder ASC")
    private Set<PopupImage> images = new LinkedHashSet<>();

    @OneToMany(mappedBy = "popup", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 50)
    @OrderBy("dayOfWeek ASC")
    private Set<PopupHours> hours = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "popup_tags",
            joinColumns = @JoinColumn(name = "popup_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public boolean isFreeEntry() {
        return entryFee == null || entryFee == 0;
    }

    public String getFeeDisplayText() {
        return isFreeEntry() ? "무료" : String.format("%,d원", entryFee);
    }

    public String getVenueName() {
        return venue != null ? venue.getName() : null;
    }

    public String getVenueAddress() {
        return venue != null ? venue.getFullAddress() : null;
    }

    public String getRegion() {
        return venue != null ? venue.getRegion() : null;
    }

    public Double getLatitude() {
        return venue != null ? venue.getLatitude() : null;
    }

    public Double getLongitude() {
        return venue != null ? venue.getLongitude() : null;
    }

    public Boolean getParkingAvailable() {
        return venue != null ? venue.getParkingAvailable() : false;
    }

    public String getPeriodText() {
        if (startDate == null && endDate == null) {
            return "기간 미정";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return startDate.format(formatter);
            }
            return startDate.format(formatter) + " - " + endDate.format(formatter);
        } else if (startDate != null) {
            return startDate.format(formatter) + " - ";
        } else {
            return " - " + endDate.format(formatter);
        }
    }

    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    public String getCategorySlug() {
        return category != null ? category.getSlug() : null;
    }

    public boolean isOngoing() {
        LocalDate now = LocalDate.now();
        return (startDate == null || !now.isBefore(startDate)) &&
                (endDate == null || !now.isAfter(endDate));
    }

    public boolean isUpcoming() {
        LocalDate now = LocalDate.now();
        return startDate != null && now.isBefore(startDate);
    }

    public boolean isEnded() {
        LocalDate now = LocalDate.now();
        return endDate != null && now.isAfter(endDate);
    }

    public Long getDaysUntilEnd() {
        if (endDate == null) {
            return null;
        }
        LocalDate now = LocalDate.now();
        return ChronoUnit.DAYS.between(now, endDate);
    }

    // 현재 날짜를 기준으로 팝업의 상태를 업데이트
    public boolean updateStatus() {
        LocalDate now = LocalDate.now();
        PopupStatus newStatus = determineStatus(now);

        if (this.status != newStatus) {
            this.status = newStatus;
            return true;
        }
        return false;
    }

    private PopupStatus determineStatus(LocalDate now) {
        if (this.endDate != null && now.isAfter(this.endDate)) {
            return PopupStatus.ENDED;
        }
        if (this.startDate != null && now.isBefore(this.startDate)) {
            return PopupStatus.PLANNED;
        }
        return PopupStatus.ONGOING;
    }

    // 관리자가 직접 팝업의 상태 변경
    public void AdminUpdateStatus(PopupStatus status) {
        this.status = status;
    }

    // 조회수 증가 메서드
    public void incrementViewCount() {
        this.viewCount++;
    }

    // 테스트용 메서드
    public static Popup createForTest(String title, PopupStatus status, Venue venue) {
        Popup popup = new Popup();
        popup.title = title;
        popup.summary = "테스트 요약";
        popup.status = status;
        popup.venue = venue;
        popup.startDate = LocalDate.now();
        popup.endDate = LocalDate.now().plusDays(7);
        popup.isFeatured = false;
        popup.entryFee = 0;
        popup.reservationAvailable = false;
        popup.waitlistAvailable = false;
        return popup;
    }

    public static Popup createForTestWithDates(String title, LocalDate startDate, LocalDate endDate, Venue venue) {
        Popup popup = createForTest(title, PopupStatus.ONGOING, venue);
        popup.startDate = startDate;
        popup.endDate = endDate;
        return popup;
    }

    public static Popup createFeaturedForTest(String title, PopupStatus status, Venue venue) {
        Popup popup = createForTest(title, status, venue);
        popup.isFeatured = true;
        return popup;
    }

    public void setStatusForTest(PopupStatus status) {
        this.status = status;
    }

    public void setFeaturedForTest(boolean featured) {
        this.isFeatured = featured;
    }

    public void setTitle(String title) {
        this.title = title;
        this.titleSearch = title != null ? title.toLowerCase().trim() : null;
    }

    //  생성 메서드
    public static Popup create(Long brandId, PopupRegisterRequestDto dto) {
        Popup popup = new Popup();
        popup.brandId = brandId;
        popup.setTitle(dto.getTitle());
        popup.summary = dto.getSummary();
        popup.description = dto.getDescription();
        popup.startDate = dto.getStartDate();
        popup.endDate = dto.getEndDate();
        popup.entryFee = dto.getEntryFee();
        popup.reservationAvailable = dto.getReservationAvailable();
        popup.reservationLink = dto.getReservationLink();
        popup.waitlistAvailable = dto.getWaitlistAvailable();
        popup.notice = dto.getNotice();
        popup.mainImageUrl = dto.getMainImageUrl();
        popup.isFeatured = dto.getIsFeatured();
        popup.status = PopupStatus.PLANNED;
        return popup;
    }

    //  수정 메서드
    public void update(PopupRegisterRequestDto dto) {
        this.setTitle(dto.getTitle());
        this.summary = dto.getSummary();
        this.description = dto.getDescription();
        this.startDate = dto.getStartDate();
        this.endDate = dto.getEndDate();
        this.entryFee = dto.getEntryFee();
        this.reservationAvailable = dto.getReservationAvailable();
        this.reservationLink = dto.getReservationLink();
        this.waitlistAvailable = dto.getWaitlistAvailable();
        this.notice = dto.getNotice();
        this.mainImageUrl = dto.getMainImageUrl();
        this.isFeatured = dto.getIsFeatured();
    }
    //
    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public void setStatus(PopupStatus status) {
        this.status = status;
    }

    public void setViewCountForTest(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}