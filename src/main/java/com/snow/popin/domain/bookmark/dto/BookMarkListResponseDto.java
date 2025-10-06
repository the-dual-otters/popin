package com.snow.popin.domain.bookmark.dto;

import com.snow.popin.domain.bookmark.entity.BookMark;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class BookMarkListResponseDto {
    private final List<BookMarkResponseDto> bookmarks;
    private final int totalPages;
    private final long totalElements;
    private final int currentPage;
    private final int size;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public static BookMarkListResponseDto of(Page<BookMark> page, List<BookMarkResponseDto> content) {
        return BookMarkListResponseDto.builder()
                .bookmarks(content)
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}