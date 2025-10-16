package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.PopupHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PopupHoursRepository extends JpaRepository<PopupHours, Long> {

    // 전체 운영시간 (popup.id 경로)
    List<PopupHours> findByPopup_Id(Long popupId);

    // 요일별 운영시간
    List<PopupHours> findByPopup_IdAndDayOfWeek(Long popupId, Integer dayOfWeek);

    // 존재 여부
    boolean existsByPopup_IdAndDayOfWeek(Long popupId, Integer dayOfWeek);

    // 삭제: 파생 deleteBy면 @Modifying 불필요 (반환값 = 삭제된 행 수)
    @Transactional
    long deleteByPopup_Id(Long popupId);

    // 운영 요일 목록 (distinct + 정렬만 필요해서 @Query 한 줄)
    @Query("select distinct ph.dayOfWeek from PopupHours ph where ph.popup.id = :popupId order by ph.dayOfWeek asc")
    List<Integer> findDistinctDayOfWeekByPopupId(@Param("popupId") Long popupId);
}
