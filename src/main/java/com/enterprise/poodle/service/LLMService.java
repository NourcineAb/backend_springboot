package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.response.QCMQuestionResponse;
import com.enterprise.poodle.entity.CourseSection;
import com.enterprise.poodle.entity.QCMQuestion;
import com.enterprise.poodle.enums.ContentType;
import com.enterprise.poodle.exception.BusinessException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.CourseSectionRepository;
import com.enterprise.poodle.repository.QCMQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final CourseSectionRepository sectionRepository;
    private final QCMQuestionRepository questionRepository;

    @Transactional
    public List<QCMQuestionResponse> generateQCMDraft(Long sectionId) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        if (section.getContentType() != ContentType.TEXT
                && section.getContentType() != ContentType.PDF) {
            throw new BusinessException(
                    "LLM QCM generation is only available for TEXT or PDF sections");
        }

        if (!section.isLlmDraftEnabled()) {
            throw new BusinessException("LLM draft generation is not enabled for this section");
        }

        // Placeholder: In production, send section content to external LLM API
        // For now, generate placeholder draft questions
        log.info("Generating LLM QCM draft for section {} (content: {})",
                sectionId, section.getContentUrl());

        List<QCMQuestion> draftQuestions = generatePlaceholderDraft(section);
        List<QCMQuestion> saved = questionRepository.saveAll(draftQuestions);

        return saved.stream()
                .map(q -> QCMQuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .correctOption(q.getCorrectOption())
                        .llmGenerated(q.isLlmGenerated())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Placeholder for LLM integration.
     * In production, replace this with an actual LLM API call (OpenAI, Claude, etc.)
     * that analyzes the section content and generates relevant QCM questions.
     */
    private List<QCMQuestion> generatePlaceholderDraft(CourseSection section) {
        List<QCMQuestion> questions = new ArrayList<>();

        // Create sample draft questions marked as LLM-generated
        QCMQuestion q1 = QCMQuestion.builder()
                .section(section)
                .questionText("[LLM DRAFT] Sample question about: " + section.getTitle())
                .optionA("Option A - placeholder")
                .optionB("Option B - placeholder")
                .optionC("Option C - placeholder")
                .optionD("Option D - placeholder")
                .correctOption("A")
                .llmGenerated(true)
                .build();
        questions.add(q1);

        return questions;
    }
}
