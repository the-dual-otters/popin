package com.snow.popin.domain.spacereservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class SpaceReservationCreateRequestDto {

    @NotNull(message = "공간 ID는 필수입니다.")
    private Long spaceId;

    @NotNull(message = "팝업 ID는 필수입니다.")
    private Long popupId;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    private String message;
    private String contactPhone;
}