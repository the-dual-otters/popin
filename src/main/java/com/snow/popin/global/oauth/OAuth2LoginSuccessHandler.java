package com.snow.popin.global.oauth;

import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        log.info("=== OAuth2 로그인 성공 핸들러 시작 ===");

        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            log.info("Principal 타입: {}", oAuth2User.getClass().getName());

            String email = oAuth2User.getEmail();
            log.info("인증된 사용자 이메일: {}", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("사용자를 찾을 수 없음 - Email: {}", email);
                        return new IllegalStateException("OAuth2 사용자 정보가 없습니다.");
                    });

            log.info("사용자 조회 성공 - ID: {}, Email: {}, Role: {}", user.getId(), user.getEmail(), user.getRole());

            String token = jwtUtil.createToken(email, user.getRole());
            log.info("JWT 토큰 생성 완료 - 길이: {}", token.length());

            String redirectUrl = "/auth/success?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            log.info("리다이렉트 URL: {}", redirectUrl);

            log.info("=== OAuth2 로그인 성공 핸들러 완료 ===");

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("=== OAuth2 로그인 성공 핸들러 실패 ===", e);
            log.error("Error Type: {}", e.getClass().getName());
            log.error("Error Message: {}", e.getMessage());
            throw e;
        }
    }
}