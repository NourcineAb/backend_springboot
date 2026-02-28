package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.request.EmployeeUpdateRequest;
import com.enterprise.poodle.dto.response.EmployeeResponse;
import com.enterprise.poodle.entity.Department;
import com.enterprise.poodle.entity.Employee;
import com.enterprise.poodle.exception.DuplicateResourceException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.DepartmentRepository;
import com.enterprise.poodle.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAllByDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(employee.getEmail())) {
            if (employeeRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                throw new DuplicateResourceException(
                        "Employee with email " + request.getEmail() + " already exists");
            }
            employee.setEmail(request.getEmail());
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        employee.setDeleted(true);
        employeeRepository.save(employee);
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .build();
    }
}
