package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.request.QCMAttemptRequest;
import com.enterprise.poodle.dto.request.QCMQuestionRequest;
import com.enterprise.poodle.dto.response.QCMAttemptResponse;
import com.enterprise.poodle.dto.response.QCMQuestionResponse;
import com.enterprise.poodle.entity.*;
import com.enterprise.poodle.enums.*;
import com.enterprise.poodle.exception.BusinessException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.*;
import com.enterprise.poodle.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QCMService {

    private final QCMQuestionRepository questionRepository;
    private final CourseSectionRepository sectionRepository;
    private final EmployeeQCMAttemptRepository attemptRepository;
    private final EmployeeCourseGradeRepository gradeRepository;
    private final EmployeeCourseProgressRepository progressRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final CertificateRepository certificateRepository;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final CertificateService certificateService;

    @Transactional
    public QCMQuestionResponse createQuestion(Long sectionId, QCMQuestionRequest request) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        if (section.getContentType() != ContentType.QCM) {
            throw new BusinessException("Questions can only be added to QCM sections");
        }

        QCMQuestion question = QCMQuestion.builder()
                .section(section)
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctOption(request.getCorrectOption())
                .llmGenerated(false)
                .build();

        QCMQuestion saved = questionRepository.save(question);

        auditLogService.logAction(ActionType.CREATE, "QCMQuestion", saved.getId(), null,
                Map.of("questionText", saved.getQuestionText(), "sectionId", sectionId));

        return mapToResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public List<QCMQuestionResponse> getQuestions(Long sectionId) {
        sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        boolean isAdmin = securityUtils.isAdmin();

        return questionRepository.findBySectionId(sectionId).stream()
                .map(q -> mapToResponse(q, isAdmin))
                .collect(Collectors.toList());
    }

    @Transactional
    public QCMAttemptResponse submitAttempt(Long sectionId, QCMAttemptRequest request) {
        Employee employee = securityUtils.getCurrentEmployee();
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        if (section.getContentType() != ContentType.QCM) {
            throw new BusinessException("This section is not a QCM");
        }

        // Check max attempts
        if (section.getMaxAttempts() != null) {
            long currentAttempts = attemptRepository.countByEmployeeIdAndSectionId(
                    employee.getId(), sectionId);
            if (currentAttempts >= section.getMaxAttempts()) {
                throw new BusinessException("Maximum number of attempts reached (" +
                        section.getMaxAttempts() + ")");
            }
        }

        // Calculate score
        List<QCMQuestion> questions = questionRepository.findBySectionId(sectionId);
        if (questions.isEmpty()) {
            throw new BusinessException("No questions found in this section");
        }

        int correctCount = 0;
        for (QCMQuestion question : questions) {
            String answer = request.getAnswers().get(question.getId());
            if (answer != null && answer.equals(question.getCorrectOption())) {
                correctCount++;
            }
        }

        int score = (int) Math.round((double) correctCount / questions.size() * 100);

        // Determine attempt number
        Integer maxAttempt = attemptRepository.findMaxAttemptNumber(
                employee.getId(), sectionId);
        int attemptNumber = (maxAttempt != null ? maxAttempt : 0) + 1;

        // Save attempt
        EmployeeQCMAttempt attempt = EmployeeQCMAttempt.builder()
                .employee(employee)
                .section(section)
                .attemptNumber(attemptNumber)
                .score(score)
                .takenAt(LocalDateTime.now())
                .build();

        attempt = attemptRepository.save(attempt);

        // Update progress
        updateProgress(employee, section);

        // If FINAL QCM, update grade and potentially issue certificate
        Course course = section.getCourse();
        boolean passed = false;
        if (section.getQcmType() == QcmType.FINAL) {
            passed = score >= course.getPassingScore();
            updateGrade(employee, course, score, passed);

            if (passed) {
                certificateService.generateCertificateIfEligible(employee, course);
            }
        }

        return QCMAttemptResponse.builder()
                .id(attempt.getId())
                .sectionId(sectionId)
                .attemptNumber(attempt.getAttemptNumber())
                .score(attempt.getScore())
                .totalQuestions(questions.size())
                .passed(section.getQcmType() == QcmType.FINAL ? passed : score >= 50)
                .takenAt(attempt.getTakenAt())
                .build();
    }

    private void updateProgress(Employee employee, CourseSection section) {
        EmployeeCourseProgress progress = progressRepository
                .findByEmployeeIdAndSectionId(employee.getId(), section.getId())
                .orElse(EmployeeCourseProgress.builder()
                        .employee(employee)
                        .course(section.getCourse())
                        .section(section)
                        .status(ProgressStatus.NOT_STARTED)
                        .build());

        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    private void updateGrade(Employee employee, Course course, int score, boolean passed) {
        EmployeeCourseGrade grade = gradeRepository
                .findByEmployeeIdAndCourseId(employee.getId(), course.getId())
                .orElse(EmployeeCourseGrade.builder()
                        .employee(employee)
                        .course(course)
                        .finalScore(0)
                        .passed(false)
                        .build());

        grade.setFinalScore(score);
        grade.setPassed(passed);
        grade.setUpdatedAt(LocalDateTime.now());
        gradeRepository.save(grade);
    }

    private QCMQuestionResponse mapToResponse(QCMQuestion question, boolean includeCorrect) {
        return QCMQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctOption(includeCorrect ? question.getCorrectOption() : null)
                .llmGenerated(question.isLlmGenerated())
                .build();
    }
}
