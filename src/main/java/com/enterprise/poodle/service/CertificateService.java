package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.response.CertificateResponse;
import com.enterprise.poodle.dto.response.CertificateVerificationResponse;
import com.enterprise.poodle.entity.Certificate;
import com.enterprise.poodle.entity.Course;
import com.enterprise.poodle.entity.Employee;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;

    @Transactional
    public void generateCertificateIfEligible(Employee employee, Course course) {
        // Check if certificate already exists
        if (certificateRepository.existsByEmployeeIdAndCourseId(
                employee.getId(), course.getId())) {
            log.info("Certificate already exists for employee {} and course {}",
                    employee.getId(), course.getId());
            return;
        }

        Certificate certificate = Certificate.builder()
                .employee(employee)
                .course(course)
                .issuedAt(LocalDateTime.now())
                .build();

        // Generate QR code URL (placeholder - in production use a QR service)
        String qrUrl = String.format("/api/certificates/verify/%s",
                certificate.getCertificateCode());
        certificate.setQrCodeUrl(qrUrl);

        certificateRepository.save(certificate);
        log.info("Certificate generated for employee {} - course {}: {}",
                employee.getId(), course.getId(), certificate.getCertificateCode());
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getMyCertificates(Long employeeId) {
        return certificateRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CertificateVerificationResponse verifyCertificate(String certificateCode) {
        Certificate certificate = certificateRepository.findByCertificateCode(certificateCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Certificate", "code", certificateCode));

        Employee employee = certificate.getEmployee();
        Course course = certificate.getCourse();

        return CertificateVerificationResponse.builder()
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .courseTitle(course.getTitle())
                .issuedAt(certificate.getIssuedAt())
                .status(certificate.isRevoked() ? "REVOKED" : "VALID")
                .build();
    }

    private CertificateResponse mapToResponse(Certificate certificate) {
        Employee employee = certificate.getEmployee();
        return CertificateResponse.builder()
                .id(certificate.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .courseTitle(certificate.getCourse().getTitle())
                .certificateCode(certificate.getCertificateCode())
                .qrCodeUrl(certificate.getQrCodeUrl())
                .issuedAt(certificate.getIssuedAt())
                .revoked(certificate.isRevoked())
                .build();
    }
}
