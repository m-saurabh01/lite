package com.aircraft.emms.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PasswordResetRequest {
    private String serviceId;
    private String securityAnswer;
    private String newPassword;
}
