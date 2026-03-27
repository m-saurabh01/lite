package com.aircraft.emms.dto;

import com.aircraft.emms.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserDto {

    private Long id;

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    private Role role;
    private List<String> roles;

    private String securityQuestion;
    private String securityAnswer;
    private boolean active;
}
