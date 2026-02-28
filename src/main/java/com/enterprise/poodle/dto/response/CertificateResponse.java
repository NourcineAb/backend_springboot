package com.enterprise.poodle.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateResponse {
    private Long id;
    private String employeeName;
    private String courseTitle;
    private String certificateCode;
    private String qrCodeUrl;
    private LocalDateTime issuedAt;
    private boolean revoked;
}
