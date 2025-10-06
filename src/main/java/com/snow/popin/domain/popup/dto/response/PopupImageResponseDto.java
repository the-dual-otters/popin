package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.PopupImage;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PopupImageResponseDto {
    private Long id;
    private String imageUrl;
    private String caption;
    private Integer sortOrder;

    public static PopupImageResponseDto from(PopupImage image) {
        return PopupImageResponseDto.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .caption(image.getCaption())
                .sortOrder(image.getSortOrder())
                .build();
    }
}