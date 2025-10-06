package com.snow.popin.domain.mypage.provider.entity;

import com.snow.popin.global.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "provider_profile",
        indexes = {
                @Index(name = "idx_provider_profile_user_email", columnList = "userEmail", unique = true)
        })
public class ProviderProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    //회원가입시
    @Column(length = 40)
    private String businessRegistrationNumber;

    @Column(nullable = false)
    private boolean verified = false;

    //하단은 정산용인데 나중에 안쓴다면 제거
    @Column(length = 40)
    private String bankName;

    @Column(length = 40)
    private String accountNumber;

    @Column(length = 40)
    private String accountHolder;
}
