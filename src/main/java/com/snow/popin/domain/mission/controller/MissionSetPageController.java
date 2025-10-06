package com.snow.popin.domain.mission.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/missions")
public class MissionSetPageController {

    @GetMapping("/{missionSetId}")
    public String missionPage(@PathVariable String missionSetId) {
        return "forward:/templates/pages/mission/mission.html";
    }
}
