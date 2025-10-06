package com.snow.popin.global.util;

import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.jwt.JwtUtil;
import com.snow.popin.global.jwt.JwtTokenResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserUtil {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final JwtTokenResolver jwtTokenResolver;

    // 현재 로그인한 사용자의 ID를 반환
    public Long getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser.getId();
    }

    // 현재 로그인한 사용자의 이메일을 반환
    public String getCurrentUserEmail() {
        Authentication authentication = getCurrentAuthentication();
        return authentication.getName(); // JWT에서 subject가 email
    }

    // 현재 로그인한 사용자의 Entity를 반환
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("인증된 사용자를 DB에서 찾을 수 없음: {}", email);
                    return new GeneralException(ErrorCode.USER_NOT_FOUND);
                });
    }

    // 현재 로그인한 사용자의 이름을 반환
    public String getCurrentUserName() {
        User currentUser = getCurrentUser();
        return currentUser.getName();
    }

    // 현재 로그인한 사용자의 역할을 반환
    public String getCurrentUserRole() {
        User currentUser = getCurrentUser();
        return currentUser.getRole().name();
    }

    // 현재 사용자가 특정 역할을 가지고 있는지 확인
    public boolean hasRole(String role) {
        try {
            String currentRole = getCurrentUserRole();
            return role.equalsIgnoreCase(currentRole);
        } catch (Exception e) {
            return false;
        }
    }

    // 현재 사용자가 로그인되어 있는지 확인
    public boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null &&
                    authentication.isAuthenticated() &&
                    !(authentication.getPrincipal() instanceof String); // "anonymousUser" 제외
        } catch (Exception e) {
            log.debug("인증 상태 확인 중 오류: {}", e.getMessage());
            return false;
        }
    }

    // 현재 사용자가 리소스의 소유자인지 확인
    public boolean isOwner(Long resourceOwnerId) {
        try {
            Long currentUserId = getCurrentUserId();
            return currentUserId.equals(resourceOwnerId);
        } catch (Exception e) {
            return false;
        }
    }

    // HttpServletRequest에서 JWT 토큰을 추출하여 사용자 ID 반환
    // Spring Security Context를 거치지 않고 직접 토큰에서 추출)
    public Optional<Long> getUserIdFromToken(HttpServletRequest request) {
        try {
            String token = jwtTokenResolver.resolve(request);
            if (token != null && jwtUtil.validateToken(token)) {
                return Optional.ofNullable(jwtUtil.getUserId(token));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.debug("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // HttpServletRequest에서 JWT 토큰을 추출하여 사용자 이메일 반환
    public Optional<String> getUserEmailFromToken(HttpServletRequest request) {
        try {
            String token = jwtTokenResolver.resolve(request);
            if (token != null && jwtUtil.validateToken(token)) {
                return Optional.ofNullable(jwtUtil.getEmail(token));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.debug("토큰에서 사용자 이메일 추출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // 관리자 권한 확인
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    // 공간 제공자 권한 확인

    public boolean isProvider() {
        return hasRole("PROVIDER");
    }

    // 팝업 주최자 권한 확인
    public boolean isHost() {
        return hasRole("HOST");
    }

    // 일반 사용자 권한 확인
    public boolean isUser() {
        return hasRole("USER");
    }

    // 현재 사용자의 간단한 정보를 Map으로 반환 (API 응답용)
    public java.util.Map<String, Object> getCurrentUserInfo() {
        User user = getCurrentUser();
        java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("name", user.getName());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("role", user.getRole().name());
        userInfo.put("authProvider", user.getAuthProvider().name());
        return userInfo;
    }

    // 현재 인증 객체를 반환
    private Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 정보 접근 시도");
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        // "anonymousUser" 체크
        if (authentication.getPrincipal() instanceof String) {
            log.warn("익명 사용자의 정보 접근 시도");
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        return authentication;
    }
}