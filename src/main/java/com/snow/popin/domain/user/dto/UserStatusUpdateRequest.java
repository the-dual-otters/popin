package com.snow.popin.domain.user.dto;

import com.snow.popin.domain.user.constant.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class UserStatusUpdateRequest {

    @NotNull(message = "사용자 상태는 필수입니다.")
    private UserStatus status;

    public UserStatusUpdateRequest(UserStatus status) {
        this.status = status;
    }
}
