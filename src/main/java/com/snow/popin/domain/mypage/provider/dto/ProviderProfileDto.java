package com.snow.popin.domain.mypage.provider.dto;

import com.snow.popin.domain.mypage.provider.entity.ProviderProfile;
import lombok.Getter;

@Getter
public class ProviderProfileDto {
    public Long id;
    public String userEmail;
    public String name;
    public String phone;
    public String businessRegistrationNumber;
    public boolean verified;
    public String bankName;
    public String accountNumber;
    public String accountHolder;

    public static ProviderProfileDto from(ProviderProfile p) {
        ProviderProfileDto d = new ProviderProfileDto();
        d.id = p.getId();
        d.userEmail = p.getUserEmail();
        d.name = p.getName();
        d.phone = p.getPhone();
        d.businessRegistrationNumber = p.getBusinessRegistrationNumber();
        d.verified = p.isVerified();
        d.bankName = p.getBankName();
        d.accountNumber = p.getAccountNumber();
        d.accountHolder = p.getAccountHolder();
        return d;
    }
}
