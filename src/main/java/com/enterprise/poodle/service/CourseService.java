package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.request.CourseAssignmentRequest;
import com.enterprise.poodle.dto.request.CourseRequest;
import com.enterprise.poodle.dto.response.CourseAssignmentResponse;
import com.enterprise.poodle.dto.response.CourseResponse;
import com.enterprise.poodle.entity.*;
import com.enterprise.poodle.enums.ActionType;
import com.enterprise.poodle.enums.CourseStatus;
import com.enterprise.poodle.exception.BusinessException;
import com.enterprise.poodle.exception.DuplicateResourceException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.mapper.CourseMapper;
import com.enterprise.poodle.repository.*;
import com.enterprise.poodle.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CourseDepartmentAssignmentRepository assignmentRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeCourseGradeRepository gradeRepository;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final CourseMapper courseMapper;

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : CourseStatus.DRAFT)
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 70)
                .build();

        Course saved = courseRepository.save(course);

        if (request.getPrerequisiteCourseIds() != null) {
            addPrerequisites(saved, request.getPrerequisiteCourseIds());
        }

        auditLogService.logAction(ActionType.CREATE, "Course", saved.getId(), null,
                Map.of("title", saved.getTitle(), "status", saved.getStatus()));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable) {
        return courseRepository.findAllByDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> getEmployeeCourses(Pageable pageable) {
        Employee employee = securityUtils.getCurrentEmployee();
        if (employee.getDepartment() == null) {
            return Page.empty(pageable);
        }
        return courseRepository.findPublishedCoursesByDepartment(
                        employee.getDepartment().getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        // If employee, check access
        if (!securityUtils.isAdmin()) {
            Employee employee = securityUtils.getCurrentEmployee();
            if (course.getStatus() != CourseStatus.PUBLISHED) {
                throw new BusinessException("Cannot access draft or archived courses");
            }
            if (employee.getDepartment() == null ||
                    !assignmentRepository.existsByCourseIdAndDepartmentId(
                            course.getId(), employee.getDepartment().getId())) {
                throw new BusinessException("Course is not assigned to your department");
            }
            // Check prerequisites
            checkPrerequisites(employee.getId(), course.getId());
        }

        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        Map<String, Object> oldValues = Map.of(
                "title", course.getTitle(),
                "status", course.getStatus(),
                "passingScore", course.getPassingScore());

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        if (request.getPassingScore() != null) {
            course.setPassingScore(request.getPassingScore());
        }
        if (request.getPrerequisiteCourseIds() != null) {
            coursePrerequisiteRepository.deleteByCourseId(id);
            course.getPrerequisites().clear();
            addPrerequisites(course, request.getPrerequisiteCourseIds());
        }

        Course saved = courseRepository.save(course);

        auditLogService.logAction(ActionType.UPDATE, "Course", saved.getId(), oldValues,
                Map.of("title", saved.getTitle(), "status", saved.getStatus(),
                        "passingScore", saved.getPassingScore()));

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
        course.setDeleted(true);
        courseRepository.save(course);

        auditLogService.logAction(ActionType.DELETE, "Course", id, null, null);
    }

    @Transactional
    public CourseAssignmentResponse assignCourseToDepartment(
            Long courseId, Long departmentId, CourseAssignmentRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        Department department = departmentRepository.findByIdAndDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));

        if (assignmentRepository.existsByCourseIdAndDepartmentId(courseId, departmentId)) {
            throw new DuplicateResourceException(
                    "Course is already assigned to this department");
        }

        CourseDepartmentAssignment assignment = CourseDepartmentAssignment.builder()
                .course(course)
                .department(department)
                .deadlineDate(request.getDeadlineDate())
                .build();

        assignment = assignmentRepository.save(assignment);

        auditLogService.logAction(ActionType.CREATE, "CourseAssignment", assignment.getId(),
                null, Map.of("courseId", courseId, "departmentId", departmentId,
                        "deadline", request.getDeadlineDate()));

        return mapAssignmentToResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<CourseAssignmentResponse> getAssignedDepartments(Long courseId) {
        courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        return assignmentRepository.findByCourseId(courseId).stream()
                .map(this::mapAssignmentToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void unassignCourseFromDepartment(Long courseId, Long departmentId) {
        courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        departmentRepository.findByIdAndDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));

        CourseDepartmentAssignment assignment = assignmentRepository
                .findByCourseIdAndDepartmentId(courseId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment", "courseId/departmentId", courseId + "/" + departmentId));

        assignmentRepository.delete(assignment);

        auditLogService.logAction(ActionType.DELETE, "CourseAssignment", assignment.getId(),
                Map.of("courseId", courseId, "departmentId", departmentId), null);
    }

    public void checkPrerequisites(Long employeeId, Long courseId) {
        List<Long> prerequisiteIds =
                coursePrerequisiteRepository.findPrerequisiteCourseIdsByCourseId(courseId);

        for (Long prereqId : prerequisiteIds) {
            boolean passed = gradeRepository.existsByEmployeeIdAndCourseIdAndPassedTrue(
                    employeeId, prereqId);
            if (!passed) {
                Course prereq = courseRepository.findById(prereqId).orElse(null);
                String title = prereq != null ? prereq.getTitle() : "ID " + prereqId;
                throw new BusinessException(
                        "Prerequisite course not completed: " + title);
            }
        }
    }

    private void addPrerequisites(Course course, List<Long> prerequisiteIds) {
        List<CoursePrerequisite> prerequisites = new ArrayList<>();
        for (Long prereqId : prerequisiteIds) {
            Course prereqCourse = courseRepository.findByIdAndDeletedFalse(prereqId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prerequisite Course", "id", prereqId));
            if (prereqId.equals(course.getId())) {
                throw new BusinessException("A course cannot be its own prerequisite");
            }
            CoursePrerequisite prerequisite = CoursePrerequisite.builder()
                    .course(course)
                    .prerequisiteCourse(prereqCourse)
                    .build();
            prerequisites.add(prerequisite);
        }
        coursePrerequisiteRepository.saveAll(prerequisites);
    }

    private CourseResponse mapToResponse(Course course) {
        CourseResponse response = courseMapper.toResponse(course);
        List<Long> prereqIds = coursePrerequisiteRepository
                .findPrerequisiteCourseIdsByCourseId(course.getId());
        response.setPrerequisiteCourseIds(prereqIds);
        return response;
    }

    private CourseAssignmentResponse mapAssignmentToResponse(CourseDepartmentAssignment a) {
        return CourseAssignmentResponse.builder()
                .id(a.getId())
                .courseId(a.getCourse().getId())
                .courseTitle(a.getCourse().getTitle())
                .departmentId(a.getDepartment().getId())
                .departmentName(a.getDepartment().getName())
                .deadlineDate(a.getDeadlineDate())
                .build();
    }
}
