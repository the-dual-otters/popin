package com.snow.popin.domain.map.repository;

import java.util.List;

public interface MapRepositoryCustom {

    /** 진행중/예정 팝업이 존재하는 지역 목록 (중복제거 + 정렬) */
    List<String> findDistinctRegionsWithActivePopups();

    /** 좌표가 존재하는 장소들의 지역 목록 (중복제거 + 정렬) */
    List<String> findDistinctRegionsWithCoordinates();
}
