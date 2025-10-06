package com.snow.popin.domain.roleupgrade.dto;

import com.snow.popin.domain.roleupgrade.entity.DocumentType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
public class DocumentUploadRequest {


    private DocumentType docType;

    private String businessNumber; // 사업자등록증인 경우에만

    private String fileUrl;

    @Builder
    public DocumentUploadRequest(DocumentType docType, String businessNumber, String fileUrl) {
        this.docType = docType;
        this.businessNumber = businessNumber;
        this.fileUrl = fileUrl;
    }

}
