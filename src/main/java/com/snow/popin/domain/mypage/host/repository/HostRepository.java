package com.snow.popin.domain.mypage.host.repository;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.entity.Host;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HostRepository extends JpaRepository<Host, Long> {

    /** 특정 사용자와 연결된 Host (1:1 가정) */
    Optional<Host> findByUser(User user);

    /** 브랜드 + 사용자 조합 존재 여부 (연관 경로는 User_Id 사용) */
    boolean existsByBrandAndUser_Id(Brand brand, Long userId);
}
