package com.snow.popin.domain.bookmark.service;

import com.snow.popin.domain.bookmark.dto.BookMarkListResponseDto;
import com.snow.popin.domain.bookmark.dto.BookMarkResponseDto;
import com.snow.popin.domain.bookmark.entity.BookMark;
import com.snow.popin.domain.bookmark.repository.BookMarkQueryDslRepository;
import com.snow.popin.domain.bookmark.repository.BookMarkRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.global.exception.PopupNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookMarkService {

    private final BookMarkRepository bookMarkRepository;
    private final BookMarkQueryDslRepository bookMarkQueryDslRepository;
    private final PopupRepository popupRepository;

    // 북마크 추가
    @Transactional
    public BookMarkResponseDto addBookmark(Long userId, Long popupId) {
        log.info("북마크 추가 시작 - userId: {}, popupId: {}", userId, popupId);

        // 팝업 존재 여부 확인
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new PopupNotFoundException(popupId));

        // 중복 체크를 제거하고 DB 제약조건에 의존 (동시성 이슈 방지)
        try {
            BookMark bookmark = BookMark.ofWithPopup(userId, popup);
            BookMark savedBookmark = bookMarkRepository.save(bookmark);

            log.info("북마크 추가 완료 - userId: {}, popupId: {}", userId, popupId);
            return BookMarkResponseDto.from(savedBookmark);

        } catch (DataIntegrityViolationException e) {
            log.warn("동시성 충돌로 중복 북마크 발생 - userId: {}, popupId: {}", userId, popupId);
            throw new IllegalArgumentException("이미 북마크한 팝업입니다.", e);
        } catch (Exception e) {
            log.error("북마크 추가 중 예상치 못한 오류 발생 - userId: {}, popupId: {}", userId, popupId, e);
            throw e;
        }
    }

    //  북마크 삭제
    @Transactional
    public void removeBookmark(Long userId, Long popupId) {
        log.info("북마크 삭제 시작 - userId: {}, popupId: {}", userId, popupId);

        if (!bookMarkRepository.existsByUserIdAndPopupId(userId, popupId)) {
            log.warn("북마크가 존재하지 않습니다 - userId: {}, popupId: {}", userId, popupId);
            throw new IllegalArgumentException("북마크가 존재하지 않습니다.");
        }

        bookMarkRepository.deleteByUserIdAndPopupId(userId, popupId);
        log.info("북마크 삭제 완료 - userId: {}, popupId: {}", userId, popupId);
    }

    // 북마크 토글 (있으면 삭제, 없으면 추가)
    @Transactional
    public BookMarkResponseDto toggleBookmark(Long userId, Long popupId) {
        log.info("북마크 토글 시작 - userId: {}, popupId: {}", userId, popupId);

        try {
            if (bookMarkRepository.existsByUserIdAndPopupId(userId, popupId)) {
                removeBookmark(userId, popupId);
                return null;
            } else {
                return addBookmark(userId, popupId);
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("토글 중 동시성 충돌 발생 - userId: {}, popupId: {}", userId, popupId);
            return null;
        }
    }

    // 사용자 북마크 목록 조회 (페이징)
    public BookMarkListResponseDto getUserBookmarks(Long userId, int page, int size) {
        log.info("사용자 북마크 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);

        Pageable pageable = createPageable(page, size);
        Page<BookMark> bookmarkPage = bookMarkQueryDslRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<BookMarkResponseDto> bookmarkDtos = bookmarkPage.getContent()
                .stream()
                .map(BookMarkResponseDto::from)
                .collect(Collectors.toList());

        log.info("사용자 북마크 목록 조회 완료 - userId: {}, 총 {}개", userId, bookmarkPage.getTotalElements());
        return BookMarkListResponseDto.of(bookmarkPage, bookmarkDtos);
    }

    // 북마크 여부 확인
    public boolean isBookmarked(Long userId, Long popupId) {
        return bookMarkRepository.existsByUserIdAndPopupId(userId, popupId);
    }

    // 사용자별 북마크 수 조회
    public long getUserBookmarkCount(Long userId) {
        return bookMarkRepository.countByUserId(userId);
    }

    // 팝업별 북마크 수 조회
    public long getPopupBookmarkCount(Long popupId) {
        return bookMarkRepository.countByPopupId(popupId);
    }

    // 사용자가 북마크한 팝업 ID 목록 조회
    public List<Long> getUserBookmarkedPopupIds(Long userId) {
        return bookMarkQueryDslRepository.findPopupIdsByUserId(userId);
    }

    private Pageable createPageable(int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 100);
        return PageRequest.of(validPage, validSize);
    }
}
