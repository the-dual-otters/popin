package com.snow.popin.domain.popup.entity;

import com.snow.popin.domain.mypage.host.dto.PopupHourResponseDto;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "popup_hours")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopupHours extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 0=Mon..6=Sun

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    private String note;

    public void updateTimes(LocalTime openTime, LocalTime closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public void updateNote(String note) {
        this.note = note;
    }
    public static PopupHours create(Popup popup, PopupHourResponseDto dto) {
        PopupHours hours = new PopupHours();
        hours.popup = popup;
        hours.dayOfWeek = dto.getDayOfWeek();
        hours.openTime = LocalTime.parse(dto.getOpenTime());
        hours.closeTime = LocalTime.parse(dto.getCloseTime());
        return hours;
    }
}
