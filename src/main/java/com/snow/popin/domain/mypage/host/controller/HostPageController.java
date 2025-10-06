package com.snow.popin.domain.mypage.host.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
public class HostPageController {

    @GetMapping("/mypage/host")
    public String hostMypage() {
        return "forward:/templates/pages/mypage/host/mpg-host.html";
    }

    @GetMapping("/mypage/host/popup/{id}")
    public String hostPopupDetail(@PathVariable Long id) {
        return "forward:/templates/pages/mypage/host/host-popup-detail.html";
    }

    @GetMapping("/mypage/host/popup/register")
    public String popupRegister() {
        return "forward:/templates/pages/mypage/host/popup-register.html";
    }

    @GetMapping("/mypage/host/popup/{id}/edit")
    public String popupEdit(@PathVariable Long id) {
        return "forward:/templates/pages/mypage/host/popup-edit.html";
    }

    @GetMapping("/mypage/host/popup/{id}/reservation")
    public String reservationManage(@PathVariable Long id) {
        return "forward:/templates/pages/mypage/host/reservation-manage.html";
    }

    @GetMapping("/mypage/host/popup/{id}/stats")
    public String popupStatsPage(@PathVariable Long id) {
        return "forward:/templates/pages/mypage/host/popup-stats.html";
    }
}
