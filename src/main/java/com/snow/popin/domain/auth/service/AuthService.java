package com.snow.popin.domain.auth.service;

import com.snow.popin.domain.auth.constant.AuthProvider;
import com.snow.popin.domain.auth.dto.*;
import com.snow.popin.domain.category.entity.Category;
import com.snow.popin.domain.category.repository.CategoryRepository;
import com.snow.popin.domain.category.repository.UserInterestRepository;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.category.entity.UserInterest;
import com.snow.popin.domain.user.constant.UserStatus;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.jwt.JwtTokenResolver;
import com.snow.popin.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtTokenResolver jwtTokenResolver;
    private final UserInterestRepository userInterestRepository;

    /**
     * 회원가입 처리
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        log.info("회원가입 시도: email={}", request.getEmail());

        validateSignupRequest(request);
        checkDuplicates(request);

        User user = createUser(request);
        User savedUser = userRepository.save(user);

        processUserInterests(savedUser, request.getInterests());

        log.info("회원가입 완료: userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        return SignupResponse.success(savedUser.getEmail(), savedUser.getName(), savedUser.getNickname());
    }

    /**
     * 사용자 로그인 처리
     */
    public LoginResponse login(LoginRequest request) {
        log.info("로그인 시도: email={}", request.getEmail());

        User user = validateUserCredentials(request);
        String accessToken = generateAccessToken(user);

        log.info("로그인 성공: email={}", user.getEmail());
        return createLoginResponse(user, accessToken);
    }

    /**
     * 사용자 로그아웃 처리
     */
    public LogoutResponse logout(LogoutRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String userEmail = extractUserEmailFromToken(request, httpRequest);

        log.info("로그아웃 요청 처리: email={}", userEmail);

        processLogout(httpResponse);

        log.info("로그아웃 완료: email={}", userEmail);
        return LogoutResponse.success("로그아웃이 완료되었습니다.");
    }

    /**
     * Spring Security UserDetailsService 구현
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return createUserDetails(user);
    }

    /**
     * 이메일 중복 확인
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 닉네임 중복 확인
     */
    public boolean nicknameExists(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 회원가입 요청 검증
     */
    private void validateSignupRequest(SignupRequest request) {
        if (!request.isPasswordMatching()) {
            throw new GeneralException(ErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }
    }

    /**
     * 중복 검증
     */
    private void checkDuplicates(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new GeneralException(ErrorCode.BAD_REQUEST, "이미 사용 중인 닉네임입니다.");
        }
    }

    /**
     * 사용자 엔티티 생성
     */
    private User createUser(SignupRequest request) {
        return User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    /**
     * 사용자 관심사 처리
     */
    private void processUserInterests(User user, List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            log.debug("관심사가 선택되지 않음");
            return;
        }

        log.info("관심사 처리 시작 - 요청된 관심사: {}", interests);

        validateInterests(interests);

        Set<Category> categories = findCategoriesByNames(interests);
        log.info("찾은 카테고리 수: {}, 카테고리 이름들: {}",
                categories.size(),
                categories.stream().map(Category::getName).collect(Collectors.toList()));

        saveUserInterests(user, categories);

        log.info("사용자 관심사 설정 완료: 관심사 개수={}", categories.size());
    }

    /**
     * 관심사 검증
     */
    private void validateInterests(List<String> interests) {
        if (interests.size() > 10) {
            throw new GeneralException(ErrorCode.BAD_REQUEST, "관심사는 최대 10개까지 선택할 수 있습니다.");
        }
    }

    /**
     * 카테고리 이름으로 카테고리 조회
     */
    private Set<Category> findCategoriesByNames(List<String> interests) {
        Set<Category> categories = categoryRepository.findByNameIn(interests);

        if (categories.size() != interests.size()) {
            List<String> foundNames = categories.stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            log.warn("일부 카테고리를 찾을 수 없습니다. 요청: {}, 찾은 카테고리: {}",
                    interests, foundNames);
        }

        return categories;
    }

    /**
     * 사용자 관심사 DB에 저장
     */
    private void saveUserInterests(User user, Set<Category> categories) {
        log.debug("사용자 관심사 저장 시작 - 카테고리 수: {}", categories.size());

        List<UserInterest> userInterests = categories.stream()
                .map(category -> new UserInterest(user, category))
                .collect(Collectors.toList());

        userInterestRepository.saveAll(userInterests);

        log.debug("사용자 관심사 저장 완료: {} 개", userInterests.size());
    }

    /**
     * 사용자 자격 증명 검증
     */
    private User validateUserCredentials(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 이메일로 로그인 시도: {}", request.getEmail());
                    return new GeneralException(ErrorCode.LOGIN_FAILED);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("잘못된 비밀번호로 로그인 시도: {}", request.getEmail());
            throw new GeneralException(ErrorCode.LOGIN_FAILED);
        }

        return user;
    }

    /**
     * JWT 액세스 토큰 생성
     */
    private String generateAccessToken(User user) {
        try {
            return jwtUtil.createToken(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole().name()
            );
        } catch (Exception e) {
            log.error("JWT 토큰 생성 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
            throw new GeneralException(ErrorCode.INTERNAL_ERROR, "인증 토큰 생성에 실패했습니다.");
        }
    }

    /**
     * 로그인 응답 객체 생성
     */
    private LoginResponse createLoginResponse(User user, String accessToken) {
        return LoginResponse.of(
                accessToken,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }

    /**
     * 토큰에서 사용자 이메일 추출
     */
    private String extractUserEmailFromToken(LogoutRequest request, HttpServletRequest httpRequest) {
        try {
            String token = extractToken(request, httpRequest);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                return jwtUtil.getEmail(token);
            }
        } catch (Exception e) {
            log.debug("토큰에서 사용자 정보 추출 실패: {}", e.getMessage());
        }

        return "unknown";
    }

    /**
     * 실제 로그아웃 처리
     */
    private void processLogout(HttpServletResponse response) {
        clearAuthCookies(response);
        addCacheControlHeaders(response);
    }

    /**
     * 인증 관련 쿠키들 정리
     */
    private void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, "jwtToken", "/");
        clearCookie(response, "JSESSIONID", "/");
        clearCookie(response, "remember-me", "/");
    }

    /**
     * 개별 쿠키 정리
     */
    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 개발환경용
        response.addCookie(cookie);
        log.debug("쿠키 삭제: {}", name);
    }

    /**
     * 캐시 제어 헤더 추가
     */
    private void addCacheControlHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    /**
     * 요청에서 토큰 추출
     */
    private String extractToken(LogoutRequest request, HttpServletRequest httpRequest) {
        if (StringUtils.hasText(request.getAccessToken())) {
            return request.getAccessToken();
        }
        return jwtTokenResolver.resolve(httpRequest);
    }


    /**
     * Spring Security UserDetails 객체 생성
     */
    private UserDetails createUserDetails(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

}