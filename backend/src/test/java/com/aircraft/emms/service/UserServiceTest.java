package com.aircraft.emms.service;

import com.aircraft.emms.dto.UserDto;
import com.aircraft.emms.entity.Role;
import com.aircraft.emms.entity.User;
import com.aircraft.emms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .serviceId("PILOT001")
                .name("Test Pilot")
                .password("hashed")
                .role(Role.PILOT)
                .active(true)
                .build();
    }

    @Test
    void createUser_shouldSucceed() {
        UserDto dto = UserDto.builder()
                .serviceId("PILOT002")
                .name("New Pilot")
                .password("Pass@123")
                .role(Role.PILOT)
                .build();

        when(userRepository.existsByServiceId("PILOT002")).thenReturn(false);
        when(passwordEncoder.encode("Pass@123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserDto result = userService.createUser(dto, "ADMIN001");

        assertThat(result.getServiceId()).isEqualTo("PILOT002");
        assertThat(result.getRole()).isEqualTo(Role.PILOT);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateServiceId_shouldThrow() {
        UserDto dto = UserDto.builder().serviceId("PILOT001").name("Dup").password("p").role(Role.PILOT).build();
        when(userRepository.existsByServiceId("PILOT001")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(dto, "ADMIN001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getUserById_shouldReturnDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(1L);

        assertThat(result.getServiceId()).isEqualTo("PILOT001");
        assertThat(result.getName()).isEqualTo("Test Pilot");
    }

    @Test
    void getUserById_notFound_shouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivateUser_shouldSetInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deactivateUser(1L, "ADMIN001");

        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    void getUsersByRole_shouldFilterCorrectly() {
        when(userRepository.findByRoleAndActiveTrue(Role.PILOT)).thenReturn(List.of(testUser));

        List<UserDto> result = userService.getUsersByRole(Role.PILOT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.PILOT);
    }
}
