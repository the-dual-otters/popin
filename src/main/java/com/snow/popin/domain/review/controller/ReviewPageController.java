package com.snow.popin.domain.review.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/reviews")
public class ReviewPageController {

    // 팝업 리뷰 전체 목록 페이지 (더보기)
    @GetMapping("/popup/{popupId}")
    public String reviewList(@PathVariable Long popupId, Model model) {
        log.info("팝업 리뷰 목록 페이지 요청 - 팝업ID: {}", popupId);
        model.addAttribute("popupId", popupId);
        return "forward:/templates/pages/popup/review-list.html";
    }

    // 리뷰 작성 폼 페이지
    @GetMapping("/popup/{popupId}/create")
    public String reviewCreateForm(@PathVariable Long popupId, Model model) {
        log.info("리뷰 작성 폼 페이지 요청 - 팝업ID: {}", popupId);
        model.addAttribute("popupId", popupId);
        return "forward:/templates/pages/popup/review-create.html";
    }

    // TODO: 마이페이지에서 구현 예정
    // 리뷰 수정 폼 페이지
    @GetMapping("/{reviewId}/edit")
    public String reviewEditForm(@PathVariable Long reviewId, Model model) {
        log.info("리뷰 수정 폼 페이지 요청 - 리뷰ID: {}", reviewId);
        model.addAttribute("reviewId", reviewId);
        return "forward:/templates/pages/review/review-edit.html";
    }

    // 내 리뷰 목록 페이지 (마이페이지)
    @GetMapping("/me")
    public String myReviews() {
        log.info("내 리뷰 목록 페이지 요청");
        return "forward:/templates/pages/mypage/user/user-reviews.html";
    }
}