package com.snow.popin.domain.popupReservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 예약 생성 요청 DTO
 * 예약자 이름, 전화번호, 예약일자를 포함
 */
@Getter
@Setter
@NoArgsConstructor
public class ReservationRequestDto {
    /** 예약자 이름 */
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
    /** 예약자 전화번호 (010-0000-0000 형식) */
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;
    /** 예약 인원 */
    @NotNull(message = "예약 인원은 필수입니다.")
    @Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
    @Max(value = 10, message = "예약 인원은 10명 이하여야 합니다.")
    private Integer partySize;
    /** 예약일자 (현재 또는 미래만 가능) */
    @NotNull(message = "예약일자는 필수입니다.")
    @FutureOrPresent(message = "예약일자는 오늘 이후여야 합니다.")
    private LocalDateTime reservationDate;
}