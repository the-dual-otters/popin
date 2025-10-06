package com.snow.popin.domain.roleupgrade.dto;

import com.snow.popin.domain.roleupgrade.entity.DocumentType;
import lombok.*;

@Getter
@NoArgsConstructor
public class DocumentResponse {

    private Long id;
    private DocumentType docType;
    private String businessNumber;
    private String fileUrl;

    @Builder
    public DocumentResponse(Long id, DocumentType docType, String businessNumber,
                            String fileUrl) {
        this.id = id;
        this.docType = docType;
        this.businessNumber = businessNumber;
        this.fileUrl = fileUrl;
    }
}
