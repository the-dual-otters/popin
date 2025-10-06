package com.snow.popin.domain.popup.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopupStatsResponse {

    private Long total;        // 전체 팝업 수
    private Long planning;      // 계획 중인 팝업 수
    private Long ongoing;       // 진행중인 팝업 수
    private Long completed;    // 완료된 팝업 수
   // private Long cancelled;    // 취소된 팝업 수

}
