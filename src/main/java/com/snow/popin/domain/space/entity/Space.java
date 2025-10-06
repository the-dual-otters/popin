package com.snow.popin.domain.space.entity;

import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "place_lists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "area_size")
    private Integer areaSize;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial = false;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(length = 500)
    private String address; // 임시 호환 필드

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "rental_fee")
    private Integer rentalFee;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    //  Venue 연관관계 추가
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    //is_hidden 추가
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    public void hide() {
        this.isHidden = true;
    }

    public void show() {
        this.isHidden = false;
    }

    @Builder
    public Space(User owner, String title, String description,
                 String address, Integer areaSize, LocalDate startDate, LocalDate endDate,
                 Integer rentalFee, String contactPhone, String coverImageUrl,
                 Venue venue) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.address = address;
        this.areaSize = areaSize;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rentalFee = rentalFee;
        this.contactPhone = contactPhone;
        this.coverImageUrl = coverImageUrl;
        this.venue = venue;
        this.isPublic = true;
        this.isOfficial = false;
    }

    // 비즈니스 메서드
    public void updateSpaceInfo(String title, String description,
                                Integer areaSize, LocalDate startDate, LocalDate endDate,
                                Integer rentalFee, String contactPhone) {
        this.title = title;
        this.description = description;
        this.areaSize = areaSize;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rentalFee = rentalFee;
        this.contactPhone = contactPhone;
    }

    public void updateVenue(Venue newVenue) {
        this.venue = newVenue;
    }

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public boolean isOwner(User user) {
        return this.owner.getId().equals(user.getId());
    }
}
