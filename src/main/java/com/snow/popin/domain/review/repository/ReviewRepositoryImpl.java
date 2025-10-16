package com.snow.popin.domain.review.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.review.entity.QReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QReview r = QReview.review;

    @Override
    public Object[] findRatingStatsByPopupId(Long popupId) {
        var tuple = queryFactory
                .select(r.rating.avg(), r.id.count())
                .from(r)
                .where(
                        r.popupId.eq(popupId),
                        r.isBlocked.isFalse()
                )
                .fetchOne();

        Double avg = (tuple == null) ? null : tuple.get(r.rating.avg());
        Long count = (tuple == null) ? 0L   : tuple.get(r.id.count());
        return new Object[]{ avg, count };
    }
}
