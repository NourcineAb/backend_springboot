package com.enterprise.poodle.service;

import com.enterprise.poodle.dto.request.LoginRequest;
import com.enterprise.poodle.dto.request.RegisterRequest;
import com.enterprise.poodle.dto.response.AuthResponse;
import com.enterprise.poodle.dto.response.EmployeeResponse;
import com.enterprise.poodle.entity.Department;
import com.enterprise.poodle.entity.Employee;
import com.enterprise.poodle.exception.DuplicateResourceException;
import com.enterprise.poodle.exception.ResourceNotFoundException;
import com.enterprise.poodle.repository.DepartmentRepository;
import com.enterprise.poodle.repository.EmployeeRepository;
import com.enterprise.poodle.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);
        Employee employee = (Employee) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(token)
                .email(employee.getEmail())
                .role(employee.getRole())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();
    }

    @Transactional
    public EmployeeResponse register(RegisterRequest request) {
        if (employeeRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Employee with email " + request.getEmail() + " already exists");
        }

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
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
