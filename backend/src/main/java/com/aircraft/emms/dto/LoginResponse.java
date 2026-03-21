package com.aircraft.emms.dto;

import com.aircraft.emms.entity.Role;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String serviceId;
    private String name;
    private Role role;
}
