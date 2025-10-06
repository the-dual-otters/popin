package com.snow.popin.domain.mypage.host.entity;

import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "brands")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Host> hosts = new ArrayList<>();


    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String officialSite;

    @Column(length = 500)
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String snsLinks;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private BusinessType businessType = BusinessType.INDIVIDUAL;

    @Column
    private Long categoryId;

    public enum BusinessType {
        INDIVIDUAL, CORPORATE
    }

    //  생성자
    @Builder
    public Brand(String name,
                 String description,
                 String officialSite,
                 String logoUrl,
                 String snsLinks,
                 BusinessType businessType,
                 Long categoryId) {
        this.name = name;
        this.description = description;
        this.officialSite = officialSite;
        this.logoUrl = logoUrl;
        this.snsLinks = snsLinks;
        this.businessType = businessType != null ? businessType : BusinessType.INDIVIDUAL;
        this.categoryId = categoryId;
    }
}
