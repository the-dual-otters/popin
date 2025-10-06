package com.snow.popin.domain.popup.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.snow.popin.domain.category.entity.QCategory.category;
import static com.snow.popin.domain.map.entity.QVenue.venue;
import static com.snow.popin.domain.popup.entity.QPopup.popup;
import static com.snow.popin.domain.popup.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class PopupSearchQueryDslRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    /**
     * 팝업 제목과 태그로 검색
     */
    public Page<Popup> searchByTitleAndTags(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String lowerQuery = query.toLowerCase().trim();

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                popup.title.lower().contains(lowerQuery)
                        .or(tag.name.lower().contains(lowerQuery))
        );

        List<Popup> content = queryFactory
                .selectDistinct(popup)
                .from(popup)
                .leftJoin(popup.tags, tag)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(
                        popup.status.when(PopupStatus.ONGOING).then(1)
                                .when(PopupStatus.PLANNED).then(2)
                                .when(PopupStatus.ENDED).then(3)
                                .otherwise(4).asc(),
                        popup.createdAt.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.countDistinct())
                .from(popup)
                .leftJoin(popup.tags, tag)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 자동완성 검색어 조회
     */
    public List<String> findSuggestions(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        String lowerQuery = query.toLowerCase().trim();
        List<String> suggestions = new ArrayList<>();

        // 제목 검색
        List<String> startsWithTitles = queryFactory
                .select(popup.title)
                .from(popup)
                .where(popup.title.lower().startsWith(lowerQuery))
                .distinct()
                .limit(limit / 2)
                .fetch();
        suggestions.addAll(startsWithTitles);

        List<String> containsTitles = queryFactory
                .select(popup.title)
                .from(popup)
                .where(popup.title.lower().contains(lowerQuery)
                        .and(popup.title.lower().startsWith(lowerQuery).not()))
                .distinct()
                .limit(limit / 2)
                .fetch();
        suggestions.addAll(containsTitles);

        // 태그 검색
        suggestions.addAll(queryFactory
                .select(tag.name)
                .from(popup)
                .join(popup.tags, tag)
                .where(tag.name.lower().contains(lowerQuery))
                .distinct()
                .limit(limit / 2)
                .fetch());

        return suggestions.stream()
                .distinct()
                .sorted((a, b) -> {
                    int lengthCompare = Integer.compare(a.length(), b.length());
                    return lengthCompare != 0 ? lengthCompare : a.compareTo(b);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}