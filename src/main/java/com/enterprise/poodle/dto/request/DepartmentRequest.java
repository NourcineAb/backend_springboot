package com.enterprise.poodle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(max = 200, message = "Department name must not exceed 200 characters")
    private String name;

    private Long parentDepartmentId;
}
