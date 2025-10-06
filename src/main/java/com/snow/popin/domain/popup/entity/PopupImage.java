package com.snow.popin.domain.popup.entity;

import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "popup_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopupImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    private String caption;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public void updateImageInfo(String imageUrl, String caption) {
        this.imageUrl = imageUrl;
        this.caption = caption;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void prePersist() {
        if (sortOrder == null) sortOrder = 0;
    }
}