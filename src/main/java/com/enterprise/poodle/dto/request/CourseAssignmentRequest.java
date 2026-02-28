package com.enterprise.poodle.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CourseAssignmentRequest {

    @NotNull(message = "Deadline date is required")
    private LocalDate deadlineDate;
}
