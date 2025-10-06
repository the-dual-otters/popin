package com.snow.popin.domain.mypage.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueRegisterDto {
    private String name;
    private String roadAddress;
    private String jibunAddress;
    private String detailAddress;
    private Double latitude;
    private Double longitude;
    private Boolean parkingAvailable;
}