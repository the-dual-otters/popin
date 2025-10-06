package com.snow.popin.domain.user.dto;

import com.snow.popin.domain.user.constant.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserStatusUpdateResponse {

    private Long userId;
    private String userName;
    private UserStatus status;
    private String statusDescription;
    private String message;

    @Builder
    public UserStatusUpdateResponse(Long userId, String userName, UserStatus status,
                                    String statusDescription, String message) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.statusDescription = statusDescription;
        this.message = message;
    }

    public static UserStatusUpdateResponse of(Long userId, String userName, UserStatus status) {
        return UserStatusUpdateResponse.builder()
                .userId(userId)
                .userName(userName)
                .status(status)
                .statusDescription(status.getDescription())
                .message("사용자 상태가 " + status.getDescription() + "으로 변경되었습니다.")
                .build();
    }
}