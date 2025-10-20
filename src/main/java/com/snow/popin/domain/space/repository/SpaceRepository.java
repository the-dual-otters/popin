package com.snow.popin.domain.space.repository;

import com.snow.popin.domain.space.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long>,
        JpaSpecificationExecutor<Space>,
        SpaceRepositoryCustom {
}