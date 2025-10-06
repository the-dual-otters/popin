package com.snow.popin.domain.popup.dto.request;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Getter
public class PopupSearchRequestDto {

    private String query;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;

    @Builder
    private PopupSearchRequestDto(String query, int page, int size) {
        this.query = query;
        this.page = Math.max(0, page);
        this.size = Math.min(Math.max(1, size), 100);
    }

    // 팩토리 메서드
    public static PopupSearchRequestDto of(String query, int page, int size) {
        return PopupSearchRequestDto.builder()
                .query(query)
                .page(page)
                .size(size)
                .build();
    }
}