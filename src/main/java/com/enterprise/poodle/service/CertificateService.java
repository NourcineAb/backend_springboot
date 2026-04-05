package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.response.CertificateResponse;
import com.enterprise.poodle.dto.response.CertificateVerificationResponse;
import com.enterprise.poodle.entity.Certificate;
import com.enterprise.poodle.entity.Course;
import com.enterprise.poodle.entity.Employee;
import com.enterprise.poodle.exception.BusinessException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.mapper.CertificateMapper;
import com.enterprise.poodle.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;

        /**
         * Generate a professional-looking certificate PDF.
         * Returns the PDF content as bytes.
         */
        public byte[] generatePdf(Long certificateId) {
                Certificate certificate = certificateRepository.findById(certificateId)
                                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

                try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
                        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
                        doc.addPage(page);

                        try (org.apache.pdfbox.pdmodel.PDPageContentStream contents = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                                float pageWidth = page.getMediaBox().getWidth();
                                float pageHeight = page.getMediaBox().getHeight();

                                // Background border
                                contents.setStrokingColor(0.2f, 0.4f, 0.8f); // Blue border
                                contents.setLineWidth(3);
                                contents.addRect(20, 20, pageWidth - 40, pageHeight - 40);
                                contents.stroke();

                                // Title
                                contents.beginText();
                                contents.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 28);
                                contents.setNonStrokingColor(0.2f, 0.4f, 0.8f);
                                float titleWidth = 18 * "Certificate of Completion".length() / 2;
                                float titleX = (pageWidth - titleWidth) / 2;
                                contents.newLineAtOffset(titleX, pageHeight - 100);
                                contents.showText("Certificate of Completion");

                                // Decorative line
                                contents.endText();
                                contents.setStrokingColor(0.2f, 0.4f, 0.8f);
                                contents.setLineWidth(1);
                                contents.moveTo(80, pageHeight - 130);
                                contents.lineTo(pageWidth - 80, pageHeight - 130);
                                contents.stroke();

                                // Load custom font
                                InputStream fontStream = getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf");
                                PDType0Font roboto = PDType0Font.load(doc, fontStream, true);

                                // Body text
                                contents.beginText();
                                contents.setFont(roboto, 12);
                                contents.setNonStrokingColor(0, 0, 0);
                                contents.newLineAtOffset(50, pageHeight - 180);
                                contents.showText("This certifies that");

                                // Employee name (highlighted)
                                contents.endText();
                                contents.beginText();
                                contents.setFont(roboto, 18);
                                contents.setNonStrokingColor(0.2f, 0.4f, 0.8f);
                                String employeeName = certificate.getEmployee().getFirstName() + " " + 
                                                    certificate.getEmployee().getLastName();
                                float nameWidth = 12 * employeeName.length() / 2;
                                float nameX = (pageWidth - nameWidth) / 2;
                                contents.newLineAtOffset(nameX, pageHeight - 220);
                                contents.showText(employeeName);

                                // Course info
                                contents.endText();
                                contents.beginText();
                                contents.setFont(roboto, 11);
                                contents.setNonStrokingColor(0, 0, 0);
                                contents.newLineAtOffset(50, pageHeight - 260);
                                contents.showText("has successfully completed the course");

                                // Course title (highlighted)
                                contents.endText();
                                contents.beginText();
                                contents.setFont(roboto, 14);
                                contents.setNonStrokingColor(0.2f, 0.4f, 0.8f);
                                String courseTitle = certificate.getCourse().getTitle();
                                contents.newLineAtOffset(50, pageHeight - 285);
                                contents.showText(courseTitle);

                                // Issue date
                                contents.endText();
                                contents.beginText();
                                contents.setFont(roboto, 10);
                                contents.setNonStrokingColor(0.4f, 0.4f, 0.4f);
                                contents.newLineAtOffset(50, pageHeight - 320);
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy");
                                String issuedDate = sdf.format(java.sql.Timestamp.valueOf(certificate.getIssuedAt()));
                                contents.showText("Issued on: " + issuedDate);

                                // Verification code
                                contents.endText();
                                contents.beginText();
                                contents.setFont(roboto, 9);
                                contents.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                                contents.newLineAtOffset(50, pageHeight - 380);
                                contents.showText("Verification Code: " + certificate.getCertificateCode());

                                // Footer message
                                contents.endText();
                                contents.beginText();
                                contents.setFont(roboto, 9);
                                contents.setNonStrokingColor(0.4f, 0.4f, 0.4f);
                                contents.newLineAtOffset(50, 50);
                                contents.showText("Scan the QR code on your certificate card to verify this credential.");
                                contents.endText();
                        }

                        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                        doc.save(out);
                        return out.toByteArray();
                } catch (java.io.IOException e) {
                        throw new RuntimeException("Failed to generate PDF", e);
                }
        }

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

        
        int attempts = 0;
        final int maxAttempts = 3;
        while (true) {
            try {
                
                String qrUrl = String.format("/api/certificates/verify/%s",
                        certificate.getCertificateCode());
                certificate.setQrCodeUrl(qrUrl);

                // save and flush to detect unique constraint violations immediately
                certificateRepository.saveAndFlush(certificate);
                log.info("Certificate generated for employee {} - course {}: {}",
                        employee.getId(), course.getId(), certificate.getCertificateCode());
                break;
            } catch (DataIntegrityViolationException ex) {
                attempts++;
                log.warn("Failed to save certificate due to data integrity violation (attempt {}): {}", attempts, ex.getMessage());
                if (attempts >= maxAttempts) {
                    throw ex; // rethrow after retries exhausted
                }
                // regenerate a new code and retry
                certificate.setCertificateCode(UUID.randomUUID().toString());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<CertificateResponse> getMyCertificates(Long employeeId, Pageable pageable) {
        return certificateRepository.findByEmployeeId(employeeId, pageable)
                .map(this::mapToResponse);
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

    @Transactional
    public CertificateResponse revokeCertificate(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", id));

        if (certificate.isRevoked()) {
            throw new BusinessException("Certificate is already revoked");
        }

        certificate.setRevoked(true);
        certificateRepository.save(certificate);
        log.info("Certificate {} revoked", certificate.getCertificateCode());
        return mapToResponse(certificate);
    }

    private CertificateResponse mapToResponse(Certificate certificate) {
        return certificateMapper.toResponse(certificate);
    }
}
