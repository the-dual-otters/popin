package com.snow.popin.domain.bookmark.entity;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(
        name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "popup_id"}),
        indexes = {
                @Index(name = "idx_bookmarks_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_bookmarks_popup", columnList = "popup_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookMark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "popup_id", nullable = false)
    private Long popupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", insertable = false, updatable = false)
    private Popup popup;

    public BookMark(Long userId, Long popupId) {
        this.userId = userId;
        this.popupId = popupId;
    }

    public static BookMark of(Long userId, Long popupId) {
        return new BookMark(userId, popupId);
    }

    public static BookMark of(User user, Popup popup) {
        BookMark bookmark = new BookMark(user.getId(), popup.getId());
        bookmark.user = user;
        bookmark.popup = popup;
        return bookmark;
    }

    public static BookMark ofWithPopup(Long userId, Popup popup) {
        BookMark bookmark = new BookMark(userId, popup.getId());
        bookmark.popup = popup;
        return bookmark;
    }
}