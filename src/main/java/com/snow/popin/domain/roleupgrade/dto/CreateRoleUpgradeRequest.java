package com.snow.popin.domain.roleupgrade.dto;

import com.snow.popin.domain.user.constant.Role;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateRoleUpgradeRequest {

    @NotNull(message = "요청 역할은 필수입니다")
    private Role requestedRole;

    // ERD의 payload JSON 컬럼에 맞게 String으로 받기
    private String payload;

    @Builder
    public CreateRoleUpgradeRequest(Role requestedRole, String payload) {
        this.requestedRole = requestedRole;
        this.payload = payload;
    }
}
