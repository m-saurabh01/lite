package com.aircraft.emms.service;

import com.aircraft.emms.dto.UserDto;
import com.aircraft.emms.entity.Role;
import com.aircraft.emms.entity.User;
import com.aircraft.emms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserDto createUser(UserDto dto, String createdBy) {
        if (userRepository.existsByServiceId(dto.getServiceId())) {
            throw new IllegalArgumentException("Service ID already exists: " + dto.getServiceId());
        }

        User user = User.builder()
                .serviceId(dto.getServiceId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .role(dto.getRole())
                .securityQuestion(dto.getSecurityQuestion())
                .securityAnswer(dto.getSecurityAnswer() != null ?
                        passwordEncoder.encode(dto.getSecurityAnswer()) : null)
                .active(true)
                .build();

        user = userRepository.save(user);
        auditService.log(createdBy, "CREATE_USER", "User", user.getId(),
                "Created user: " + user.getServiceId() + " with role: " + user.getRole());

        return toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto dto, String updatedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setName(dto.getName());
        user.setActive(dto.isActive());

        // Handle multi-role from dto.getRoles(), fallback to single role
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roleSet = dto.getRoles().stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
            user.setRoleSet(roleSet);
        } else if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getSecurityQuestion() != null) {
            user.setSecurityQuestion(dto.getSecurityQuestion());
        }
        if (dto.getSecurityAnswer() != null && !dto.getSecurityAnswer().isBlank()) {
            user.setSecurityAnswer(passwordEncoder.encode(dto.getSecurityAnswer()));
        }

        user = userRepository.save(user);
        auditService.log(updatedBy, "UPDATE_USER", "User", user.getId(),
                "Updated user: " + user.getServiceId());

        return toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(Role role) {
        return userRepository.findByRoleAndActiveTrue(role).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByServiceId(String serviceId) {
        User user = userRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + serviceId));
        return toDto(user);
    }

    @Transactional
    public void deactivateUser(Long id, String deactivatedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
        auditService.log(deactivatedBy, "DEACTIVATE_USER", "User", user.getId(),
                "Deactivated user: " + user.getServiceId());
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .serviceId(user.getServiceId())
                .name(user.getName())
                .role(user.getRole())
                .roles(user.getRoleSet().stream().map(Enum::name).toList())
                .securityQuestion(user.getSecurityQuestion())
                .active(user.isActive())
                .build();
    }
}
