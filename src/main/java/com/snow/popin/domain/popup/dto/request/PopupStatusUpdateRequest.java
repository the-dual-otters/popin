package com.snow.popin.domain.popup.dto.request;

import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class PopupStatusUpdateRequest {

    @NotNull(message = "팝업 상태는 필수입니다.")
    private PopupStatus status;

    public PopupStatusUpdateRequest(PopupStatus status){
        this.status = status;
    }

}