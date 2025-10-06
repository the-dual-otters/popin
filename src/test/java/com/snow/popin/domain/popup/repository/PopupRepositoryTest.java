package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.testdata.PopupTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(PopupRepositoryTest.TestConfig.class)
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PopupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PopupRepository popupRepository;

    @Autowired
    private PopupQueryDslRepository popupQueryDslRepository;

    // 메인 페이지 필터링 메서드 테스트
    @Test
    @DisplayName("전체 팝업 조회 - 상태별 필터링")
    void findAllWithStatusFilter_상태필터_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup ongoingPopup = PopupTestDataBuilder.createPopup("진행중 팝업", PopupStatus.ONGOING, venue);
        Popup plannedPopup = PopupTestDataBuilder.createPopup("예정 팝업", PopupStatus.PLANNED, venue);
        entityManager.persistAndFlush(ongoingPopup);
        entityManager.persistAndFlush(plannedPopup);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Popup> result = popupQueryDslRepository.findAllWithStatusFilter(PopupStatus.ONGOING, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PopupStatus.ONGOING);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행중 팝업");
    }

    @Test
    @DisplayName("전체 팝업 조회 - 전체 상태 (null)")
    void findAllWithStatusFilter_전체상태_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup ongoingPopup = PopupTestDataBuilder.createPopup("진행중 팝업", PopupStatus.ONGOING, venue);
        Popup plannedPopup = PopupTestDataBuilder.createPopup("예정 팝업", PopupStatus.PLANNED, venue);
        Popup endedPopup = PopupTestDataBuilder.createPopup("종료 팝업", PopupStatus.ENDED, venue);
        entityManager.persistAndFlush(ongoingPopup);
        entityManager.persistAndFlush(plannedPopup);
        entityManager.persistAndFlush(endedPopup);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Popup> result = popupQueryDslRepository.findAllWithStatusFilter(null, pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("인기 팝업 조회 - 조회수 기준 정렬")
    void findPopularByViewCount_조회수정렬_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup highViewPopup = PopupTestDataBuilder.createPopupWithViewCount("고조회수 팝업", PopupStatus.ONGOING, venue, 1000L);
        Popup lowViewPopup = PopupTestDataBuilder.createPopupWithViewCount("저조회수 팝업", PopupStatus.ONGOING, venue, 100L);
        entityManager.persistAndFlush(highViewPopup);
        entityManager.persistAndFlush(lowViewPopup);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Popup> result = popupQueryDslRepository.findPopularActivePopups(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("고조회수 팝업");
        assertThat(result.getContent().get(0).getViewCount()).isEqualTo(1000L);
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("저조회수 팝업");
        assertThat(result.getContent().get(1).getViewCount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("마감임박 팝업 조회")
    void findDeadlineSoonPopups_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        LocalDate today = LocalDate.now();
        LocalDate soonDeadline = today.plusDays(3);
        LocalDate farDeadline = today.plusDays(10);

        Popup soonPopup = PopupTestDataBuilder.createPopupWithDates("임박 팝업", today.minusDays(5), soonDeadline, venue);
        soonPopup.setStatusForTest(PopupStatus.ONGOING);
        Popup farPopup = PopupTestDataBuilder.createPopupWithDates("여유 팝업", today.minusDays(5), farDeadline, venue);
        farPopup.setStatusForTest(PopupStatus.ONGOING);

        entityManager.persistAndFlush(soonPopup);
        entityManager.persistAndFlush(farPopup);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Popup> result = popupQueryDslRepository.findDeadlineSoonPopups(PopupStatus.ONGOING, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        // 종료일 오름차순 정렬 확인
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("임박 팝업");
    }

    @Test
    @DisplayName("지역별 + 기간별 필터링")
    void findByRegionAndDateRange_복합필터_테스트() {
        // given
        Venue venueGangnam = PopupTestDataBuilder.createVenue("강남구");
        Venue venueJongno = PopupTestDataBuilder.createVenue("종로구");
        entityManager.persistAndFlush(venueGangnam);
        entityManager.persistAndFlush(venueJongno);

        LocalDate today = LocalDate.now();
        LocalDate week = today.plusDays(7);

        Popup gangnamePopup = PopupTestDataBuilder.createPopupWithDates("강남 팝업", today, week, venueGangnam);
        gangnamePopup.setStatusForTest(PopupStatus.ONGOING);
        Popup jongnoPopup = PopupTestDataBuilder.createPopupWithDates("종로 팝업", today, week, venueJongno);
        jongnoPopup.setStatusForTest(PopupStatus.ONGOING);

        entityManager.persistAndFlush(gangnamePopup);
        entityManager.persistAndFlush(jongnoPopup);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Popup> result = popupQueryDslRepository.findByRegionAndDateRange(
                "강남구", today, week, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getVenue().getRegion()).isEqualTo("강남구");
    }

    @Test
    @DisplayName("지역별 + 기간별 필터링 - 모든 필터 null")
    void findByRegionAndDateRange_모든필터null_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup popup1 = PopupTestDataBuilder.createPopup("팝업1", PopupStatus.ONGOING, venue);
        Popup popup2 = PopupTestDataBuilder.createPopup("팝업2", PopupStatus.PLANNED, venue);
        entityManager.persistAndFlush(popup1);
        entityManager.persistAndFlush(popup2);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Popup> result = popupQueryDslRepository.findByRegionAndDateRange(
                null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    // 팝업 상세 조회 테스트
    @Test
    @DisplayName("팝업 상세 조회 - 연관 엔티티 포함")
    void findByIdWithDetails_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup popup = PopupTestDataBuilder.createPopup("상세 팝업", PopupStatus.ONGOING, venue);
        entityManager.persistAndFlush(popup);

        // when
        Optional<Popup> result = popupRepository.findByIdWithDetails(popup.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getVenue()).isNotNull();
        assertThat(result.get().getVenue().getRegion()).isEqualTo("강남구");
    }

    // 추천 및 유사 팝업 테스트
    @Test
    @DisplayName("유사한 팝업 조회 - 같은 카테고리")
    void findSimilarPopups_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        // 카테고리를 모킹해야 함 - 실제 테스트에서는 Category 엔티티 필요
        Popup popup1 = PopupTestDataBuilder.createPopup("패션 팝업1", PopupStatus.ONGOING, venue);
        Popup popup2 = PopupTestDataBuilder.createPopup("패션 팝업2", PopupStatus.ONGOING, venue);
        Popup popup3 = PopupTestDataBuilder.createPopup("뷰티 팝업", PopupStatus.ONGOING, venue);

        entityManager.persistAndFlush(popup1);
        entityManager.persistAndFlush(popup2);
        entityManager.persistAndFlush(popup3);

        Pageable pageable = PageRequest.of(0, 10);

        // when - 실제 구현시에는 카테고리 설정 필요
        // Page<Popup> result = popupRepository.findSimilarPopups("패션", popup1.getId(), pageable);

        // then
        // assertThat(result.getContent()).hasSize(1);
        // assertThat(result.getContent().get(0).getId()).isEqualTo(popup2.getId());
    }

    @Test
    @DisplayName("카테고리별 추천 팝업 조회")
    void findRecommendedPopupsByCategories_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup highViewPopup = PopupTestDataBuilder.createPopupWithViewCount("고조회수 추천", PopupStatus.ONGOING, venue, 2000L);
        Popup lowViewPopup = PopupTestDataBuilder.createPopupWithViewCount("저조회수 추천", PopupStatus.ONGOING, venue, 500L);

        entityManager.persistAndFlush(highViewPopup);
        entityManager.persistAndFlush(lowViewPopup);

        List<Long> categoryIds = Arrays.asList(1L, 2L);
        Pageable pageable = PageRequest.of(0, 10);

        // when - 실제 구현시에는 카테고리 설정 필요
        // Page<Popup> result = popupRepository.findRecommendedPopupsByCategories(categoryIds, pageable);

        // then
        // assertThat(result.getContent()).hasSize(2);
        // 조회수 높은 순서로 정렬 확인
        // assertThat(result.getContent().get(0).getViewCount()).isEqualTo(2000L);
    }

    // 카테고리 및 지역별 조회 테스트
    @Test
    @DisplayName("카테고리별 팝업 조회")
    void findByCategoryName_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup popup1 = PopupTestDataBuilder.createPopupWithViewCount("뷰티 팝업1", PopupStatus.ONGOING, venue, 1000L);
        Popup popup2 = PopupTestDataBuilder.createPopupWithViewCount("뷰티 팝업2", PopupStatus.PLANNED, venue, 500L);

        entityManager.persistAndFlush(popup1);
        entityManager.persistAndFlush(popup2);

        Pageable pageable = PageRequest.of(0, 10);

        // when - 실제 구현시에는 카테고리 설정 필요
        // Page<Popup> result = popupRepository.findByCategoryName("뷰티", pageable);

        // then
        // assertThat(result.getContent()).hasSize(2);
        // 조회수 높은 순서로 정렬 확인
        // assertThat(result.getContent().get(0).getViewCount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("지역별 팝업 조회")
    void findByRegion_테스트() {
        // given
        Venue venueGangnam = PopupTestDataBuilder.createVenue("강남구");
        Venue venueJongno = PopupTestDataBuilder.createVenue("종로구");
        entityManager.persistAndFlush(venueGangnam);
        entityManager.persistAndFlush(venueJongno);

        Popup gangnamePopup1 = PopupTestDataBuilder.createPopupWithViewCount("강남 팝업1", PopupStatus.ONGOING, venueGangnam, 800L);
        Popup gangnamePopup2 = PopupTestDataBuilder.createPopupWithViewCount("강남 팝업2", PopupStatus.PLANNED, venueGangnam, 600L);
        Popup jongnoPopup = PopupTestDataBuilder.createPopup("종로 팝업", PopupStatus.ONGOING, venueJongno);

        entityManager.persistAndFlush(gangnamePopup1);
        entityManager.persistAndFlush(gangnamePopup2);
        entityManager.persistAndFlush(jongnoPopup);

        // when
        List<Popup> result = popupQueryDslRepository.findByRegion("강남구");

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("title").containsExactlyInAnyOrder("강남 팝업1", "강남 팝업2");
        // 조회수 높은 순서로 정렬 확인
        assertThat(result.get(0).getViewCount()).isGreaterThanOrEqualTo(result.get(1).getViewCount());
    }

    @Test
    @DisplayName("상태 업데이트 대상 조회 - PLANNED에서 ONGOING으로")
    void findPopupsToUpdateToOngoing_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        LocalDate today = LocalDate.now();
        // 대상: 오늘 시작, 어제 시작
        Popup popupToStartToday = PopupTestDataBuilder.createPopupWithDates("오늘 시작", today, today.plusDays(5), venue);
        popupToStartToday.setStatusForTest(PopupStatus.PLANNED);
        Popup popupStartedYesterday = PopupTestDataBuilder.createPopupWithDates("어제 시작", today.minusDays(1), today.plusDays(5), venue);
        popupStartedYesterday.setStatusForTest(PopupStatus.PLANNED);

        // 비대상: 내일 시작, 이미 ONGOING 상태
        Popup popupStartsTomorrow = PopupTestDataBuilder.createPopupWithDates("내일 시작", today.plusDays(1), today.plusDays(5), venue);
        popupStartsTomorrow.setStatusForTest(PopupStatus.PLANNED);
        Popup ongoingPopup = PopupTestDataBuilder.createPopupWithDates("진행중", today, today.plusDays(5), venue);
        ongoingPopup.setStatusForTest(PopupStatus.ONGOING);

        entityManager.persist(popupToStartToday);
        entityManager.persist(popupStartedYesterday);
        entityManager.persist(popupStartsTomorrow);
        entityManager.persist(ongoingPopup);
        entityManager.flush();

        // when
        List<Popup> result = popupQueryDslRepository.findPopupsToUpdateToOngoing(today);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("title").containsExactlyInAnyOrder("오늘 시작", "어제 시작");
    }

    @Test
    @DisplayName("상태 업데이트 대상 조회 - ONGOING에서 ENDED로")
    void findPopupsToUpdateToEnded_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("종로구");
        entityManager.persistAndFlush(venue);

        LocalDate today = LocalDate.now();
        // 대상: 어제 종료
        Popup popupEndedYesterday = PopupTestDataBuilder.createPopupWithDates("어제 종료", today.minusDays(10), today.minusDays(1), venue);
        popupEndedYesterday.setStatusForTest(PopupStatus.ONGOING);

        // 비대상: 오늘 종료, 이미 ENDED 상태
        Popup popupEndsToday = PopupTestDataBuilder.createPopupWithDates("오늘 종료", today.minusDays(10), today, venue);
        popupEndsToday.setStatusForTest(PopupStatus.ONGOING);
        Popup endedPopup = PopupTestDataBuilder.createPopupWithDates("이미 종료", today.minusDays(10), today.minusDays(1), venue);
        endedPopup.setStatusForTest(PopupStatus.ENDED);

        entityManager.persist(popupEndedYesterday);
        entityManager.persist(popupEndsToday);
        entityManager.persist(endedPopup);
        entityManager.flush();

        // when
        List<Popup> result = popupQueryDslRepository.findPopupsToUpdateToEnded(today);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("어제 종료");
    }

    // 통계 메서드 테스트
    @Test
    @DisplayName("상태별 팝업 개수 조회")
    void countByStatus_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup ongoingPopup1 = PopupTestDataBuilder.createPopup("진행중1", PopupStatus.ONGOING, venue);
        Popup ongoingPopup2 = PopupTestDataBuilder.createPopup("진행중2", PopupStatus.ONGOING, venue);
        Popup plannedPopup = PopupTestDataBuilder.createPopup("예정", PopupStatus.PLANNED, venue);

        entityManager.persistAndFlush(ongoingPopup1);
        entityManager.persistAndFlush(ongoingPopup2);
        entityManager.persistAndFlush(plannedPopup);

        // when
        long ongoingCount = popupRepository.countByStatus(PopupStatus.ONGOING);
        long plannedCount = popupRepository.countByStatus(PopupStatus.PLANNED);

        // then
        assertThat(ongoingCount).isEqualTo(2);
        assertThat(plannedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("전체 팝업 개수 조회")
    void count_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup popup1 = PopupTestDataBuilder.createPopup("팝업1", PopupStatus.ONGOING, venue);
        Popup popup2 = PopupTestDataBuilder.createPopup("팝업2", PopupStatus.PLANNED, venue);
        Popup popup3 = PopupTestDataBuilder.createPopup("팝업3", PopupStatus.ENDED, venue);

        entityManager.persistAndFlush(popup1);
        entityManager.persistAndFlush(popup2);
        entityManager.persistAndFlush(popup3);

        // when
        long totalCount = popupRepository.count();

        // then
        assertThat(totalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("특정 상태의 팝업 조회 (AI 추천용)")
    void findByStatus_AI추천용_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup ongoingPopup1 = PopupTestDataBuilder.createPopup("진행중 팝업1", PopupStatus.ONGOING, venue);
        Popup ongoingPopup2 = PopupTestDataBuilder.createPopup("진행중 팝업2", PopupStatus.ONGOING, venue);
        Popup plannedPopup = PopupTestDataBuilder.createPopup("예정 팝업", PopupStatus.PLANNED, venue);

        entityManager.persistAndFlush(ongoingPopup1);
        entityManager.persistAndFlush(ongoingPopup2);
        entityManager.persistAndFlush(plannedPopup);

        // when
        List<Popup> ongoingResults = popupRepository.findByStatus(PopupStatus.ONGOING);
        List<Popup> plannedResults = popupRepository.findByStatus(PopupStatus.PLANNED);

        // then
        assertThat(ongoingResults).hasSize(2);
        assertThat(ongoingResults).extracting("title")
                .containsExactlyInAnyOrder("진행중 팝업1", "진행중 팝업2");

        assertThat(plannedResults).hasSize(1);
        assertThat(plannedResults.get(0).getTitle()).isEqualTo("예정 팝업");
    }

    @Test
    @DisplayName("ID 목록으로 팝업 조회 (AI 추천 결과용)")
    void findByIdIn_AI추천결과_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup popup1 = PopupTestDataBuilder.createPopup("AI 추천 팝업1", PopupStatus.ONGOING, venue);
        Popup popup2 = PopupTestDataBuilder.createPopup("AI 추천 팝업2", PopupStatus.ONGOING, venue);
        Popup popup3 = PopupTestDataBuilder.createPopup("일반 팝업", PopupStatus.ONGOING, venue);

        entityManager.persistAndFlush(popup1);
        entityManager.persistAndFlush(popup2);
        entityManager.persistAndFlush(popup3);

        List<Long> aiRecommendedIds = Arrays.asList(popup1.getId(), popup2.getId());

        // when
        List<Popup> result = popupQueryDslRepository.findByIdIn(aiRecommendedIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("title")
                .containsExactlyInAnyOrder("AI 추천 팝업1", "AI 추천 팝업2");

        // 연관 엔티티도 함께 조회되는지 확인
        assertThat(result.get(0).getVenue()).isNotNull();
        assertThat(result.get(0).getVenue().getRegion()).isEqualTo("강남구");
    }

    @Test
    @DisplayName("ID 목록으로 팝업 조회 - 빈 목록")
    void findByIdIn_빈목록_테스트() {
        // given
        List<Long> emptyIds = Arrays.asList();

        // when
        List<Popup> result = popupQueryDslRepository.findByIdIn(emptyIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ID 목록으로 팝업 조회 - 존재하지 않는 ID")
    void findByIdIn_존재하지않는ID_테스트() {
        // given
        List<Long> nonExistentIds = Arrays.asList(999L, 998L);

        // when
        List<Popup> result = popupQueryDslRepository.findByIdIn(nonExistentIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 상태의 팝업 조회 - 빈 결과")
    void findByStatus_빈결과_테스트() {
        // given
        Venue venue = PopupTestDataBuilder.createVenue("강남구");
        entityManager.persistAndFlush(venue);

        Popup ongoingPopup = PopupTestDataBuilder.createPopup("진행중 팝업", PopupStatus.ONGOING, venue);
        entityManager.persistAndFlush(ongoingPopup);

        // when
        List<Popup> result = popupRepository.findByStatus(PopupStatus.ENDED);

        // then
        assertThat(result).isEmpty();
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        public AuditorAware<String> auditorProvider() {
            return () -> Optional.of("test-user");
        }
    }
}