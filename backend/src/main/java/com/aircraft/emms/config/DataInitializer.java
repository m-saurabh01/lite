package com.aircraft.emms.config;

import com.aircraft.emms.entity.User;
import com.aircraft.emms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ADMIN_ID = "ADMIN001";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final String DEFAULT_SECURITY_ANSWER = "emms2026";

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByServiceId(DEFAULT_ADMIN_ID).ifPresent(admin -> {
            boolean updated = false;
            if (!passwordEncoder.matches(DEFAULT_ADMIN_PASSWORD, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                admin.setSecurityAnswer(passwordEncoder.encode(DEFAULT_SECURITY_ANSWER));
                updated = true;
            }
            if (admin.getCreatedAt() == null) {
                admin.setCreatedAt(java.time.LocalDateTime.now());
                updated = true;
            }
            if (updated) {
                userRepository.save(admin);
                log.info("Default admin user initialized.");
            }
        });
    }
}
