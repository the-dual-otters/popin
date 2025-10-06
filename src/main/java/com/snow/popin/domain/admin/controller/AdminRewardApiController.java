package com.snow.popin.domain.admin.controller;

import com.snow.popin.domain.admin.service.AdminRewardService;
import com.snow.popin.domain.reward.dto.request.RewardOptionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/mission-sets/{missionSetId}/rewards")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRewardApiController {

    private final AdminRewardService adminRewardService;

    @GetMapping
    public List<RewardOptionDto> list(@PathVariable UUID missionSetId) {
        return adminRewardService.list(missionSetId);
    }

    @PostMapping
    public RewardOptionDto create(@PathVariable UUID missionSetId,
                                  @RequestBody RewardOptionDto dto) {
        return adminRewardService.create(missionSetId, dto);
    }

    @PutMapping("/{optionId}")
    public RewardOptionDto update(@PathVariable Long optionId,
                                  @RequestBody RewardOptionDto dto) {
        return adminRewardService.update(optionId, dto);
    }

    @DeleteMapping("/{optionId}")
    public void delete(@PathVariable Long optionId) {
        adminRewardService.delete(optionId);
    }
}
