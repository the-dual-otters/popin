package com.snow.popin.domain.mypage.host.repository;

import com.snow.popin.domain.mypage.host.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByNameContaining(String name);

    boolean existsByName(String name);

    List<Brand> findByBusinessType(Brand.BusinessType businessType);
}