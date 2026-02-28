package com.enterprise.poodle.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsResponse {

    @Data
    @Builder
    public static class CompletionRate {
        private Long courseId;
        private String courseTitle;
        private long totalAssigned;
        private long totalCompleted;
        private double completionRate;
    }

    @Data
    @Builder
    public static class PassRate {
        private Long courseId;
        private String courseTitle;
        private long totalAttempted;
        private long totalPassed;
        private double passRate;
    }

    @Data
    @Builder
    public static class DepartmentAverageScore {
        private Long departmentId;
        private String departmentName;
        private double averageScore;
        private long totalGraded;
    }

    @Data
    @Builder
    public static class OverdueEmployee {
        private Long employeeId;
        private String employeeName;
        private String email;
        private String departmentName;
        private String deadlineDate;
    }

    @Data
    @Builder
    public static class MostFailedQuestion {
        private Long questionId;
        private String questionText;
        private long totalAttempts;
        private long incorrectCount;
        private double failRate;
    }
}
