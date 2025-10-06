package com.snow.popin.domain.reservation;

import com.snow.popin.domain.popup.entity.PopupHours;
import com.snow.popin.domain.popup.repository.PopupHoursRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("팝업 운영시간 Repository 테스트")
class PopupHoursRepositoryTest {

    @Autowired
    private PopupHoursRepository popupHoursRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // FIX: 처음부터 올바른 쿼리만 사용하도록 수정
        insertTestData();
    }

    @Test
    @DisplayName("팝업 ID로 운영시간 조회")
    void findByPopupId() {
        // when
        List<PopupHours> popup1Hours = popupHoursRepository.findByPopupId(1L);
        List<PopupHours> popup2Hours = popupHoursRepository.findByPopupId(2L);

        // then
        assertThat(popup1Hours).hasSize(5); // 월~금
        assertThat(popup2Hours).hasSize(2); // 토~일

        // 시간 검증
        popup1Hours.forEach(hours -> {
            assertThat(hours.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(hours.getCloseTime()).isEqualTo(LocalTime.of(22, 0));
        });

        popup2Hours.forEach(hours -> {
            assertThat(hours.getOpenTime()).isEqualTo(LocalTime.of(11, 0));
            assertThat(hours.getCloseTime()).isEqualTo(LocalTime.of(21, 0));
        });
    }

    @Test
    @DisplayName("팝업 ID와 요일로 운영시간 조회")
    void findByPopupIdAndDayOfWeek() {
        // when
        List<PopupHours> mondayHours = popupHoursRepository.findByPopupIdAndDayOfWeek(1L, 1); // 월요일
        List<PopupHours> saturdayHours = popupHoursRepository.findByPopupIdAndDayOfWeek(2L, 6); // 토요일
        List<PopupHours> popup1SundayHours = popupHoursRepository.findByPopupIdAndDayOfWeek(1L, 0); // 팝업1은 일요일 운영 안함

        // then
        assertThat(mondayHours).hasSize(1);
        assertThat(mondayHours.get(0).getDayOfWeek()).isEqualTo(1);
        assertThat(mondayHours.get(0).getOpenTime()).isEqualTo(LocalTime.of(10, 0));

        assertThat(saturdayHours).hasSize(1);
        assertThat(saturdayHours.get(0).getDayOfWeek()).isEqualTo(6);
        assertThat(saturdayHours.get(0).getOpenTime()).isEqualTo(LocalTime.of(11, 0));

        assertThat(popup1SundayHours).isEmpty(); // 팝업1은 일요일 운영 안함
    }

    @Test
    @DisplayName("팝업의 운영 요일 목록 조회")
    void findDistinctDayOfWeekByPopupId() {
        // when
        List<Integer> popup1Days = popupHoursRepository.findDistinctDayOfWeekByPopupId(1L);
        List<Integer> popup2Days = popupHoursRepository.findDistinctDayOfWeekByPopupId(2L);

        // then
        assertThat(popup1Days).containsExactly(1, 2, 3, 4, 5); // 월~금
        assertThat(popup2Days).containsExactlyInAnyOrder(0, 6); // 일, 토
    }

    @Test
    @DisplayName("특정 요일에 운영하는지 확인")
    void existsByPopupIdAndDayOfWeek() {
        // when & then
        assertThat(popupHoursRepository.existsByPopupIdAndDayOfWeek(1L, 1)).isTrue(); // 팝업1, 월요일
        assertThat(popupHoursRepository.existsByPopupIdAndDayOfWeek(1L, 0)).isFalse(); // 팝업1, 일요일
        assertThat(popupHoursRepository.existsByPopupIdAndDayOfWeek(2L, 6)).isTrue(); // 팝업2, 토요일
        assertThat(popupHoursRepository.existsByPopupIdAndDayOfWeek(2L, 1)).isFalse(); // 팝업2, 월요일
    }

    @Test
    @DisplayName("팝업 ID로 운영시간 삭제")
    void deleteByPopupId() {
        // given
        assertThat(popupHoursRepository.findByPopupId(1L)).hasSize(5);

        // when
        popupHoursRepository.deleteByPopupId(1L);
        entityManager.flush(); // 삭제 쿼리 즉시 실행

        // then
        assertThat(popupHoursRepository.findByPopupId(1L)).isEmpty();
        assertThat(popupHoursRepository.findByPopupId(2L)).hasSize(2); // 다른 팝업은 영향 없음
    }

    @Test
    @DisplayName("존재하지 않는 팝업 ID로 조회시 빈 리스트 반환")
    void findByNonExistentPopupId() {
        // when
        List<PopupHours> hours = popupHoursRepository.findByPopupId(999L);
        List<Integer> days = popupHoursRepository.findDistinctDayOfWeekByPopupId(999L);

        // then
        assertThat(hours).isEmpty();
        assertThat(days).isEmpty();
        assertThat(popupHoursRepository.existsByPopupIdAndDayOfWeek(999L, 1)).isFalse();
    }

    private void insertTestData() {
        try {
            entityManager.getEntityManager().createNativeQuery(
                            "INSERT INTO popups (id, brand_id, title, summary, start_date, end_date, status, entry_fee, reservation_available, main_image_url, is_featured, created_at, updated_at) " +
                                    "VALUES (1, 1, 'Test Popup 1', 'Summary 1', '2024-01-01', '2024-12-31', 'ONGOING', 0, true, 'test.jpg', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .executeUpdate();

            entityManager.getEntityManager().createNativeQuery(
                            "INSERT INTO popups (id, brand_id, title, summary, start_date, end_date, status, entry_fee, reservation_available, main_image_url, is_featured, created_at, updated_at) " +
                                    "VALUES (2, 1, 'Test Popup 2', 'Summary 2', '2024-01-01', '2024-12-31', 'ONGOING', 0, true, 'test.jpg', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .executeUpdate();

            for (int day = 1; day <= 5; day++) {
                entityManager.getEntityManager().createNativeQuery(
                                "INSERT INTO popup_hours (popup_id, day_of_week, open_time, close_time, created_at, updated_at) " +
                                        "VALUES (1, " + day + ", '10:00', '22:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        .executeUpdate();
            }

            entityManager.getEntityManager().createNativeQuery(
                            "INSERT INTO popup_hours (popup_id, day_of_week, open_time, close_time, created_at, updated_at) " +
                                    "VALUES (2, 6, '11:00', '21:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .executeUpdate();

            entityManager.getEntityManager().createNativeQuery(
                            "INSERT INTO popup_hours (popup_id, day_of_week, open_time, close_time, created_at, updated_at) " +
                                    "VALUES (2, 0, '11:00', '21:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .executeUpdate();

            entityManager.flush();

        } catch (Exception e) {
            System.err.println("테스트 데이터 삽입 실패: " + e.getMessage());
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }
}