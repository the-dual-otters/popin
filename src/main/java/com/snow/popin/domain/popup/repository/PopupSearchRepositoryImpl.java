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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.snow.popin.domain.category.entity.QCategory.category;
import static com.snow.popin.domain.map.entity.QVenue.venue;
import static com.snow.popin.domain.popup.entity.QPopup.popup;
import static com.snow.popin.domain.popup.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class PopupSearchRepositoryImpl implements PopupSearchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Popup> searchByTitleAndTags(String q, Pageable pageable) {
        if (!StringUtils.hasText(q)) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        String lower = q.toLowerCase().trim();

        BooleanBuilder where = new BooleanBuilder()
                .and(popup.title.lower().contains(lower)
                        .or(tag.name.lower().contains(lower)));

        // content
        List<Popup> content = queryFactory
                .selectDistinct(popup)
                .from(popup)
                .leftJoin(popup.tags, tag)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(where)
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

        // total (distinct id 기준)
        Long total = queryFactory
                .select(popup.id.countDistinct())
                .from(popup)
                .leftJoin(popup.tags, tag)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<String> findSuggestions(String q, int limit) {
        if (!StringUtils.hasText(q)) return List.of();
        String lower = q.toLowerCase().trim();
        List<String> out = new ArrayList<>();

        // 제목: startsWith 우선
        out.addAll(queryFactory
                .select(popup.title)
                .from(popup)
                .where(popup.title.lower().startsWith(lower))
                .distinct()
                .limit(Math.max(1, limit / 2))
                .fetch());

        // 제목: contains(단, startsWith는 제외)
        out.addAll(queryFactory
                .select(popup.title)
                .from(popup)
                .where(popup.title.lower().contains(lower)
                        .and(popup.title.lower().startsWith(lower).not()))
                .distinct()
                .limit(Math.max(1, limit / 2))
                .fetch());

        // 태그
        out.addAll(queryFactory
                .select(tag.name)
                .from(popup)
                .join(popup.tags, tag)
                .where(tag.name.lower().contains(lower))
                .distinct()
                .limit(Math.max(1, limit / 2))
                .fetch());

        // 중복 제거 + 길이 우선 정렬
        return out.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted((a, b) -> {
                    int len = Integer.compare(a.length(), b.length());
                    return len != 0 ? len : a.compareTo(b);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}
