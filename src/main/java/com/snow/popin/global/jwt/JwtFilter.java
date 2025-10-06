package com.snow.popin.global.jwt;

import com.snow.popin.global.constant.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.snow.popin.global.error.ErrorResponseUtil.sendErrorResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtTokenResolver jwtTokenResolver;
    private final ApplicationContext applicationContext;

    private UserDetailsService getUserDetailsService() {
        return applicationContext.getBean(UserDetailsService.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = req.getRequestURI();
        log.debug("🔍 JWT 필터 처리 시작 : {}", requestURI);

        try {
            String token = jwtTokenResolver.resolve(req);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {

                log.debug("✅ 토큰 유효함");

                String email = jwtUtil.getEmail(token);

                if (StringUtils.hasText(email)) {
                    log.debug("토큰에서 이메일 추출 : {}", email);

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        try {
                            UserDetails userDetails = getUserDetailsService().loadUserByUsername(email);
                            log.debug("사용자 정보 로드 완료 : {}", userDetails.getUsername());

                            UsernamePasswordAuthenticationToken authenticationToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    );

                            authenticationToken.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(req)
                            );

                            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                            log.debug("✅ 사용자 인증 설정 완료 : {}", email);

                        } catch (Exception e) {
                            log.warn("⚠️ 사용자 정보 로드 실패 (공개 페이지면 무시) : {}", e.getMessage());
                            SecurityContextHolder.clearContext();
                        }
                    }
                }
            } else {
                log.debug("토큰 없음 또는 유효하지 않음 (공개 페이지는 계속 진행)");
            }
        } catch (Exception e) {
            log.error("❌ 필터에서 예외 발생: {} - {}", requestURI, e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // 예외 발생 여부와 관계없이 필터 체인은 정확히 한 번만 실행
        filterChain.doFilter(req, res);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        String method = req.getMethod();

        log.debug("🔍 필터 제외 경로 확인 : {} [{}]", path, method);

        // 정적 리소스
        if (path.startsWith("/css/") || path.startsWith("/js/") ||
                path.startsWith("/images/") || path.startsWith("/static/") ||
                path.startsWith("/uploads/") ||
                path.equals("/favicon.ico") || path.startsWith("/templates/")) {
            log.debug("✅ 정적 리소스 - 필터 제외");
            return true;
        }

        // 공개 페이지
        if (path.equals("/") || path.equals("/index.html") ||
                path.equals("/main") || path.equals("/error")) {
            log.debug("✅ 공개 페이지 - 필터 제외");
            return true;
        }

        // 팝업 관련 페이지
        if (path.startsWith("/popup/") || path.startsWith("/map") ||
                path.startsWith("/space/") || path.startsWith("/reviews/")) {
            log.debug("✅ 공개 콘텐츠 페이지 - 필터 제외");
            return true;
        }

        // 인증 페이지
        if (path.startsWith("/auth/")) {
            log.debug("✅ 인증 페이지 - 필터 제외");
            return true;
        }

        // 공개 API - GET 요청만!
        if ("GET".equals(method)) {
            if (path.startsWith("/api/popups") ||
                    path.startsWith("/api/spaces") ||
                    path.startsWith("/api/reviews") ||
                    path.startsWith("/api/venues") ||
                    path.startsWith("/api/categories")) {
                log.debug("✅ 공개 API (GET) - 필터 제외");
                return true;
            }
        }

        // 인증 API (모든 메서드)
        if (path.equals("/api/auth/login") ||
                path.equals("/api/auth/signup") ||
                path.equals("/api/auth/check-email") ||
                path.equals("/api/auth/check-nickname")) {
            log.debug("✅ 인증 API - 필터 제외");
            return true;
        }

        log.debug("❌ 보호된 경로 - 필터 적용");
        return false;
    }
}