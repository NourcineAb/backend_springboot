package com.enterprise.poodle.dto.response;

import com.enterprise.poodle.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
}
