package com.snow.popin.domain.mypage.provider.repository;

import com.snow.popin.domain.mypage.provider.entity.ProviderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {

    Optional<ProviderProfile> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);
}