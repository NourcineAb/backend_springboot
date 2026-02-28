package com.enterprise.poodle.controller;

import com.enterprise.poodle.dto.response.CertificateResponse;
import com.enterprise.poodle.dto.response.CertificateVerificationResponse;
import com.enterprise.poodle.security.SecurityUtils;
import com.enterprise.poodle.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final SecurityUtils securityUtils;

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates() {
        Long employeeId = securityUtils.getCurrentEmployeeId();
        return ResponseEntity.ok(certificateService.getMyCertificates(employeeId));
    }

    @GetMapping("/verify/{certificateCode}")
    public ResponseEntity<CertificateVerificationResponse> verifyCertificate(
            @PathVariable String certificateCode) {
        return ResponseEntity.ok(certificateService.verifyCertificate(certificateCode));
    }
}
