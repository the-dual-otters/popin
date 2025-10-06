package com.snow.popin.domain.recommendation.service;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.recommendation.dto.AiRecommendationResponseDto;
import com.snow.popin.domain.recommendation.dto.ReservationHistoryDto;
import com.snow.popin.domain.recommendation.dto.UserPreferenceDto;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiRecommendationService {

    private final GeminiAiService geminiAiService;
    private final PopupRepository popupRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;

    private static final Map<Long, String> CATEGORY_MAP = Map.of(
            1L, "패션",
            2L, "반려동물",
            3L, "게임",
            4L, "캐릭터/IP",
            5L, "문화/컨텐츠",
            6L, "연예",
            7L, "여행/레저/스포츠"
    );

    // 최소 추천 개수 상수
    private static final int MIN_RECOMMENDATIONS = 4;
    private static final int DEFAULT_RECOMMENDATIONS = 8;
    private static final int MAX_RECOMMENDATIONS = 15;

    /**
     * 사용자 기반 AI 팝업 추천
     */
    @Cacheable(value = "aiRecommendations", key = "#userId + '_' + #limit", unless = "#result == null || !#result.success")
    public AiRecommendationResponseDto getPersonalizedRecommendations(Long userId, int limit) {
        log.info("사용자 {} AI 추천 시작 (limit: {})", userId, limit);

        try {
            // 입력값 검증 및 최소값 보장
            if (userId == null || userId <= 0) {
                log.warn("잘못된 사용자 ID: {}", userId);
                return AiRecommendationResponseDto.failure("잘못된 사용자 정보로 인해 AI 추천을 제공할 수 없습니다.");
            }

            // limit을 최소 4개 이상으로 보장
            int adjustedLimit = Math.max(limit, MIN_RECOMMENDATIONS);
            if (adjustedLimit > MAX_RECOMMENDATIONS) {
                adjustedLimit = MAX_RECOMMENDATIONS;
            }

            log.info("조정된 추천 개수: {} (원본: {})", adjustedLimit, limit);

            // 사용자 선호도 분석
            UserPreferenceDto userPreference = analyzeUserPreferences(userId);
            if (userPreference.getInterests().isEmpty() &&
                    (userPreference.getReservationHistory() == null || userPreference.getReservationHistory().isEmpty())) {
                log.info("사용자 {}의 선호도 데이터 부족, 인기 팝업으로 대체", userId);
                return AiRecommendationResponseDto.failure("사용자 선호도 데이터가 부족하여 AI 추천을 제공할 수 없습니다.");
            }

            // 현재 진행중인 팝업 목록 조회
            List<Popup> availablePopups = popupRepository.findByStatus(PopupStatus.ONGOING);
            if (availablePopups.size() < MIN_RECOMMENDATIONS) {
                log.warn("진행중인 팝업이 {}개 미만 ({}개), 인기 팝업으로 대체", MIN_RECOMMENDATIONS, availablePopups.size());
                return AiRecommendationResponseDto.failure("추천 가능한 팝업이 부족하여 AI 추천을 제공할 수 없습니다.");
            }

            // AI 프롬프트 생성 및 호출
            String prompt = createEnhancedRecommendationPrompt(userPreference, availablePopups, adjustedLimit);
            log.debug("생성된 프롬프트 길이: {} 문자", prompt.length());

            String aiResponse = geminiAiService.generateText(prompt);

            if (!StringUtils.hasText(aiResponse)) {
                log.warn("AI 응답이 비어있음, 인기 팝업으로 대체");
                return AiRecommendationResponseDto.failure("AI 응답을 받지 못하여 추천을 제공할 수 없습니다.");
            }

            log.info("AI 응답 수신 - 길이: {} 문자", aiResponse.length());
            log.debug("AI 응답 내용: {}", aiResponse);

            // AI 응답 파싱
            AiRecommendationResponseDto result = parseAiResponseWithFallback(aiResponse, availablePopups, adjustedLimit);

            log.info("사용자 {} AI 추천 완료 - 추천 개수: {}", userId,
                    result.isSuccess() ? result.getRecommendedPopupIds().size() : 0);

            return result;

        } catch (Exception e) {
            log.error("사용자 {} AI 추천 처리 중 오류", userId, e);
            return AiRecommendationResponseDto.failure("AI 추천 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     *  AI 추천 프롬프트 생성
     */
    private String createEnhancedRecommendationPrompt(UserPreferenceDto userPreference,
                                                      List<Popup> availablePopups, int limit) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 한국의 팝업스토어 추천 전문 AI입니다.\n\n");

        // 매우 명확한 작업 지시
        prompt.append("**중요: 반드시 정확히 ").append(limit).append("개의 서로 다른 팝업을 추천해주세요.**\n");
        prompt.append("**절대 ").append(limit).append("개보다 적게 추천하지 마세요.**\n\n");

        // 사용자 프로필
        prompt.append("## 사용자 프로필\n");
        if (!userPreference.getInterests().isEmpty()) {
            prompt.append("관심 분야: ").append(String.join(", ", userPreference.getInterests())).append("\n");
        }

        // 예약 이력 분석
        if (userPreference.getReservationHistory() != null && !userPreference.getReservationHistory().isEmpty()) {
            prompt.append("\n최근 방문 패턴 (카테고리별 방문 횟수):\n");
            Map<String, Long> categoryCount = userPreference.getReservationHistory().stream()
                    .filter(h -> StringUtils.hasText(h.getCategory()))
                    .collect(Collectors.groupingBy(
                            ReservationHistoryDto::getCategory,
                            Collectors.counting()
                    ));

            categoryCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry ->
                            prompt.append("- ").append(entry.getKey()).append(" (").append(entry.getValue()).append("회)\n")
                    );
        }

        // 현재 팝업 목록 (처음 50개만 표시하여 토큰 절약)
        Map<Long, String> brandMap = getBrandNamesMap(availablePopups);

        prompt.append("\n## 현재 추천 가능한 팝업 목록 (총 ").append(availablePopups.size()).append("개)\n");
        int displayCount = Math.min(availablePopups.size(), 50);

        for (int i = 0; i < displayCount; i++) {
            Popup popup = availablePopups.get(i);
            String category = popup.getCategory() != null && popup.getCategory().getId() != null ?
                    CATEGORY_MAP.getOrDefault(popup.getCategory().getId(), "기타") : "기타";
            String brandName = brandMap.getOrDefault(popup.getBrandId(), "브랜드");
            String region = popup.getVenue() != null && StringUtils.hasText(popup.getVenue().getRegion()) ?
                    popup.getVenue().getRegion() : "지역미정";

            prompt.append(String.format("ID:%d | %s | %s | %s | %s\n",
                    popup.getId(), popup.getTitle(), category, brandName, region));
        }

        if (availablePopups.size() > 50) {
            prompt.append("... (총 ").append(availablePopups.size()).append("개 중 50개만 표시)\n");
        }

        prompt.append(String.format("\n## 추천 요청\n" +
                        "위 목록에서 사용자에게 가장 적합한 팝업을 **정확히 %d개** 선별하여 추천해주세요.\n\n" +
                        "**추천 우선순위:**\n" +
                        "1. 과거 예약 이력과 유사한 패턴\n" +
                        "2. 사용자 관심사와 일치하는 카테고리\n" +
                        "3. 브랜드 선호도\n" +
                        "4. 접근성 좋은 지역\n" +
                        "**반드시 지켜야 할 응답 형식:**\n" +
                        "추천 팝업 ID: [12,34,56,78,90,123,456,789]\n" +
                        "추천 이유: 구체적인 추천 근거를 200자 내외로 설명\n\n" +
                        "**절대 규칙:**\n" +
                        "- 반드시 대괄호 [] 안에 정확히 %d개의 ID를 쉼표로 구분하여 나열\n" +
                        "- 모든 ID는 위 목록에 있는 유효한 ID여야 함\n" +
                        "- 중복된 ID 절대 사용 금지\n" +
                        "- %d개보다 적게 추천하면 안 됨\n",
                limit, limit, limit));

        return prompt.toString();
    }

    /**
     *  AI 응답 파싱
     */
    private AiRecommendationResponseDto parseAiResponseWithFallback(String aiResponse,
                                                                    List<Popup> availablePopups,
                                                                    int expectedCount) {
        try {
            log.info("=== AI 응답 파싱 시작 ===");
            log.info("기대 추천 개수: {}", expectedCount);

            List<Long> recommendedIds = extractPopupIdsEnhanced(aiResponse, availablePopups);

            log.info("1차 파싱 결과: {}개 추출", recommendedIds.size());
            log.info("추출된 ID들: {}", recommendedIds);

            // 개수가 부족한 경우 추가 추출 시도
            if (recommendedIds.size() < expectedCount) {
                log.warn("추천 개수 부족 ({}/{}), 추가 추출 시도", recommendedIds.size(), expectedCount);

                List<Long> additionalIds = extractAdditionalIds(aiResponse, availablePopups, recommendedIds);
                recommendedIds.addAll(additionalIds);

                log.info("2차 파싱 후: {}개", recommendedIds.size());
            }

            // 부족한 경우 인기 팝업으로 보완
            if (recommendedIds.size() < MIN_RECOMMENDATIONS) {
                log.warn("최소 추천 개수 미달 ({}/{}), 인기 팝업으로 보완", recommendedIds.size(), MIN_RECOMMENDATIONS);
                recommendedIds = supplementWithPopularPopups(recommendedIds, availablePopups, expectedCount);

                log.info("보완 후 최종: {}개", recommendedIds.size());
            }

            // 추천 이유 추출
            String reasoning = extractEnhancedReasoning(aiResponse);

            if (recommendedIds.size() < MIN_RECOMMENDATIONS) {
                log.error("최종 추천 개수가 최소값 미달: {}개", recommendedIds.size());
                return AiRecommendationResponseDto.failure("AI 응답 파싱 결과가 부족하여 추천을 제공할 수 없습니다.");
            }

            log.info("=== AI 응답 파싱 완료 - {}개 팝업 추천 성공 ===", recommendedIds.size());
            return AiRecommendationResponseDto.success(recommendedIds, reasoning);

        } catch (Exception e) {
            log.error("AI 응답 파싱 중 오류", e);
            return AiRecommendationResponseDto.failure("AI 응답 파싱 중 오류가 발생했습니다.");
        }
    }

    /**
     *  팝업 ID 추출
     */
    private List<Long> extractPopupIdsEnhanced(String aiResponse, List<Popup> availablePopups) {
        List<Long> recommendedIds = new ArrayList<>();
        Set<Long> availableIds = availablePopups.stream()
                .map(Popup::getId)
                .collect(Collectors.toSet());

        Pattern pattern1 = Pattern.compile("\\[(\\d+(?:\\s*,\\s*\\d+)*)\\]");
        Matcher matcher1 = pattern1.matcher(aiResponse);

        if (matcher1.find()) {
            String idsStr = matcher1.group(1);
            log.info("패턴1 매치 발견: [{}]", idsStr);

            Arrays.stream(idsStr.split(","))
                    .map(String::trim)
                    .forEach(id -> addValidId(id, availableIds, recommendedIds));

            log.info("패턴1에서 {}개 추출", recommendedIds.size());
        }

        return recommendedIds;
    }

    /**
     * 추가 ID 추출 (개수가 부족할 때)
     */
    private List<Long> extractAdditionalIds(String aiResponse, List<Popup> availablePopups, List<Long> existingIds) {
        List<Long> additionalIds = new ArrayList<>();
        Set<Long> availableIds = availablePopups.stream()
                .map(Popup::getId)
                .collect(Collectors.toSet());

        Set<Long> alreadyAdded = new HashSet<>(existingIds);

        Pattern pattern2 = Pattern.compile("ID\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(aiResponse);

        while (matcher2.find() && additionalIds.size() < 10) {
            String idStr = matcher2.group(1);
            try {
                Long popupId = Long.parseLong(idStr.trim());
                if (availableIds.contains(popupId) && !alreadyAdded.contains(popupId)) {
                    additionalIds.add(popupId);
                    alreadyAdded.add(popupId);
                    log.info("패턴2에서 추가 ID 발견: {}", popupId);
                }
            } catch (NumberFormatException e) {
                log.debug("잘못된 ID 형식 무시: {}", idStr);
            }
        }

        if (additionalIds.size() < 3) {
            Pattern pattern3 = Pattern.compile("\\b(\\d{1,6})\\b");
            Matcher matcher3 = pattern3.matcher(aiResponse);

            while (matcher3.find() && additionalIds.size() < 10) {
                String idStr = matcher3.group(1);
                try {
                    Long popupId = Long.parseLong(idStr);
                    if (popupId > 0 && availableIds.contains(popupId) && !alreadyAdded.contains(popupId)) {
                        additionalIds.add(popupId);
                        alreadyAdded.add(popupId);
                        log.info("패턴3에서 추가 ID 발견: {}", popupId);
                    }
                } catch (NumberFormatException e) {}
            }
        }

        log.info("추가 추출에서 {}개 ID 발견", additionalIds.size());
        return additionalIds;
    }

    /**
     * 인기 팝업으로 부족한 개수 보완
     */
    private List<Long> supplementWithPopularPopups(List<Long> existingIds, List<Popup> availablePopups, int targetCount) {
        List<Long> result = new ArrayList<>(existingIds);
        Set<Long> existingSet = new HashSet<>(existingIds);

        // 조회수 기준으로 정렬된 팝업들에서 추가
        List<Popup> sortedPopups = availablePopups.stream()
                .filter(p -> !existingSet.contains(p.getId()))
                .sorted((p1, p2) -> Long.compare(p2.getViewCount(), p1.getViewCount()))
                .limit(targetCount - existingIds.size())
                .collect(Collectors.toList());

        for (Popup popup : sortedPopups) {
            result.add(popup.getId());
            log.info("인기 팝업으로 보완: ID {} (조회수: {})", popup.getId(), popup.getViewCount());
        }

        return result;
    }

    /**
     * 유효한 ID인지 확인 후 추가
     */
    private void addValidId(String idStr, Set<Long> availableIds, List<Long> recommendedIds) {
        try {
            Long popupId = Long.parseLong(idStr.trim());
            if (availableIds.contains(popupId) && !recommendedIds.contains(popupId)) {
                recommendedIds.add(popupId);
                log.debug("유효한 팝업 ID 추가: {}", popupId);
            } else {
                log.debug("무효한 또는 중복된 팝업 ID: {} (사용가능: {}, 이미추가: {})",
                        popupId, availableIds.contains(popupId), recommendedIds.contains(popupId));
            }
        } catch (NumberFormatException e) {
            log.debug("잘못된 ID 형식: {}", idStr);
        }
    }

    /**
     * 향상된 추천 이유 추출
     */
    private String extractEnhancedReasoning(String aiResponse) {
        String[] patterns = {
                "추천\\s*이유\\s*:?\\s*(.+)",
                "이유\\s*:?\\s*(.+)",
                "근거\\s*:?\\s*(.+)"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(aiResponse);
            if (matcher.find()) {
                String reason = matcher.group(1).trim();
                String[] lines = reason.split("\\n");
                String firstLine = lines[0].length() > 300 ? lines[0].substring(0, 300) + "..." : lines[0];

                if (firstLine.length() > 50) { // 의미있는 길이인지 확인
                    return firstLine;
                }
            }
        }

        return "다양한 카테고리의 인기 팝업들을 개인 취향에 맞게 추천드립니다.";
    }

    private UserPreferenceDto analyzeUserPreferences(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return UserPreferenceDto.builder()
                    .userId(userId)
                    .interests(Collections.emptyList())
                    .reservationHistory(Collections.emptyList())
                    .build();
        }

        List<String> interests = getUserInterests(user);
        List<ReservationHistoryDto> reservationHistory = getReservationHistory(user);

        return UserPreferenceDto.builder()
                .userId(userId)
                .interests(interests)
                .reservationHistory(reservationHistory)
                .build();
    }

    private List<String> getUserInterests(User user) {
        try {
            if (user.getInterests() == null || user.getInterests().isEmpty()) {
                return Collections.emptyList();
            }
            return user.getInterestCategoryNames();
        } catch (Exception e) {
            log.warn("사용자 {} 관심사 조회 실패", user.getId(), e);
            return Collections.emptyList();
        }
    }

    private List<ReservationHistoryDto> getReservationHistory(User user) {
        try {
            List<Reservation> reservations = reservationRepository.findByUser(user);
            return reservations.stream()
                    .sorted((r1, r2) -> r2.getReservationDate().compareTo(r1.getReservationDate()))
                    .limit(10)
                    .map(this::convertToReservationHistoryDto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("사용자 {} 예약 이력 조회 실패", user.getId(), e);
            return Collections.emptyList();
        }
    }

    private ReservationHistoryDto convertToReservationHistoryDto(Reservation reservation) {
        try {
            Popup popup = reservation.getPopup();
            if (popup == null) return null;

            String categoryName = popup.getCategory() != null && popup.getCategory().getId() != null ?
                    CATEGORY_MAP.get(popup.getCategory().getId()) : null;
            String brandName = getBrandName(popup.getBrandId());

            return ReservationHistoryDto.builder()
                    .popupId(popup.getId())
                    .popupTitle(popup.getTitle())
                    .category(categoryName)
                    .brand(brandName)
                    .reservationDate(reservation.getReservationDate().toLocalDate())
                    .status(reservation.getStatus().name())
                    .venue(popup.getVenue() != null ? popup.getVenue().getName() : "")
                    .build();
        } catch (Exception e) {
            log.warn("예약 이력 DTO 변환 실패 - reservationId: {}", reservation.getId(), e);
            return null;
        }
    }

    private String getBrandName(Long brandId) {
        if (brandId == null) return "브랜드";
        try {
            return brandRepository.findById(brandId).map(Brand::getName).orElse("브랜드");
        } catch (Exception e) {
            log.warn("브랜드 조회 실패 - brandId: {}", brandId, e);
            return "브랜드";
        }
    }

    private Map<Long, String> getBrandNamesMap(List<Popup> popups) {
        Set<Long> brandIds = popups.stream()
                .map(Popup::getBrandId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (brandIds.isEmpty()) return Collections.emptyMap();

        try {
            return brandRepository.findAllById(brandIds).stream()
                    .collect(Collectors.toMap(Brand::getId, Brand::getName,
                            (existing, replacement) -> existing));
        } catch (Exception e) {
            log.warn("브랜드 배치 조회 실패", e);
            return Collections.emptyMap();
        }
    }
}