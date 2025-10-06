package com.snow.popin.domain.admin.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.snow.popin.global.exception.QrCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    @Value("${uploadPath}")
    private String uploadPath;

    @Value("${app.host-url}")
    private String hostUrl; // application.properties 에서 읽어옴

    public String generateMissionSetQr(UUID missionSetId) {
        log.info("[QR] generateMissionSetQr 시작 - missionSetId={}", missionSetId);
        log.info("[QR] hostUrl={}, uploadPath={}", hostUrl, uploadPath);

        try {
            // QR 코드에 담을 URL
            String qrContent = hostUrl + "/missions/" + missionSetId;
            log.info("[QR] QR content={}", qrContent);

            // 저장할 디렉토리 (/uploads/QrCodes)
            Path qrDir = Paths.get(uploadPath, "QrCodes");
            if (!Files.exists(qrDir)) {
                Files.createDirectories(qrDir);
                log.info("[QR] Created directory: {}", qrDir.toAbsolutePath());
            }

            // 파일명
            String filename = missionSetId.toString() + ".png";
            Path qrPath = qrDir.resolve(filename);
            log.info("[QR] QR path={}", qrPath);

            // QR 코드 생성
            int size = 300;
            BitMatrix matrix = new MultiFormatWriter().encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            MatrixToImageWriter.writeToPath(matrix, "PNG", qrPath);
            log.info("[QR] QR code 생성 완료: {}", qrPath);

            // 반환 URL (WebMvcConfig 매핑 기준)
            String returnUrl = "/uploads/QrCodes/" + filename;
            log.info("[QR] returnUrl={}", returnUrl);
            return returnUrl;

        } catch (Exception e) {
            log.error("[QR] QR 코드 생성 실패", e);
            throw new QrCodeException("QR 코드 생성 실패: " + e.getMessage(), e);
        }
    }
}
