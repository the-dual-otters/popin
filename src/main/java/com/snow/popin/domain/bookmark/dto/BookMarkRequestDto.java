package com.snow.popin.domain.bookmark.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class BookMarkRequestDto {

    @NotNull(message = "팝업 ID는 필수입니다.")
    private Long popupId;

    public BookMarkRequestDto(Long popupId) {
        this.popupId = popupId;
    }

    public void setPopupId(Long popupId) {
        this.popupId = popupId;
    }
}