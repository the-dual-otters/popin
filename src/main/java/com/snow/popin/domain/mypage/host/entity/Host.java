package com.snow.popin.domain.mypage.host.entity;

import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(
        name = "brand_members",
        indexes = {
                @Index(name = "idx_brand_member_user_id", columnList = "user_id"),
                @Index(name = "idx_brand_member_brand_id", columnList = "brand_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Host extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_brand", nullable = false)
    private HostRole roleInBrand;

    public boolean isOwner() {
        return this.roleInBrand == HostRole.OWNER;
    }

    //  생성자
    @Builder
    public Host(Brand brand, User user, HostRole roleInBrand) {
        this.brand = brand;
        this.user = user;
        this.roleInBrand = roleInBrand;
    }
}
