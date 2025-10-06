package com.snow.popin.domain.mypage.host.repository;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.entity.Host;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HostRepository extends JpaRepository<Host, Long> {

    /**
     * 특정 사용자와 연결된 Host 조회
     * (한 명의 유저는 하나의 Host만 가질 수 있음)
     *
     * @param user 조회할 사용자
     * @return 해당 사용자의 Host 엔티티 (없으면 Optional.empty)
     */
    Optional<Host> findByUser(User user);
    /**
     * 특정 브랜드와 사용자 조합이 존재하는지 여부 확인
     *
     * @param brand 브랜드 엔티티
     * @param userId 사용자 ID
     * @return 조합이 존재하면 true, 아니면 false
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END " +
            "FROM Host h WHERE h.brand = :brand AND h.user.id = :userId")
    boolean existsByBrandAndUser(@Param("brand") Brand brand, @Param("userId") Long userId);

    /**
     * 특정 브랜드의 첫 번째 호스트 조회 (주로 소유자)
     *
     * @param brand 브랜드 엔티티
     * @return 해당 브랜드의 첫 번째 호스트 (없으면 Optional.empty)
     */
    Optional<Host> findFirstByBrand(Brand brand);
}
