package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.response.CourseProgressResponse;
import com.enterprise.poodle.entity.*;
import com.enterprise.poodle.enums.CourseStatus;
import com.enterprise.poodle.enums.ProgressStatus;
import com.enterprise.poodle.exception.BusinessException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.*;
import com.enterprise.poodle.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final EmployeeCourseProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final EmployeeCourseGradeRepository gradeRepository;
    private final CourseDepartmentAssignmentRepository assignmentRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId) {
        Employee employee = securityUtils.getCurrentEmployee();
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessException("Cannot access draft or archived courses");
        }

        List<CourseSection> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<EmployeeCourseProgress> progressList =
                progressRepository.findByEmployeeIdAndCourseId(employee.getId(), courseId);

        Map<Long, EmployeeCourseProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        p -> p.getSection().getId(), p -> p));

        long completedCount = progressList.stream()
                .filter(p -> p.getStatus() == ProgressStatus.COMPLETED)
                .count();

        List<CourseProgressResponse.SectionProgressResponse> sectionProgress = sections.stream()
                .map(section -> {
                    EmployeeCourseProgress p = progressMap.get(section.getId());
                    return CourseProgressResponse.SectionProgressResponse.builder()
                            .sectionId(section.getId())
                            .sectionTitle(section.getTitle())
                            .status(p != null ? p.getStatus() : ProgressStatus.NOT_STARTED)
                            .completedAt(p != null ? p.getCompletedAt() : null)
                            .build();
                })
                .collect(Collectors.toList());

        // Get grade if exists
        Integer finalScore = null;
        Boolean passed = null;
        var grade = gradeRepository.findByEmployeeIdAndCourseId(employee.getId(), courseId);
        if (grade.isPresent()) {
            finalScore = grade.get().getFinalScore();
            passed = grade.get().isPassed();
        }

        double completionPercentage = sections.isEmpty() ? 0 :
                (double) completedCount / sections.size() * 100;

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .totalSections(sections.size())
                .completedSections((int) completedCount)
                .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                .finalScore(finalScore)
                .passed(passed)
                .sectionProgress(sectionProgress)
                .build();
    }
}
