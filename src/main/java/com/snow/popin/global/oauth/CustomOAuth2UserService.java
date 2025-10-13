package com.snow.popin.global.oauth;

import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        log.info("=== OAuth2 로그인 시작 ===");

        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);

            // 어느 플랫폼인지 확인 ID
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            log.info("Provider: {}", registrationId);
            log.info("Attributes: {}", attributes);

            // OAuthAttributes에 정보처리 요청
            OAuthAttributes extract = OAuthAttributes.of(registrationId, attributes);

            log.info("추출된 정보 - Email: {}, Name: {}, Provider: {}, ProviderId: {}",
                    extract.getEmail(), extract.getName(), extract.getProvider(), extract.getProviderId());

            User user = saveOrUpdate(extract);

            log.info("사용자 저장 완료 - User ID: {}, Email: {}", user.getId(), user.getEmail());
            log.info("=== OAuth2 로그인 성공 ===");

            return new CustomOAuth2User(
                    Collections.singleton(() -> "ROLE_USER"),
                    attributes,
                    extract.getEmail()
            );

        } catch (Exception e) {
            log.error("=== OAuth2 로그인 실패 ===", e);
            log.error("Error message: {}", e.getMessage());
            throw e;
        }
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        log.info("사용자 조회 시작 - Email: {}", attributes.getEmail());

        User user = userRepository.findByEmail(attributes.getEmail())
                .map(existing -> {
                    log.info("기존 사용자 발견 - ID: {}", existing.getId());
                    existing.updateProfile(attributes.getName(), attributes.getName(), null);
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("신규 사용자 생성");
                    return attributes.toEntity();
                });

        User savedUser = userRepository.save(user);
        log.info("사용자 저장 완료 - ID: {}", savedUser.getId());

        return savedUser;
    }
}