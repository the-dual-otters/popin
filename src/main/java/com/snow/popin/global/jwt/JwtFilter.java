package com.snow.popin.global.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtTokenResolver jwtTokenResolver;

    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        final String uri = req.getRequestURI();

        try {
            final String token = jwtTokenResolver.resolve(req);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                final String email = jwtUtil.getEmail(token);

                if (StringUtils.hasText(email)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 필요할 때만 가져온다 (여기가 포인트)
                    UserDetailsService uds = userDetailsServiceProvider.getObject();
                    UserDetails userDetails = uds.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("✅ JWT 인증 설정 완료: {}", email);
                }
            } else {
                log.debug("JWT 없음/무효 → 계속 진행 (공개 경로일 수 있음): {}", uri);
            }
        } catch (Exception e) {
            log.warn("JWT 필터 처리 중 예외 ({}): {}", uri, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(req, res);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        String method = req.getMethod();

        // 정적 리소스
        if (path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/static/")
                || path.startsWith("/uploads/") || path.equals("/favicon.ico")
                || path.startsWith("/templates/")) {
            return true;
        }

        // 공개 페이지
        if (path.equals("/") || path.equals("/index.html")
                || path.equals("/main") || path.equals("/error")) {
            return true;
        }

        // 공개 컨텐츠 페이지
        if (path.startsWith("/popup/") || path.startsWith("/map")
                || path.startsWith("/space/") || path.startsWith("/reviews/")) {
            return true;
        }

        // 인증 페이지
        if (path.startsWith("/auth/")) {
            return true;
        }

        // 소셜 로그인
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            log.debug("✅ OAuth2 로그인 경로 - 필터 제외");
            return true;
        }

        // 공개 API - GET 요청만!
        if ("GET".equals(method)) {
            if (path.startsWith("/api/popups")
                    || path.startsWith("/api/spaces")
                    || path.startsWith("/api/reviews")
                    || path.startsWith("/api/venues")) {
                return true;
            }
            // 필요하면 "정확한 공개 엔드포인트"만 예외 추가:
            // if (path.equals("/api/categories")) return true;
        }

        // 인증 API
        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/signup")
                || path.equals("/api/auth/check-email")
                || path.equals("/api/auth/check-nickname")) {
            return true;
        }

        // 그 외는 필터 적용
        return false;
    }
}
