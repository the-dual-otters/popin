package com.snow.popin.domain.popup.dto.request;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

@Getter
public class PopupReportCreateRequest {
    private String brandName;
    @NotBlank private String popupName;
    @NotBlank private String address;
    private LocalDate startDate;
    private LocalDate endDate;
    private String extraInfo;
    private List<MultipartFile> images;
}
