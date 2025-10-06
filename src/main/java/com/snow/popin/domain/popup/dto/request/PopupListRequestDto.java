package com.snow.popin.domain.popup.dto.request;

import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;

@Getter
public class PopupListRequestDto {

    private PopupStatus status;
    private String region;
    private String dateFilter;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String sortBy = "latest";

    private List<Long> categoryIds;

    @Min(0)
    private int page = 0;

    @Min(1)
    private int size = 20;

    public void setStatus(PopupStatus status) {
        this.status = status;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setDateFilter(String dateFilter) {
        this.dateFilter = dateFilter;
        setDateRangeByFilter(dateFilter);
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isDeadlineSoon() {
        return "deadline".equals(sortBy);
    }

    public boolean hasRegionFilter() {
        return region != null && !"전체".equals(region) && !region.trim().isEmpty();
    }

    public boolean hasDateFilter() {
        return startDate != null || endDate != null;
    }

    public boolean hasCategoryFilter() {
        return categoryIds != null && !categoryIds.isEmpty();
    }

    private void setDateRangeByFilter(String dateFilter) {
        if (dateFilter != null && !"custom".equals(dateFilter)) {
            LocalDate now = LocalDate.now();
            switch (dateFilter) {
                case "today":
                    this.startDate = now;
                    this.endDate = now;
                    break;
                case "week":
                    this.startDate = now;
                    this.endDate = now.plusDays(7);
                    break;
                case "two_weeks":
                    this.startDate = now;
                    this.endDate = now.plusDays(14);
                    break;
                default:
                    this.startDate = null;
                    this.endDate = null;
            }
        }
    }
}