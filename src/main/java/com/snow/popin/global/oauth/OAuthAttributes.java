package com.snow.popin.global.oauth;

import com.snow.popin.domain.auth.constant.AuthProvider;
import com.snow.popin.domain.user.constant.UserStatus;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.constant.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 카카오/네이버 정보를 우리의 형식으로 변환
 */
@Slf4j
@Getter
public class OAuthAttributes {

    private Map<String, Object> attributes;
    private String name;
    private String email;
    private String provider;
    private String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes,
                           String name,
                           String email,
                           String provider,
                           String providerId) {
        this.attributes = attributes;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        log.info("OAuthAttributes.of() 호출 - Provider: {}", registrationId);

        if ("kakao".equals(registrationId)) {
            return ofKakao(attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver(attributes);
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인: " + registrationId);
        }
    }

    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        log.info("카카오 사용자 정보 파싱 시작");

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String kakaoId = String.valueOf(attributes.get("id"));
        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");

        // 이메일 제공 동의 안 한 경우 임시 이메일 생성
        if (email == null || email.isEmpty()) {
            email = "kakao_" + kakaoId + "@popin.local";
            log.info("이메일이 없어서 자동 생성: {}", email);
        }

        log.info("카카오 로그인 - Email: {}, Name: {}", email, nickname);

        return OAuthAttributes.builder()
                .name(nickname)
                .email(email)
                .provider("kakao")
                .providerId(kakaoId)
                .attributes(attributes)
                .build();
    }

    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        log.info("네이버 사용자 정보 파싱 시작");
        log.info("전체 attributes: {}", attributes);

        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        log.info("response: {}", response);

        String naverId = (String) response.get("id");
        log.info("네이버 ID: {}", naverId);

        String email = (String) response.get("email");
        log.info("원본 이메일: {}", email);

        if (email == null || email.isEmpty()) {
            email = "naver_" + naverId + "@popin.local";
            log.info("이메일이 없어서 자동 생성: {}", email);
        }

        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email(email)
                .provider("naver")
                .attributes(response)
                .build();
    }

    /**
     * User 엔티티로 변환
     */
    public User toEntity() {
        log.info("User 엔티티 생성 - Email: {}, Name: {}, Provider: {}", email, name, provider);

        // 임시 비밀번호 생성
        String tempPassword = java.util.UUID.randomUUID().toString();

        return User.builder()
                .email(email)
                .password(tempPassword)
                .name(name)
                .nickname(name)
                .authProvider(AuthProvider.valueOf(provider.toUpperCase()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}