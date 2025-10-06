package com.snow.popin.domain.mypage.provider.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage/provider")
public class ProviderPageController {

    @GetMapping
    public String index() {
        return "forward:/templates/pages/mypage/provider/mpg-provider.html";
    }
}
