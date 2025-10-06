package com.snow.popin.domain.popup.service;

import com.snow.popin.domain.popup.dto.request.PopupSearchRequestDto;
import com.snow.popin.domain.popup.dto.response.*;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupSearchQueryDslRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupSearchService {

    private final PopupSearchQueryDslRepository popupSearchQueryDslRepository;

    /**
     * 팝업 검색 (제목 + 태그)
     */
    public PopupListResponseDto searchPopups(PopupSearchRequestDto request) {
        String query = preprocessQuery(request.getQuery());

        // 2글자 미만이면 빈 결과 반환
        if (query == null || query.length() < 2) {
            log.info("검색어 길이 부족 - query: '{}', length: {}",
                    request.getQuery(), query != null ? query.length() : 0);
            return createEmptyResult(request);
        }

        log.info("팝업 검색 - query: '{}', page: {}, size: {}",
                query, request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Popup> popupPage = popupSearchQueryDslRepository.searchByTitleAndTags(query, pageable);

        List<PopupSummaryResponseDto> popupDtos = popupPage.getContent()
                .stream()
                .map(PopupSummaryResponseDto::from)
                .collect(Collectors.toList());

        log.info("팝업 검색 완료 - 결과: {}개", popupPage.getTotalElements());
        return PopupListResponseDto.of(popupPage, popupDtos);
    }

    /**
     * 자동완성 제안 조회
     */
    public AutocompleteResponseDto getAutocompleteSuggestions(String query) {
        String processedQuery = preprocessQuery(query);

        // 1글자 미만이면 빈 결과 반환
        if (processedQuery == null || processedQuery.length() < 1) {
            return AutocompleteResponseDto.empty(query);
        }

        log.info("자동완성 조회 - query: '{}'", processedQuery);

        try {
            List<String> suggestions = popupSearchQueryDslRepository.findSuggestions(processedQuery, 8);

            List<AutocompleteSuggestionDto> suggestionDtos = suggestions.stream()
                    .map(suggestion -> AutocompleteSuggestionDto.of(suggestion, "suggestion", 0L))
                    .collect(Collectors.toList());

            log.info("자동완성 완료 - 결과: {}개", suggestionDtos.size());
            return AutocompleteResponseDto.of(suggestionDtos, query);

        } catch (Exception e) {
            log.error("자동완성 조회 실패 - query: '{}'", query, e);
            return AutocompleteResponseDto.empty(query);
        }
    }

    /**
     * 검색어 전처리
     */
    private String preprocessQuery(String query) {
        if (query == null) {
            return null;
        }
        return query.trim().replaceAll("\\s+", " ");
    }

    /**
     * 빈 검색 결과 생성
     */
    private PopupListResponseDto createEmptyResult(PopupSearchRequestDto request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return PopupListResponseDto.of(Page.empty(pageable), List.of());
    }
}