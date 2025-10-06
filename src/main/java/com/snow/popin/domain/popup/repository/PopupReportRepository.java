package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.PopupReport;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopupReportRepository extends JpaRepository<PopupReport, Long> {
    Page<PopupReport> findAllByStatusOrderByCreatedAtDesc(PopupReport.Status status, Pageable pageable);
    Page<PopupReport> findAllByReporterOrderByCreatedAtDesc(User reporter, Pageable pageable);
}