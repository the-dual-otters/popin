package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.PopupHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PopupHoursRepository extends JpaRepository<PopupHours, Long> {
    @Query("SELECT ph FROM PopupHours ph WHERE ph.popup.id = :popupId")
    List<PopupHours> findByPopupId(@Param("popupId") Long popupId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM PopupHours ph WHERE ph.popup.id = :popupId")
    int deleteByPopupId(@Param("popupId") Long popupId);

    // 특정 팝업의 특정 요일에 대한 운영시간 조회
    List<PopupHours> findByPopupIdAndDayOfWeek(Long popupId, Integer dayOfWeek);

    // 특정 팝업의 운영 요일 목록 조회
    @Query("SELECT DISTINCT p.dayOfWeek FROM PopupHours p WHERE p.popup.id = :popupId ORDER BY p.dayOfWeek ASC")
    List<Integer> findDistinctDayOfWeekByPopupId(@Param("popupId") Long popupId);

    // 특정 팝업이 특정 요일에 운영하는지 확인
    boolean existsByPopupIdAndDayOfWeek(Long popupId, Integer dayOfWeek);
}