package com.snow.popin.domain.space.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${uploadPath}")
    private String uploadPath;

    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES =
            java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final java.util.Set<String> ALLOWED_EXT =
            java.util.Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    //파일 저장
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.debug("No file to upload, skipping save.");
            return null;
        }

        try {
            // uploadPath를 Path 객체로 변환
            Path root = Paths.get(uploadPath);

            // 업로드 디렉토리 없으면 생성
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("Created upload directory: {}", root.toAbsolutePath());
            }

            // 확장자 추출
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> name.contains("."))
                    .map(name -> name.substring(name.lastIndexOf(".")))
                    .orElse("");
            String lowerExt = ext.toLowerCase();
            if (!ALLOWED_EXT.contains(lowerExt)) {
                throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
            }
            String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("허용되지 않은 콘텐츠 타입입니다.");
            }

            // UUID + 확장자로 파일명 생성
            String filename = UUID.randomUUID() + ext;
            Path target = root.resolve(filename).normalize().toAbsolutePath();

            // 파일 복사 (기존 파일 있으면 교체)
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved: {}", target);

            // WebMvcConfig의 /uploads/** 매핑에 맞춰서 반환
            return "/uploads/" + filename;

        } catch (IOException e) {
            log.error("File save failed", e);
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    /**
     * 문서 파일 저장 메서드 (역할 승격용)
     * RoleUpgradeService에서 검증 완료된 파일을 저장
     */
    public String saveDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.debug("No file to upload, skipping save.");
            return null;
        }

        try {
            // documents 하위 디렉토리 생성
            Path root = Paths.get(uploadPath, "documents");

            // 업로드 디렉토리 없으면 생성
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("Created upload directory: {}", root.toAbsolutePath());
            }

            // 고유한 파일명 생성 (타임스탬프 + UUID + 원본확장자)
            String filename = generateUniqueFileName(file.getOriginalFilename());
            Path target = root.resolve(filename).normalize().toAbsolutePath();

            // 파일 복사 (기존 파일 있으면 교체)
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Document file saved: {}", target);

            // 웹 접근 가능한 경로 반환
            return "/uploads/documents/" + filename;

        } catch (IOException e) {
            log.error("Document file save failed", e);
            throw new RuntimeException("문서 파일 저장 실패", e);
        }
    }

    /**
     * 고유한 파일명 생성 (문서 파일용)
     */
    private String generateUniqueFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);

        return String.format("%s_%s%s", timestamp, uuid, extension);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}