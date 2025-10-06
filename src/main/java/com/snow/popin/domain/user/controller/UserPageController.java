package com.snow.popin.domain.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserPageController {

    @GetMapping("/mypage")
    public String myPage() {
        return "forward:/templates/pages/mypage/user/user-mypage.html"; // static/users/mypage.html
    }

    @GetMapping("/user-missions")
    public String myMissions() {
        return "forward:/templates/pages/mypage/user/user-missions.html"; // static/users/user-missions.html
    }

    @GetMapping("/user-bookmarks")
    public String myBookmarks() {
        return "forward:/templates/pages/mypage/user/user-bookmarks.html";
    }

    @GetMapping("/user-reviews")
    public String myReviews() {
        return "forward:/templates/pages/mypage/user/user-reviews.html";
    }

    @GetMapping("/user-popup-reservation")
    public String myPopupReservation() {
        return "forward:/templates/pages/mypage/user/user-popup-reservation.html";
    }

    @GetMapping("/role-upgrade-request")
    public String roleUpgradeRequest() {
        return "forward:/templates/pages/role-upgrade-request.html";
    }
}
