package com.snow.popin.domain.popup.controller;

import com.snow.popin.domain.popup.dto.request.PopupSearchRequestDto;
import com.snow.popin.domain.popup.dto.response.AutocompleteResponseDto;
import com.snow.popin.domain.popup.dto.response.PopupListResponseDto;
import com.snow.popin.domain.popup.service.PopupSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PopupSearchController {

    private final PopupSearchService popupSearchService;

    // 팝업스토어 검색 (제목, 태그)
    @GetMapping("/popups")
    public ResponseEntity<PopupListResponseDto> searchPopups(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        PopupSearchRequestDto request = PopupSearchRequestDto.of(query, page, size);
        PopupListResponseDto response = popupSearchService.searchPopups(request);

        return ResponseEntity.ok(response);
    }

    // 자동완성 리스트 조회
    // 팝업 제목과 태그에서 검색어와 일치하는 항목들을 반환
    @GetMapping("/suggestions")
    public ResponseEntity<AutocompleteResponseDto> getAutocompleteSuggestions(
            @RequestParam(value = "q", required = false)
            @Size(min = 1, max = 100, message = "검색어는 1~100자 사이여야 합니다") String query) {

        try {
            AutocompleteResponseDto response = popupSearchService.getAutocompleteSuggestions(query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("자동완성 제안 요청 실패", e);
            return ResponseEntity.ok(AutocompleteResponseDto.empty(query));
        }
    }
}