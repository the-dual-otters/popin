package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.Popup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopupSearchRepository
        extends JpaRepository<Popup, Long>, PopupSearchRepositoryCustom {
}
