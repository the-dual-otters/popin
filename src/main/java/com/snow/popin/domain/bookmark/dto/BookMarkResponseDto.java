package com.snow.popin.domain.bookmark.dto;

import com.snow.popin.domain.bookmark.entity.BookMark;
import com.snow.popin.domain.popup.dto.response.PopupBasicResponseDto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookMarkResponseDto {
    private final Long id;
    private final Long userId;
    private final Long popupId;
    private final LocalDateTime createdAt;
    private final PopupBasicResponseDto popup;

    public static BookMarkResponseDto from(BookMark bookmark) {
        return BookMarkResponseDto.builder()
                .id(bookmark.getId())
                .userId(bookmark.getUserId())
                .popupId(bookmark.getPopupId())
                .createdAt(bookmark.getCreatedAt())
                .popup(bookmark.getPopup() != null ?
                        PopupBasicResponseDto.from(bookmark.getPopup()) : null)
                .build();
    }

}