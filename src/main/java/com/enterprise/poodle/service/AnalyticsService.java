package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.response.AnalyticsResponse;
import com.enterprise.poodle.entity.*;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CourseRepository courseRepository;
    private final CourseDepartmentAssignmentRepository assignmentRepository;
    private final EmployeeCourseGradeRepository gradeRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCourseProgressRepository progressRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeQCMAttemptRepository attemptRepository;
    private final QCMQuestionRepository questionRepository;
    private final CourseSectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse.CompletionRate getCourseCompletionRate(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Count total assigned employees
        List<CourseDepartmentAssignment> assignments = assignmentRepository.findByCourseId(courseId);
        long totalAssigned = 0;
        for (CourseDepartmentAssignment assignment : assignments) {
            totalAssigned += employeeRepository.findByDepartmentId(
                    assignment.getDepartment().getId()).size();
        }

        // Count employees who completed all sections
        long totalSections = sectionRepository.countByCourseId(courseId);
        long totalCompleted = 0;

        if (totalSections > 0 && totalAssigned > 0) {
            // Get all employees from assigned departments
            List<Long> deptIds = assignments.stream()
                    .map(a -> a.getDepartment().getId())
                    .collect(Collectors.toList());
            List<Employee> employees = employeeRepository.findByDepartmentIdIn(deptIds);

            for (Employee emp : employees) {
                long completed = progressRepository.countByEmployeeIdAndCourseIdAndStatus(
                        emp.getId(), courseId,
                        com.enterprise.poodle.enums.ProgressStatus.COMPLETED);
                if (completed >= totalSections) {
                    totalCompleted++;
                }
            }
        }

        double rate = totalAssigned > 0
                ? (double) totalCompleted / totalAssigned * 100 : 0;

        return AnalyticsResponse.CompletionRate.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .totalAssigned(totalAssigned)
                .totalCompleted(totalCompleted)
                .completionRate(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse.PassRate getCoursePassRate(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        long totalAttempted = gradeRepository.countByCourseId(courseId);
        long totalPassed = gradeRepository.countPassedByCourseId(courseId);
        double passRate = totalAttempted > 0
                ? (double) totalPassed / totalAttempted * 100 : 0;

        return AnalyticsResponse.PassRate.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .totalAttempted(totalAttempted)
                .totalPassed(totalPassed)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse.DepartmentAverageScore getDepartmentAverageScore(Long departmentId) {
        Department department = departmentRepository.findByIdAndDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", "id", departmentId));

        Double avgScore = gradeRepository.findAverageScoreByDepartmentId(departmentId);
        long totalGraded = gradeRepository.countByDepartmentId(departmentId);

        return AnalyticsResponse.DepartmentAverageScore.builder()
                .departmentId(departmentId)
                .departmentName(department.getName())
                .averageScore(avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0)
                .totalGraded(totalGraded)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsResponse.OverdueEmployee> getOverdueEmployees(Long courseId) {
        courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        List<CourseDepartmentAssignment> overdueAssignments =
                assignmentRepository.findOverdueAssignments(courseId);

        return overdueAssignments.stream()
                .flatMap(assignment -> {
                    List<Employee> deptEmployees = employeeRepository
                            .findByDepartmentId(assignment.getDepartment().getId());

                    return deptEmployees.stream()
                            .filter(emp -> {
                                // Check if employee has NOT passed the course
                                return !gradeRepository.existsByEmployeeIdAndCourseIdAndPassedTrue(
                                        emp.getId(), courseId);
                            })
                            .map(emp -> AnalyticsResponse.OverdueEmployee.builder()
                                    .employeeId(emp.getId())
                                    .employeeName(emp.getFirstName() + " " + emp.getLastName())
                                    .email(emp.getEmail())
                                    .departmentName(assignment.getDepartment().getName())
                                    .deadlineDate(assignment.getDeadlineDate().toString())
                                    .build());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsResponse.MostFailedQuestion> getMostFailedQuestions(Long courseId) {
        courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Get all QCM sections for the course
        List<CourseSection> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<Long> qcmSectionIds = sections.stream()
                .filter(s -> s.getContentType() == com.enterprise.poodle.enums.ContentType.QCM)
                .map(CourseSection::getId)
                .collect(Collectors.toList());

        if (qcmSectionIds.isEmpty()) {
            return List.of();
        }

        // Get all questions for these sections and compute fail rates per section
        return qcmSectionIds.stream()
                .flatMap(sId -> {
                    CourseSection sec = sectionRepository.findById(sId).orElse(null);
                    int passingScore = sec != null && sec.getCourse() != null
                            ? sec.getCourse().getPassingScore() : 70;
                    long totalAttempts = attemptRepository.countBySectionId(sId);
                    long failedAttempts = attemptRepository.countFailedBySectionId(sId, passingScore);
                    double failRate = totalAttempts > 0
                            ? (double) failedAttempts / totalAttempts * 100 : 0;

                    return questionRepository.findBySectionId(sId).stream()
                            .map(q -> AnalyticsResponse.MostFailedQuestion.builder()
                                    .questionId(q.getId())
                                    .questionText(q.getQuestionText())
                                    .totalAttempts(totalAttempts)
                                    .incorrectCount(failedAttempts)
                                    .failRate(Math.round(failRate * 100.0) / 100.0)
                                    .build());
                })
                .sorted((a, b) -> Double.compare(b.getFailRate(), a.getFailRate()))
                .limit(10)
                .collect(Collectors.toList());
    }
}
