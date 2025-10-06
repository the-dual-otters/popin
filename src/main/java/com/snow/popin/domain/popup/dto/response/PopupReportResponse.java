package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.PopupReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PopupReportResponse {

    private final Long id;
    private final String brandName;
    private final String popupName;
    private final String address;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String extraInfo;
    private final List<String> images;
    private final String status;

    public static PopupReportResponse from(PopupReport r) {
        return PopupReportResponse.builder()
                .id(r.getId())
                .brandName(r.getBrandName())
                .popupName(r.getPopupName())
                .address(r.getAddress())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .extraInfo(r.getExtraInfo())
                .images(r.getImages())
                .status(r.getStatus().name())
                .build();
    }
}
