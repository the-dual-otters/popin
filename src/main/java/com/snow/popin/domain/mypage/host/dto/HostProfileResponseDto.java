package com.snow.popin.domain.mypage.host.dto;

import com.snow.popin.domain.mypage.host.entity.Host;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostProfileResponseDto {
    private String email;
    private String name;
    private String nickname;
    private String phone;
    private String brandName;

    public static HostProfileResponseDto from(Host host) {
        return HostProfileResponseDto.builder()
                .email(host.getUser().getEmail())
                .name(host.getUser().getName())
                .nickname(host.getUser().getNickname())
                .phone(host.getUser().getPhone())
                .brandName(host.getBrand().getName())
                .build();
    }
}
