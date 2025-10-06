package com.snow.popin.domain.inquiry.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryCountResponse {
    private long total;
    private long popup;
    private long review;
    private long user;
    private long space;
    private long general;
    private long open;
    private long inProgress;
    private long closed;
    private long popupPending;
    private long spacePending;
    private long reviewPending;
    private long generalPending;
    private long userPending;

    @Builder
    public InquiryCountResponse(long total, long popup, long review, long user, long space,
                                long general, long open, long inProgress, long closed,
                                long popupPending, long spacePending, long reviewPending,
                                long generalPending, long userPending) {
        this.total = total;
        this.popup = popup;
        this.review = review;
        this.user = user;
        this.space = space;
        this.general = general;
        this.open = open;
        this.inProgress = inProgress;
        this.closed = closed;
        this.popupPending = popupPending;
        this.spacePending = spacePending;
        this.reviewPending = reviewPending;
        this.generalPending = generalPending;
        this.userPending = userPending;
    }
}