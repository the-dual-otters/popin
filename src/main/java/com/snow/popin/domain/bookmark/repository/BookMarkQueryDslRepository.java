package com.snow.popin.domain.bookmark.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.bookmark.entity.BookMark;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.snow.popin.domain.bookmark.entity.QBookMark.bookMark;
import static com.snow.popin.domain.popup.entity.QPopup.popup;
import static com.snow.popin.domain.map.entity.QVenue.venue;
import static com.snow.popin.domain.category.entity.QCategory.category;

@Repository
@RequiredArgsConstructor
public class BookMarkQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 사용자별 북마크 목록 조회 (페이징)
     */
    public Page<BookMark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        List<BookMark> content = queryFactory
                .selectFrom(bookMark)
                .leftJoin(bookMark.popup, popup).fetchJoin()
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(bookMark.userId.eq(userId))
                .orderBy(bookMark.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(bookMark.count())
                .from(bookMark)
                .where(bookMark.userId.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 사용자의 북마크한 팝업 ID 목록 조회
     */
    public List<Long> findPopupIdsByUserId(Long userId) {
        return queryFactory
                .select(bookMark.popupId)
                .from(bookMark)
                .where(bookMark.userId.eq(userId))
                .fetch();
    }
}