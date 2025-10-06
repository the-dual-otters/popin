package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.Popup;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PopupBasicResponseDto {

    private final Long popupId;
    private final String popupTitle;
    private final String summary;
    private final String mainImageUrl;
    private final String region;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String periodText;
    private final String status;

    public static PopupBasicResponseDto from(Popup popup) {
        return PopupBasicResponseDto.builder()
                .popupId(popup.getId())
                .popupTitle(popup.getTitle())
                .summary(popup.getSummary())
                .mainImageUrl(popup.getMainImageUrl())
                .region(popup.getRegion())
                .description(popup.getDescription())
                .startDate(popup.getStartDate())
                .endDate(popup.getEndDate())
                .periodText(formatPeriod(popup.getStartDate(), popup.getEndDate()))
                .status(popup.getStatus().name())
                .build();
    }

    private static String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) return null;
        return String.format("%s - %s",
                start.toString().replace("-", "."),
                end.toString().replace("-", "."));
    }
}
