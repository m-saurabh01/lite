package com.aircraft.emms.dto;

import com.aircraft.emms.entity.Role;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String serviceId;
    private String name;
    private Role role;
    private List<String> roles;
}
