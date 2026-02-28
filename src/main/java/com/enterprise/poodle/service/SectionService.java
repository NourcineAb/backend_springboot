package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.request.SectionRequest;
import com.enterprise.poodle.dto.response.SectionResponse;
import com.enterprise.poodle.entity.Course;
import com.enterprise.poodle.entity.CourseSection;
import com.enterprise.poodle.enums.ActionType;
import com.enterprise.poodle.enums.ContentType;
import com.enterprise.poodle.enums.QcmType;
import com.enterprise.poodle.exception.BusinessException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.CourseRepository;
import com.enterprise.poodle.repository.CourseSectionRepository;
import com.enterprise.poodle.repository.QCMQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final CourseSectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final QCMQuestionRepository questionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SectionResponse createSection(Long courseId, SectionRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Validate FINAL QCM uniqueness
        if (request.getContentType() == ContentType.QCM
                && request.getQcmType() == QcmType.FINAL
                && sectionRepository.existsByCourseIdAndQcmType(courseId, QcmType.FINAL)) {
            throw new BusinessException("Course already has a FINAL QCM section");
        }

        CourseSection section = CourseSection.builder()
                .course(course)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .contentUrl(request.getContentUrl())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .qcmType(request.getContentType() == ContentType.QCM ? request.getQcmType() : null)
                .maxAttempts(request.getMaxAttempts())
                .llmDraftEnabled(request.getLlmDraftEnabled() != null && request.getLlmDraftEnabled())
                .build();

        CourseSection saved = sectionRepository.save(section);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByCourse(Long courseId) {
        courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        return sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest request) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        Map<String, Object> oldValues = Map.of(
                "title", section.getTitle(),
                "contentType", section.getContentType());

        // Validate FINAL QCM uniqueness when changing type
        if (request.getContentType() == ContentType.QCM
                && request.getQcmType() == QcmType.FINAL
                && section.getQcmType() != QcmType.FINAL) {
            if (sectionRepository.existsByCourseIdAndQcmType(
                    section.getCourse().getId(), QcmType.FINAL)) {
                throw new BusinessException("Course already has a FINAL QCM section");
            }
        }

        if (request.getTitle() != null) section.setTitle(request.getTitle());
        if (request.getContentType() != null) section.setContentType(request.getContentType());
        if (request.getContentUrl() != null) section.setContentUrl(request.getContentUrl());
        if (request.getOrderIndex() != null) section.setOrderIndex(request.getOrderIndex());
        if (request.getContentType() == ContentType.QCM) {
            section.setQcmType(request.getQcmType());
        }
        if (request.getMaxAttempts() != null) section.setMaxAttempts(request.getMaxAttempts());
        if (request.getLlmDraftEnabled() != null) section.setLlmDraftEnabled(request.getLlmDraftEnabled());

        CourseSection saved = sectionRepository.save(section);

        auditLogService.logAction(ActionType.UPDATE, "Section", saved.getId(),
                oldValues, Map.of("title", saved.getTitle(), "contentType", saved.getContentType()));

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));
        sectionRepository.delete(section);
    }

    private SectionResponse mapToResponse(CourseSection section) {
        return SectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse().getId())
                .title(section.getTitle())
                .contentType(section.getContentType())
                .contentUrl(section.getContentUrl())
                .orderIndex(section.getOrderIndex())
                .qcmType(section.getQcmType())
                .maxAttempts(section.getMaxAttempts())
                .llmDraftEnabled(section.isLlmDraftEnabled())
                .questionCount((int) questionRepository.countBySectionId(section.getId()))
                .build();
    }
}
