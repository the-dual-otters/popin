package com.snow.popin.domain.auth.controller;

import com.snow.popin.domain.auth.dto.LogoutRequest;
import com.snow.popin.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthViewController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "forward:/templates/auth/login.html";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "forward:/templates/auth/signup.html";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest req, HttpServletResponse res) {

        try{
            // 서버 측 로그아웃 처리
            LogoutRequest logoutReq = new LogoutRequest();
            authService.logout(logoutReq, req, res);
            log.info("서버 측 로그아웃 처리 완료");
        } catch (Exception e){
            log.warn("서버 측 로그아웃 처리 중 오류 (계속 진행) : {}", e.getMessage());
        }

        return "forward:/templates/auth/logout.html";

    }
}