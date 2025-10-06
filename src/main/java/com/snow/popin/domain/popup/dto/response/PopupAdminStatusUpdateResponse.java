package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PopupAdminStatusUpdateResponse {

    private final Long id;
    private final String title;
    private final PopupStatus status;
    private final LocalDateTime updatedAt;
    private final String message;

    public static PopupAdminStatusUpdateResponse of(Popup popup) {
        return PopupAdminStatusUpdateResponse.builder()
                .id(popup.getId())
                .title(popup.getTitle())
                .status(popup.getStatus())
                .updatedAt(popup.getUpdatedAt())
                .message("팝업 상태가 성공적으로 변경되었습니다.")
                .build();
    }

}
