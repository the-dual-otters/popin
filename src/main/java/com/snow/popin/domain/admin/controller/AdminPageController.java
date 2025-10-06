package com.snow.popin.domain.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admins")
public class AdminPageController {

    @GetMapping("/space-management")
    public String spaceManagement() {
        return "forward:/templates/admin/space-management.html";
    }

    @GetMapping("/admin-inquiry-main")
    public String adminInquiryMain() {
        return "forward:/templates/admin/admin-inquiry-main.html";
    }

    @GetMapping("/popup-management")
    public String popupManagement() {
        return "forward:/templates/admin/popup-management.html";
    }

    @GetMapping("/mission-management")
    public String missionManagement() {
        return "forward:/templates/admin/mission-management.html";
    }

    @GetMapping("/user-management")
    public String userManagement() {
        return "forward:/templates/admin/user-management.html";
    }

    @GetMapping("/popup-report")
    public String popupReport() {
        return "forward:/templates/admin/popup-report.html";
    }

    @GetMapping("/space-report")
    public String spaceReport() {
        return "forward:/templates/admin/space-report.html";
    }

    @GetMapping("/review-report")
    public String reviewReport() {
        return "forward:/templates/admin/review-report.html";
    }

    @GetMapping("/general-report")
    public String generalReport() {
        return "forward:/templates/admin/general-report.html";
    }

    @GetMapping("/admin-role-upgrade")
    public String adminRoleUpgrade() {
        return "forward:/templates/admin/role-upgrade/admin-role-upgrade.html";
    }
}
