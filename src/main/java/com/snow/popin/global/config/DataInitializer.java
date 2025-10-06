package com.snow.popin.global.config;

import com.snow.popin.domain.auth.constant.AuthProvider;
import com.snow.popin.domain.inquiry.entity.Inquiry;
import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import com.snow.popin.domain.inquiry.repository.InquiryRepository;
import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.map.repository.MapRepository;
import com.snow.popin.domain.mission.entity.Mission;
import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.mission.repository.MissionRepository;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterRequestDto;
import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.entity.Host;
import com.snow.popin.domain.mypage.host.entity.HostRole;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.notification.entity.Notification;
import com.snow.popin.domain.notification.entity.NotificationSetting;
import com.snow.popin.domain.notification.repository.NotificationRepository;
import com.snow.popin.domain.notification.repository.NotificationSettingRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.reward.entity.RewardOption;
import com.snow.popin.domain.reward.repository.RewardOptionRepository;
import com.snow.popin.domain.roleupgrade.entity.RoleUpgrade;
import com.snow.popin.domain.roleupgrade.repository.RoleUpgradeRepository;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.space.repository.SpaceRepository;
import com.snow.popin.domain.user.constant.UserStatus; // 누락된 import 추가
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev"})
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MissionSetRepository missionSetRepository;
    private final MissionRepository missionRepository;
    private final RewardOptionRepository rewardOptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PopupRepository popupRepository;
    private final BrandRepository brandRepository;
    private final HostRepository hostRepository;
    private final ReservationRepository reservationRepository;
    private final InquiryRepository inquiryRepository;
    private final RoleUpgradeRepository roleUpgradeRepository;
    private final SpaceRepository spaceRepository;
    private final MapRepository venueRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== DataInitializer 실행 시작 ===");

        createDataIfNotExists("유저", userRepository.count(), this::createDummyUsers);
        createDataIfNotExists("장소", venueRepository.count(), this::createDummyVenues);
        createDataIfNotExists("미션", missionSetRepository.count(), this::createDummyMissions);
        createFakeHostAndReservations();
        createDataIfNotExists("신고", inquiryRepository.count(), this::createDummyInquiries);
        createDataIfNotExists("역할승격", roleUpgradeRepository.count(), this::createDummyRoleUpgrades);
        createDataIfNotExists("공간", spaceRepository.count(), this::createDummySpaces);
        createDataIfNotExists("알림설정", notificationSettingRepository.count(), this::createDummyNotificationSettings);
        createDataIfNotExists("알림", notificationRepository.count(), this::createDummyNotifications);
        fixExistingPopupsVenue();

        log.info("=== DataInitializer 실행 완료 ===");
    }

    // 공통 데이터 생성 로직
    private void createDataIfNotExists(String dataType, long count, Runnable createAction) {
        if (count > 0) {
            log.info("{} 더미 데이터가 이미 존재하여 생성하지 않습니다. (현재 {}개)", dataType, count);
            return;
        }
        log.info("{} 더미 데이터를 생성합니다.", dataType);
        try {
            createAction.run();
            log.info("{} 더미 데이터 생성 완료", dataType);
        } catch (Exception e) {
            log.error("{} 더미 데이터 생성 실패: {}", dataType, e.getMessage(), e);
        }
    }

    // 사용자 생성 공통 메소드
    private User createUserIfNotExists(String email, String name, String nickname, String phone, Role role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    try {
                        User user = User.builder()
                                .email(email)
                                .password(passwordEncoder.encode("1234"))
                                .name(name)
                                .nickname(nickname)
                                .phone(phone)
                                .authProvider(AuthProvider.LOCAL)
                                .role(role)
                                .status(UserStatus.ACTIVE)
                                .build();
                        User savedUser = userRepository.save(user);
                        log.info("사용자 생성 완료: {} ({})", savedUser.getEmail(), savedUser.getRole());
                        return savedUser;
                    } catch (Exception e) {
                        log.error("사용자 생성 실패: {} - {}", email, e.getMessage());
                        throw e;
                    }
                });
    }

    private void createDummyUsers() {
        log.info("기본 더미 사용자들을 생성합니다...");

        List<UserData> userData = Arrays.asList(
                new UserData("user1@test.com", "유저1이름", "유저닉네임1", "010-1234-1234", Role.USER),
                new UserData("user2@test.com", "유저2이름", "유저닉네임2", "010-5678-5678", Role.USER),
                new UserData("admin@test.com", "관리자1", "관리자1닉네임", "010-9999-9999", Role.ADMIN),
                new UserData("host1@test.com", "호스트1", "host1", "010-1111-1111", Role.HOST),
                new UserData("provider1@test.com", "공간제공자1", "provider1", "010-2222-2222", Role.PROVIDER)
        );

        userData.forEach(data -> {
            try {
                createUserIfNotExists(data.email, data.name, data.nickname, data.phone, data.role);
            } catch (Exception e) {
                log.error("사용자 생성 실패: {}", data.email, e);
            }
        });

        long userCount = userRepository.count();
        log.info("총 {}명의 사용자가 데이터베이스에 존재합니다.", userCount);
    }

    private void createDummyVenues() {
        List<VenueData> venueData = Arrays.asList(
                new VenueData("테스트 팝업 장소1", "서울특별시 강남구 테헤란로 123", "서울특별시 강남구 역삼동 123-45", "1층 101호", 37.5012, 127.0396, true),
                new VenueData("테스트 팝업 장소2", "서울특별시 마포구 홍익로 234", "서울특별시 마포구 홍익동 234-56", "2층 전체", 37.5512, 126.9222, false),
                new VenueData("테스트 팝업 장소3", "서울특별시 용산구 이태원로 345", "서울특별시 용산구 이태원동 345-67", "지하 1층", 37.5345, 126.9947, true)
        );

        List<Venue> venues = venueData.stream()
                .map(data -> Venue.of(data.name, data.roadAddress, data.lotAddress, data.detail, data.latitude, data.longitude, data.hasParking))
                .collect(Collectors.toList());

        venueRepository.saveAll(venues);
        log.info("{}개의 장소 데이터 생성 완료", venues.size());
    }

    private void createDummyMissions() {
        // venue 가져오기
        List<Venue> venues = venueRepository.findAll();
        if (venues.isEmpty()) {
            createDummyVenues(); // venue가 없으면 먼저 생성
            venues = venueRepository.findAll();
        }

        // 팝업 생성
        Popup popup = Popup.createForTest("테스트 팝업", PopupStatus.ONGOING, venues.get(0));
        popupRepository.saveAndFlush(popup);

        // 미션셋 생성
        MissionSet missionSet = MissionSet.builder()
                .popupId(popup.getId())
                .requiredCount(3)
                .status(MissionSetStatus.ENABLED)
                .rewardPin("1234")
                .build();
        missionSetRepository.saveAndFlush(missionSet);

        // 미션 생성
        List<MissionData> missionData = Arrays.asList(
                new MissionData("mission1", "포토 스팟에서 사진 찍고 업로드", "photo"),
                new MissionData("mission2", "행사 부스에서 받은 리플릿 코드 입력", "CODE-2025"),
                new MissionData("mission3", "스태프가 알려준 암호 입력", "popin"),
                new MissionData("mission4", "전시 패널 A의 키워드 입력", "ART"),
                new MissionData("mission5", "퀴즈 정답 입력", "42"),
                new MissionData("mission6", "SNS 해시태그 확인 코드", "SNS")
        );

        List<Mission> missions = missionData.stream()
                .map(data -> Mission.builder()
                        .title(data.title)
                        .description(data.description)
                        .answer(data.answer)
                        .missionSet(missionSet)
                        .build())
                .collect(Collectors.toList());
        missionRepository.saveAll(missions);

        // 리워드 옵션 생성
        List<RewardData> rewardData = Arrays.asList(
                new RewardData("엽서", 200),
                new RewardData("텀블러", 100),
                new RewardData("에코백", 50),
                new RewardData("스티커팩", 200)
        );

        List<RewardOption> options = rewardData.stream()
                .map(data -> RewardOption.builder()
                        .missionSet(missionSet)
                        .name(data.name)
                        .total(data.total)
                        .build())
                .collect(Collectors.toList());
        rewardOptionRepository.saveAll(options);

        log.info("생성된 Popup ID: {}, MissionSet ID: {}", popup.getId(), missionSet.getId());
    }

    private PopupRegisterRequestDto buildPopupDto(String title) {
        List<Venue> venues = venueRepository.findAll();
        if (venues.isEmpty()) {
            createDummyVenues();
            venues = venueRepository.findAll();
        }

        PopupRegisterRequestDto dto = new PopupRegisterRequestDto();
        dto.setTitle(title);
        dto.setSummary("테스트 요약");
        dto.setDescription("테스트 설명");
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setEntryFee(1);
        dto.setReservationAvailable(true);
        dto.setWaitlistAvailable(false);
        dto.setNotice("공지 없음");
        dto.setMainImageUrl("https://dummyimage.com/600x400");
        dto.setIsFeatured(false);
        dto.setVenueId(venues.get(0).getId());
        return dto;
    }

    private void createFakeHostAndReservations() {
        // 사용자 생성
        User host1 = createUserIfNotExists("host1@test.com", "호스트1", "host1", "010-1111-1111", Role.HOST);
        User reservation1 = createUserIfNotExists("reservation1@test.com", "예약자1", "reservation1", "010-2222-2222", Role.USER);

        // 브랜드 생성
        Brand brand = brandRepository.findAll().stream()
                .filter(b -> b.getName().equals("가짜브랜드"))
                .findFirst()
                .orElseGet(() -> brandRepository.save(
                        Brand.builder()
                                .name("가짜브랜드")
                                .description("host1 전용 가짜 브랜드")
                                .businessType(Brand.BusinessType.INDIVIDUAL)
                                .build()
                ));

        // 호스트 등록
        if (!hostRepository.existsByBrandAndUser(brand, host1.getId())) {
            hostRepository.save(Host.builder()
                    .brand(brand)
                    .user(host1)
                    .roleInBrand(HostRole.OWNER)
                    .build());
        }

        // 팝업 및 예약 생성 (중복 방지)
        createPopupWithReservation(brand, "가짜 팝업1", reservation1, LocalDateTime.now().plusDays(1), false, false);
        createPopupWithReservation(brand, "가짜 팝업2", reservation1, LocalDateTime.now().minusDays(1), true, false);
        createPopupWithReservation(brand, "가짜 팝업3", reservation1, LocalDateTime.now().plusDays(2), false, true);

        log.info("더미 데이터 생성 완료: host1@test.com → 브랜드/팝업3개, reservation1@test.com → 예약 3건");
    }

    private void createPopupWithReservation(Brand brand, String title, User user, LocalDateTime reservationTime, boolean markVisited, boolean cancel) {
        if (popupRepository.findFirstByTitle(title).isEmpty()) {

            List<Venue> venues = venueRepository.findAll();
            if (venues.isEmpty()) {
                createDummyVenues(); // venue가 없으면 먼저 생성
                venues = venueRepository.findAll();
            }

            // ★ 수정: 팝업 생성 후 venue 설정
            Popup popup = Popup.create(brand.getId(), buildPopupDto(title));
            popup.setVenue(venues.get(0)); // venue 설정
            popupRepository.save(popup);

            Reservation reservation = Reservation.create(
                    popup, user, user.getName(), user.getPhone(),
                    markVisited ? 10 : (cancel ? 15 : 20), // 인원수
                    reservationTime
            );

            if (markVisited) reservation.markAsVisited();
            if (cancel) reservation.cancel();
            reservationRepository.save(reservation);

            log.info("팝업 생성 완료: {} (venue: {})", title, venues.get(0).getName());
        }
    }

    /**
     * 기존에 venue가 없는 팝업들에게 venue를 연결하는 메서드
     */
    private void fixExistingPopupsVenue() {
        List<Popup> popupsWithoutVenue = popupRepository.findAll().stream()
                .filter(popup -> popup.getVenue() == null)
                .collect(Collectors.toList());

        if (!popupsWithoutVenue.isEmpty()) {
            List<Venue> venues = venueRepository.findAll();
            if (venues.isEmpty()) {
                createDummyVenues();
                venues = venueRepository.findAll();
            }

            // 첫 번째 venue를 기본값으로 설정
            Venue defaultVenue = venues.get(0);

            for (Popup popup : popupsWithoutVenue) {
                popup.setVenue(defaultVenue);
                popupRepository.save(popup);
                log.info("팝업 {} venue 연결 완료: {}", popup.getTitle(), defaultVenue.getName());
            }

            log.info("이 {}개 팝업의 venue 연결 완료", popupsWithoutVenue.size());
        }
    }

    private void createDummyInquiries() {
        User user1 = userRepository.findByEmail("user1@test.com").orElse(null);
        User user2 = userRepository.findByEmail("user2@test.com").orElse(null);
        User reservationUser = userRepository.findByEmail("reservation1@test.com").orElse(null);

        List<Popup> popups = popupRepository.findAll();
        Long popup1Id = getIdOrDefault(popups, 0, 1L);
        Long popup2Id = getIdOrDefault(popups, 1, 1L);
        Long popup3Id = getIdOrDefault(popups, 2, 1L);

        List<InquiryData> inquiryData = Arrays.asList(
                // 팝업 신고
                new InquiryData(getEmailOrDefault(user1, "user1@test.com"), TargetType.POPUP, popup1Id, "부적절한 콘텐츠 신고", "이 팝업에서 부적절한 내용이 전시되고 있습니다.", InquiryStatus.OPEN),
                new InquiryData(getEmailOrDefault(user2, "user2@test.com"), TargetType.POPUP, popup2Id, "허위 정보 신고", "팝업 정보가 실제와 다릅니다.", InquiryStatus.IN_PROGRESS),
                new InquiryData(getEmailOrDefault(reservationUser, "reservation1@test.com"), TargetType.POPUP, popup3Id, "스팸 팝업 신고", "동일한 팝업이 여러 번 중복 등록되어 있어 스팸으로 의심됩니다.", InquiryStatus.CLOSED),

                // 공간 신고
                new InquiryData(getEmailOrDefault(user1, "user1@test.com"), TargetType.SPACE, 1L, "시설 문제 신고", "대여한 공간의 시설이 사진과 너무 다릅니다.", InquiryStatus.OPEN),
                new InquiryData(getEmailOrDefault(user2, "user2@test.com"), TargetType.SPACE, 2L, "예약 취소 문제", "예약 확정 후 일방적으로 취소당했습니다.", InquiryStatus.IN_PROGRESS),

                // 리뷰 신고
                new InquiryData(getEmailOrDefault(user1, "user1@test.com"), TargetType.REVIEW, 1L, "욕설 및 비방 리뷰", "이 리뷰에 욕설과 인신공격성 발언이 포함되어 있습니다.", InquiryStatus.OPEN),
                new InquiryData(getEmailOrDefault(reservationUser, "reservation1@test.com"), TargetType.REVIEW, 2L, "허위 리뷰 신고", "실제 방문하지 않고 작성한 것으로 보이는 허위 리뷰입니다.", InquiryStatus.IN_PROGRESS),

                // 일반 문의
                new InquiryData(getEmailOrDefault(user2, "user2@test.com"), TargetType.GENERAL, 1L, "로그인 문제 문의", "앱에서 로그인이 자꾸 풀립니다.", InquiryStatus.OPEN),
                new InquiryData("helper@test.com", TargetType.GENERAL, 2L, "즐겨찾기 기능 요청", "팝업 즐겨찾기 기능을 추가해주시면 좋겠습니다.", InquiryStatus.IN_PROGRESS),

                // 사용자 신고
                new InquiryData(getEmailOrDefault(reservationUser, "reservation1@test.com"), TargetType.USER, getUserIdOrDefault(user1, 1L), "사용자 부적절 행위 신고", "이 사용자가 팝업장에서 다른 방문객들에게 불쾌감을 주는 행위를 했습니다.", InquiryStatus.OPEN)
        );

        List<Inquiry> inquiries = inquiryData.stream()
                .map(data -> Inquiry.builder()
                        .email(data.email)
                        .targetType(data.targetType)
                        .targetId(data.targetId)
                        .subject(data.subject)
                        .content(data.content)
                        .status(data.status)
                        .build())
                .collect(Collectors.toList());

        inquiryRepository.saveAll(inquiries);
        log.info("신고 더미 데이터 생성 완료 - 이 {}건", inquiries.size());
    }

    private void createDummyRoleUpgrades() {
        List<RoleUpgradeData> roleUpgradeData = Arrays.asList(
                new RoleUpgradeData("host-applicant1@test.com", Role.HOST, "{\"companyName\":\"팝업스토어 전문업체\",\"businessNumber\":\"123-45-67890\",\"description\":\"다양한 브랜드와 협업하여 팝업스토어를 기획하고 운영하는 전문업체입니다.\"}"),
                new RoleUpgradeData("host-applicant2@test.com", Role.HOST, "{\"companyName\":\"크리에이티브 스페이스\",\"businessNumber\":\"987-65-43210\",\"description\":\"창의적인 공간 기획과 이벤트 운영을 전문으로 하는 업체입니다.\"}"),
                new RoleUpgradeData("provider-applicant1@test.com", Role.PROVIDER, "{\"companyName\":\"프리미엄 공간 대여\",\"businessNumber\":\"111-22-33444\",\"description\":\"강남, 홍대, 이태원 등 핫플레이스에 위치한 프리미엄 공간을 보유하고 있습니다.\",\"spaceCount\":15}"),
                new RoleUpgradeData("provider-applicant2@test.com", Role.PROVIDER, "{\"companyName\":\"유니크 베뉴\",\"businessNumber\":\"555-66-77888\",\"description\":\"독특하고 개성있는 소규모 공간을 제공합니다.\",\"spaceCount\":5}")
        );

        List<RoleUpgrade> roleUpgrades = roleUpgradeData.stream()
                .map(data -> RoleUpgrade.builder()
                        .email(data.email)
                        .requestedRole(data.requestedRole)
                        .payload(data.payload)
                        .build())
                .collect(Collectors.toList());

        // 상태 다양하게 설정
        if (roleUpgrades.size() > 0) roleUpgrades.get(0).approve();
        if (roleUpgrades.size() > 1) roleUpgrades.get(1).reject("제출된 서류에 누락이 있습니다.");

        roleUpgradeRepository.saveAll(roleUpgrades);
    }

    private void createDummySpaces() {
        User provider1 = createUserIfNotExists("provider1@test.com", "공간제공자1", "provider1", "010-1111-2222", Role.PROVIDER);
        User provider2 = createUserIfNotExists("provider2@test.com", "공간제공자2", "provider2", "010-2222-3333", Role.PROVIDER);

        List<Venue> venues = venueRepository.findAll();
        if (venues.size() < 5) {
            createAdditionalVenues();
            venues = venueRepository.findAll();
        }

        final List<Venue> finalVenues = venues; // effectively final 변수 생성

        List<SpaceData> spaceData = Arrays.asList(
                new SpaceData(provider1, "강남 프리미엄 스튜디오", "촬영, 전시회, 세미나에 최적화된 프리미엄 스튜디오입니다.", 120, 300000, "010-1111-2222", "https://dummyimage.com/600x400/4A90E2/ffffff?text=Premium+Studio"),
                new SpaceData(provider1, "홍대 아트스페이스", "예술 작품 전시와 창작 활동을 위한 아트스페이스입니다.", 80, 180000, "010-1111-2222", "https://dummyimage.com/600x400/E74C3C/ffffff?text=Art+Space"),
                new SpaceData(provider2, "이태원 이벤트홀", "각종 이벤트, 파티, 워크샵을 위한 다목적 이벤트홀입니다.", 200, 500000, "010-2222-3333", "https://dummyimage.com/600x400/F39C12/ffffff?text=Event+Hall")
        );

        List<Space> spaces = spaceData.stream()
                .map(data -> Space.builder()
                        .owner(data.owner)
                        .title(data.title)
                        .description(data.description)
                        .areaSize(data.areaSize)
                        .startDate(LocalDate.now().minusDays(10))
                        .endDate(LocalDate.now().plusDays(60))
                        .rentalFee(data.rentalFee)
                        .contactPhone(data.contactPhone)
                        .coverImageUrl(data.coverImageUrl)
                        .venue(finalVenues.get(0))
                        .build())
                .collect(Collectors.toList());

        spaceRepository.saveAll(spaces);
    }

    private void createAdditionalVenues() {
        List<VenueData> additionalVenues = Arrays.asList(
                new VenueData("송파 컨벤션센터", "서울특별시 송파구 올림픽로 456", "서울특별시 송파구 잠실동 456-78", "메인홀", 37.5145, 127.1026, true),
                new VenueData("성수동 카페 스페이스", "서울특별시 성동구 성수일로 567", "서울특별시 성동구 성수동2가 567-89", "1층 카페공간", 37.5445, 127.0560, false)
        );

        List<Venue> venues = additionalVenues.stream()
                .map(data -> Venue.of(data.name, data.roadAddress, data.lotAddress, data.detail, data.latitude, data.longitude, data.hasParking))
                .collect(Collectors.toList());

        venueRepository.saveAll(venues);
    }

    /**
     * 알림 설정 더미 데이터 생성
     */
    private void createDummyNotificationSettings() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (notificationSettingRepository.findByUser(user).isEmpty()) {
                NotificationSetting setting = NotificationSetting.createDefault(user);
                notificationSettingRepository.save(setting);
                log.info("사용자 {}의 알림 설정 생성 완료", user.getEmail());
            }
        }
    }

    /**
     * 알림 더미 데이터 생성
     */
    private void createDummyNotifications() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("사용자가 없어 알림 데이터를 생성할 수 없습니다.");
            return;
        }

        // 알림 데이터 정의
        List<NotificationData> notificationDataList = Arrays.asList(
                // RESERVATION 타입 알림
                new NotificationData("예약 확정 알림", "가짜 팝업1 예약이 확정되었습니다.", NotificationType.RESERVATION, "/popup/1"),
                new NotificationData("예약 취소 알림", "가짜 팝업2 예약이 취소되었습니다.", NotificationType.RESERVATION, "/popup/2"),
                new NotificationData("예약 확인 필요", "내일 방문 예정인 팝업이 있습니다. 확인해주세요.", NotificationType.RESERVATION, "/mypage/reservations"),

                // SYSTEM 타입 알림
                new NotificationData("시스템 점검 안내", "오늘 밤 12시~2시 서버 점검이 진행됩니다.", NotificationType.SYSTEM, "/notice"),
                new NotificationData("업데이트 알림", "새로운 기능이 추가되었습니다. 확인해보세요!", NotificationType.SYSTEM, "/"),
                new NotificationData("약관 변경 안내", "이용약관이 변경되었습니다.", NotificationType.SYSTEM, "/terms"),

                // EVENT 타입 알림
                new NotificationData("특별 이벤트", "신규 회원 대상 특별 할인 이벤트가 시작되었습니다!", NotificationType.EVENT, "/event/1"),
                new NotificationData("미션 완료 알림", "팝업 미션을 모두 완료하셨습니다. 리워드를 받아가세요!", NotificationType.EVENT, "/mission/reward"),
                new NotificationData("새 팝업 오픈", "관심 지역에 새로운 팝업이 오픈했습니다.", NotificationType.EVENT, "/popup/new")
        );

        // 각 사용자에게 랜덤하게 알림 생성
        for (User user : users) {
            // 사용자당 3-7개의 알림 생성
            int notificationCount = 3 + (int) (Math.random() * 5);

            for (int i = 0; i < notificationCount; i++) {
                NotificationData data = notificationDataList.get(i % notificationDataList.size());

                Notification notification = Notification.builder()
                        .user(user)
                        .title(data.title)
                        .message(data.message)
                        .type(data.type)
                        .link(data.link)
                        .build();

                // 일부 알림은 읽음 처리
                if (Math.random() < 0.3) { // 30% 확률로 읽음 처리
                    notification.markAsRead();
                }

                notificationRepository.save(notification);
            }

            log.info("사용자 {}에게 {}개의 알림 생성 완료", user.getEmail(), notificationCount);
        }
    }

    // 헬퍼 메소드들
    private Long getIdOrDefault(List<? extends Object> list, int index, Long defaultValue) {
        return list.size() > index ? ((Popup) list.get(index)).getId() : defaultValue;
    }

    private String getEmailOrDefault(User user, String defaultEmail) {
        return user != null ? user.getEmail() : defaultEmail;
    }

    private Long getUserIdOrDefault(User user, Long defaultId) {
        return user != null ? user.getId() : defaultId;
    }

    // 데이터 클래스들
    private static class UserData {
        final String email, name, nickname, phone;
        final Role role;

        UserData(String email, String name, String nickname, String phone, Role role) {
            this.email = email; this.name = name; this.nickname = nickname; this.phone = phone; this.role = role;
        }
    }

    private static class VenueData {
        final String name, roadAddress, lotAddress, detail;
        final double latitude, longitude;
        final boolean hasParking;

        VenueData(String name, String roadAddress, String lotAddress, String detail, double latitude, double longitude, boolean hasParking) {
            this.name = name; this.roadAddress = roadAddress; this.lotAddress = lotAddress; this.detail = detail;
            this.latitude = latitude; this.longitude = longitude; this.hasParking = hasParking;
        }
    }

    private static class MissionData {
        final String title, description, answer;

        MissionData(String title, String description, String answer) {
            this.title = title; this.description = description; this.answer = answer;
        }
    }

    private static class RewardData {
        final String name;
        final int total;

        RewardData(String name, int total) {
            this.name = name; this.total = total;
        }
    }

    private static class InquiryData {
        final String email, subject, content;
        final TargetType targetType;
        final Long targetId;
        final InquiryStatus status;

        InquiryData(String email, TargetType targetType, Long targetId, String subject, String content, InquiryStatus status) {
            this.email = email; this.targetType = targetType; this.targetId = targetId;
            this.subject = subject; this.content = content; this.status = status;
        }
    }

    private static class RoleUpgradeData {
        final String email, payload;
        final Role requestedRole;

        RoleUpgradeData(String email, Role requestedRole, String payload) {
            this.email = email; this.requestedRole = requestedRole; this.payload = payload;
        }
    }

    private static class SpaceData {
        final User owner;
        final String title, description, contactPhone, coverImageUrl;
        final int areaSize, rentalFee;

        SpaceData(User owner, String title, String description, int areaSize, int rentalFee, String contactPhone, String coverImageUrl) {
            this.owner = owner; this.title = title; this.description = description;
            this.areaSize = areaSize; this.rentalFee = rentalFee; this.contactPhone = contactPhone; this.coverImageUrl = coverImageUrl;
        }
    }

    private static class NotificationData {
        final String title, message, link;
        final NotificationType type;

        NotificationData(String title, String message, NotificationType type, String link) {
            this.title = title; this.message = message; this.type = type; this.link = link;
        }
    }
}