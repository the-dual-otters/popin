package com.snow.popin.domain.review.repository;

public interface ReviewRepositoryCustom {
    Object[] findRatingStatsByPopupId(Long popupId);
}
