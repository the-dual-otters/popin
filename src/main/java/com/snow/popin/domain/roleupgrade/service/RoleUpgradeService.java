package com.snow.popin.domain.roleupgrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.roleupgrade.dto.CreateRoleUpgradeRequest;
import com.snow.popin.domain.roleupgrade.dto.RoleUpgradeResponse;
import com.snow.popin.domain.roleupgrade.entity.ApprovalStatus;
import com.snow.popin.domain.roleupgrade.entity.DocumentType;
import com.snow.popin.domain.roleupgrade.entity.RoleUpgrade;
import com.snow.popin.domain.roleupgrade.entity.RoleUpgradeDocument;
import com.snow.popin.domain.roleupgrade.repository.RoleUpgradeRepository;
import com.snow.popin.domain.space.service.FileStorageService;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RoleUpgradeService {

    private final RoleUpgradeRepository roleRepo;
    private final ObjectMapper objMapper;
    private final FileStorageService fileStorageService;

    // 역할 승격 요청 생성 + file
    @Transactional
    public Long createRoleUpgradeRequest(String email, CreateRoleUpgradeRequest req, List<MultipartFile> files) {
        validateNoDuplicateRequest(email);

        try {
            String payloadJson = objMapper.writeValueAsString(req.getPayload());

            RoleUpgrade roleUpgrade = RoleUpgrade.builder()
                    .email(email)
                    .requestedRole(req.getRequestedRole())
                    .payload(payloadJson)
                    .build();

            RoleUpgrade saved = roleRepo.save(roleUpgrade);

            // 파일이 있는 경우에만 문서 처리
            if (files != null && !files.isEmpty()){
                processUploadedFiles(saved, files, req.getRequestedRole());
            }

            log.info("역할 승격 요청 생성 완료. ID: {}, Email: {}, Role: {}, 첨부파일 수: {}",
                    saved.getId(), email, req.getRequestedRole(), files != null ? files.size() : 0);

            return saved.getId();
        } catch (GeneralException e) {
            // GeneralException은 그대로 다시 던져서 GlobalExceptionHandler에서 처리
            throw e;
        } catch (Exception e) {
            log.error("역할 승격 요청 생성 실패: {}", e.getMessage(), e);
            throw new GeneralException(ErrorCode.INTERNAL_ERROR, "역할 승격 요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 파일 처리 로직 분리
     */
    private void processUploadedFiles(RoleUpgrade roleUpgrade, List<MultipartFile> files, Role requestedRole) {
        DocumentType documentType = determineDocTypeByRole(requestedRole);

        for (MultipartFile file : files){
            if (!file.isEmpty()){
                try {
                    // 파일 유효성 검사
                    validateUploadFile(file);

                    String fileUrl = fileStorageService.saveDocument(file);

                    RoleUpgradeDocument document = RoleUpgradeDocument.builder()
                            .roleUpgrade(roleUpgrade)
                            .docType(documentType)
                            .fileUrl(fileUrl)
                            .build();

                    roleUpgrade.addDocument(document);

                    log.info("문서 첨부 완료. RequestId: {}, FileName: {}, FileUrl: {}",
                            roleUpgrade.getId(),
                            sanitizeFileName(file.getOriginalFilename()),
                            fileUrl);
                } catch (IllegalArgumentException e) {
                    // FileStorageService에서 발생하는 validation 예외를 GeneralException으로 변환
                    throw new GeneralException(ErrorCode.VALIDATION_ERROR, e.getMessage());
                } catch (Exception e) {
                    log.error("파일 처리 중 오류 발생: {}", e.getMessage(), e);
                    throw new GeneralException(ErrorCode.FILE_UPLOAD_ERROR, "파일 업로드 중 오류가 발생했습니다.");
                }
            }
        }
    }

    /**
     * 파일명 안전하게 처리
     */
    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null) {
            return "unknown_file";
        }

        try {
            // UTF-8로 안전하게 인코딩
            byte[] bytes = originalFilename.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to sanitize filename: {}", originalFilename);
            return "file_" + System.currentTimeMillis();
        }
    }

    /**
     * 역할에 따른 DocumentType 결정
     */
    private DocumentType determineDocTypeByRole(Role requestedRole) {
        switch (requestedRole) {
            case HOST:
                return DocumentType.BUSINESS_LICENSE; // 사업자등록증
            case PROVIDER:
                return DocumentType.REAL_ESTATE;      // 부동산 관련 서류
            default:
                return DocumentType.ETC;              // 기타
        }
    }

    /**
     * 내 역할 승격 요청 목록 조회
     */
    public List<RoleUpgradeResponse> getMyRoleUpgradeRequests(String email){
        List<RoleUpgrade> reqs = roleRepo.findByEmailOrderByCreatedAtDesc(email);

        return reqs.stream()
                .map(RoleUpgradeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 이미 대기중인 요청이 있는지 확인
     */
    private void validateNoDuplicateRequest(String email){
        if (roleRepo.existsByEmailAndStatus(email, ApprovalStatus.PENDING)){
            throw new GeneralException(ErrorCode.DUPLICATE_ROLE_UPGRADE_REQUEST);
        }
    }

    /**
     * 대기중인 요청에만 문서 첨부 가능
     */
    public static void validatePendingStatus(RoleUpgrade roleUpgrade) {
        if (!roleUpgrade.isPending()) {
            throw new GeneralException(ErrorCode.INVALID_REQUEST_STATUS);
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateUploadFile(MultipartFile file){
        // 파일 크기 검사 (10MB 제한)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new GeneralException(ErrorCode.VALIDATION_ERROR, "파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 파일 타입 검사
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedFileType(contentType)) {
            throw new GeneralException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 파일 형식입니다. (JPG, PNG, PDF만 가능)");
        }

        // 파일명 검사
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new GeneralException(ErrorCode.VALIDATION_ERROR, "파일명이 올바르지 않습니다.");
        }
    }

    /**
     * 허용된 파일 타입 검사
     */
    private boolean isAllowedFileType(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("application/pdf");
    }
}