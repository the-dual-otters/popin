package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long>,
        JpaSpecificationExecutor<Popup>,
        PopupRepositoryCustom {

    Optional<Popup> findFirstByTitle(String title);

    long countByStatus(PopupStatus status);
}
