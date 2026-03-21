package com.aircraft.emms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    @NotBlank(message = "Password is required")
    private String password;
}
