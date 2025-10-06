package com.snow.popin.domain.review.entity;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_popup_user", columnNames = {"popup_id","user_id"})
    },
    indexes = {
        @Index(name = "idx_review_popup_created", columnList = "popup_id, created_at"),
        @Index(name = "idx_review_user", columnList = "user_id"),
        @Index(name = "idx_review_blocked", columnList = "is_blocked"),
        @Index(name = "idx_review_popup_rating", columnList = "popup_id, rating")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "popup_id", nullable = false)
    private Long popupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", insertable = false, updatable = false)
    private Popup popup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public Review(Long popupId, Long userId, String content, Integer rating) {
        this.popupId = popupId;
        this.userId = userId;
        this.content = content;
        this.rating = rating;
        this.isBlocked = false;
    }

    // 정적 팩토리 메서드
    public static Review of(Long popupId, Long userId, String content, Integer rating) {
        return Review.builder()
                .popupId(popupId)
                .userId(userId)
                .content(content)
                .rating(rating)
                .build();
    }

    // 비즈니스 로직 메서드
    public void updateContent(String newContent) {
        this.content = newContent;
    }

    public void updateRating(Integer newRating) {
        this.rating = newRating;
    }

    public void block() {
        this.isBlocked = true;
    }

    public void unblock() {
        this.isBlocked = false;
    }

    public boolean isBlocked() {
        return Boolean.TRUE.equals(this.isBlocked);
    }

    // 리뷰 수정 가능 여부 (작성자 본인만)
    public boolean canEdit(Long currentUserId) {
        return this.userId.equals(currentUserId) && !isBlocked();
    }
}
