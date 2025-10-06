package com.snow.popin.domain.space.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/space")
public class SpacePageController {

    @GetMapping("/list")
    public String list() {
        return "forward:/templates/pages/space/space-list.html";
    }

    @GetMapping("/register")
    public String register() {
        return "forward:/templates/pages/space/space-register.html";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id) {
        return "forward:/templates/pages/space/space-detail.html";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id) {
        return "forward:/templates/pages/space/space-edit.html";
    }
}
