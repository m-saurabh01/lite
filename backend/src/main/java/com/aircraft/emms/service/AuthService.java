package com.aircraft.emms.service;

import com.aircraft.emms.dto.*;
import com.aircraft.emms.entity.Role;
import com.aircraft.emms.entity.User;
import com.aircraft.emms.repository.UserRepository;
import com.aircraft.emms.security.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenStore tokenStore;
    private final AuditService auditService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getServiceId(), request.getPassword())
        );

        User user = userRepository.findByServiceId(request.getServiceId())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String token = tokenStore.generateToken(user.getServiceId(), user.getRole().name());

        auditService.log(user.getServiceId(), "LOGIN", "User", user.getId(), "User logged in");

        return LoginResponse.builder()
                .token(token)
                .serviceId(user.getServiceId())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    public void logout(String token) {
        tokenStore.invalidateToken(token);
    }

    public String getSecurityQuestion(String serviceId) {
        User user = userRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getSecurityQuestion();
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByServiceId(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getSecurityAnswer() == null ||
            !passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            throw new BadCredentialsException("Security answer is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenStore.invalidateAllForUser(user.getServiceId());
        auditService.log(user.getServiceId(), "PASSWORD_RESET", "User", user.getId(), "Password reset via security question");
    }
}
