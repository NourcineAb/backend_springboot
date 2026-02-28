package com.enterprise.poodle.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QCMAttemptResponse {
    private Long id;
    private Long sectionId;
    private Integer attemptNumber;
    private Integer score;
    private Integer totalQuestions;
    private boolean passed;
    private LocalDateTime takenAt;
}
