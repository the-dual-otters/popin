package com.snow.popin.global.config;

import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.jwt.JwtFilter;
import com.snow.popin.global.oauth.CustomOAuth2UserService;
import com.snow.popin.global.oauth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;

import static com.snow.popin.global.error.ErrorResponseUtil.sendErrorResponse;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 정적 리소스 완전 제외
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .antMatchers("/css/**", "/js/**", "/images/**", "/static/**",
                        "/favicon.ico", "/templates/**", "/uploads/**", "/error/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                .authorizeRequests(authz -> authz
                        // 정적 리소스
                        .antMatchers("/uploads/**", "/css/**", "/js/**", "/images/**",
                                "/static/**", "/favicon.ico", "/templates/**", "/*.json",
                                "/pages/**", "/error/**").permitAll()

                        // === 공개 API (GET만) ===
                        .antMatchers(HttpMethod.GET, "/api/popups/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/spaces/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/venues/**").permitAll()

                        .antMatchers(HttpMethod.GET, "/api/categories").permitAll()


                        // 공개 페이지 - 로그인 없이 접근 가능
                        .antMatchers("/", "/index.html", "/main",
                                "/popup/**", "/map", "/space/**", "/reviews/**").permitAll()

                        // 인증 관련 API
                        .antMatchers("/auth/**", "/api/auth/**").permitAll()
                        //  OAuth2 로그인 관련 경로 추가
                        .antMatchers("/oauth2/**", "/login/**", "/auth/success").permitAll()

                        // === 로그인이 필요한 페이지 ===
                        .antMatchers("/mypage/**", "/bookmarks/**", "/chat/**").authenticated()

                        // === 로그인이 필요한 API (POST/PUT/DELETE) ===
                        .antMatchers(HttpMethod.POST, "/api/reviews/**").authenticated()
                        .antMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                        .antMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                        .antMatchers(HttpMethod.POST, "/api/popups/**").authenticated()
                        .antMatchers(HttpMethod.PUT, "/api/popups/**").authenticated()
                        .antMatchers(HttpMethod.DELETE, "/api/popups/**").authenticated()
                        .antMatchers("/api/bookmarks/**").authenticated()
                        .antMatchers("/api/reservations/**").authenticated()
                        .antMatchers("/api/host/**").authenticated()

                        // 관리자 페이지
                        .antMatchers("/admins/**", "/api/admin/**").hasRole("ADMIN")

                        // === 나머지는 모두 허용 ===
                        .anyRequest().permitAll()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint()
                        .userService(customOAuth2UserService)
                        .and()
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                // JWT 필터 등록
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // 예외 처리
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((req, res, authException) -> {
                            if (isApiRequest(req)) {
                                sendErrorResponse(res, ErrorCode.UNAUTHORIZED);
                            } else {
                                String redirectUrl = "/auth/login?redirect=" +
                                        java.net.URLEncoder.encode(req.getRequestURI(), "UTF-8");
                                res.sendRedirect(redirectUrl);
                            }
                        })
                        .accessDeniedHandler((req, res, accessDeniedException) -> {
                            if (isApiRequest(req)) {
                                sendErrorResponse(res, ErrorCode.ACCESS_DENIED);
                            } else {
                                res.sendRedirect("/error?code=403");
                            }
                        })
                )

                .headers(headers -> headers
                        .frameOptions().deny()
                        .contentTypeOptions().and()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isApiRequest(HttpServletRequest req) {
        String reqWith = req.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(reqWith) || req.getRequestURI().startsWith("/api/");
    }
}
