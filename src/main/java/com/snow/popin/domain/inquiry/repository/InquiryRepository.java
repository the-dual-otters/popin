package com.snow.popin.domain.inquiry.repository;

import com.snow.popin.domain.inquiry.entity.Inquiry;
import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface  InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 대상 유형별 신고 조회
    Page<Inquiry> findByTargetTypeOrderByCreatedAtDesc(TargetType targetType, Pageable pageable);

    // 상태별 신고 조회
    Page<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status, Pageable pageable);

    // 대상 유형과 상태별 신고 조회
    Page<Inquiry> findByTargetTypeAndStatusOrderByCreatedAtDesc(
            TargetType targetType, InquiryStatus status, Pageable pageable);

    // 특정 대상에 대한 신고 조회
    List<Inquiry> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            TargetType targetType, Long targetId);

    // 대상 유형별 신고 개수 조회
    long countByTargetType(TargetType targetType);

    // 상태별 신고 개수 조회
    long countByStatus(InquiryStatus status);

    // 대상 유형과 상태별 신고 개수 조회
    long countByTargetTypeAndStatus(TargetType targetType, InquiryStatus status);

    // 이메일로 신고 조회
    Page<Inquiry> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    // 최근 신고 조회
    @Query("SELECT i FROM Inquiry i ORDER BY i.createdAt DESC")
    List<Inquiry> findRecentInquiries(Pageable pageable);

    // 처리 대기 중인 신고 조회
    @Query("SELECT i FROM Inquiry i WHERE i.status = 'OPEN' ORDER BY i.createdAt ASC")
    List<Inquiry> findPendingInquiries(Pageable pageable);

}
