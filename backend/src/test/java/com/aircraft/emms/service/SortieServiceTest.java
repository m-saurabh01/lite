package com.aircraft.emms.service;

import com.aircraft.emms.dto.SortieDto;
import com.aircraft.emms.entity.*;
import com.aircraft.emms.repository.SortieRepository;
import com.aircraft.emms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SortieServiceTest {

    @Mock private SortieRepository sortieRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks private SortieService sortieService;

    private User captain;
    private User pilot;
    private Sortie testSortie;

    @BeforeEach
    void setUp() {
        captain = User.builder().id(1L).serviceId("CAP001").name("Captain").role(Role.CAPTAIN).active(true).build();
        pilot = User.builder().id(2L).serviceId("PLT001").name("Pilot").role(Role.PILOT).active(true).build();

        testSortie = Sortie.builder()
                .id(1L)
                .sortieNumber("SRT-001")
                .aircraftType("F-16")
                .aircraftNumber("AC-101")
                .captain(captain)
                .scheduledDate(LocalDate.now().plusDays(1))
                .scheduledStart(LocalTime.of(9, 0))
                .scheduledEnd(LocalTime.of(11, 0))
                .status(SortieStatus.CREATED)
                .createdBy(captain)
                .build();
    }

    @Test
    void createSortie_shouldSucceed() {
        SortieDto dto = SortieDto.builder()
                .sortieNumber("SRT-002")
                .aircraftType("F-16")
                .aircraftNumber("AC-102")
                .scheduledDate(LocalDate.now().plusDays(1))
                .build();

        when(sortieRepository.existsBySortieNumber("SRT-002")).thenReturn(false);
        when(userRepository.findByServiceId("CAP001")).thenReturn(Optional.of(captain));
        when(sortieRepository.save(any(Sortie.class))).thenAnswer(inv -> {
            Sortie s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        SortieDto result = sortieService.createSortie(dto, "CAP001");

        assertThat(result.getSortieNumber()).isEqualTo("SRT-002");
        assertThat(result.getStatus()).isEqualTo(SortieStatus.CREATED);
    }

    @Test
    void createSortie_duplicateNumber_shouldThrow() {
        SortieDto dto = SortieDto.builder().sortieNumber("SRT-001").aircraftType("F-16")
                .aircraftNumber("AC-101").scheduledDate(LocalDate.now()).build();
        when(sortieRepository.existsBySortieNumber("SRT-001")).thenReturn(true);

        assertThatThrownBy(() -> sortieService.createSortie(dto, "CAP001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void assignPilot_shouldDetectClash() {
        testSortie.setStatus(SortieStatus.CREATED);
        Sortie clashing = Sortie.builder().sortieNumber("SRT-003")
                .scheduledStart(LocalTime.of(10, 0)).scheduledEnd(LocalTime.of(12, 0)).build();

        when(sortieRepository.findById(1L)).thenReturn(Optional.of(testSortie));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pilot));
        when(sortieRepository.findClashingSorties(eq(2L), any(), any(), any(), eq(1L)))
                .thenReturn(List.of(clashing));

        assertThatThrownBy(() -> sortieService.assignPilot(1L, 2L, "CAP001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clash");
    }

    @Test
    void assignPilot_nonPilotUser_shouldThrow() {
        when(sortieRepository.findById(1L)).thenReturn(Optional.of(testSortie));
        when(userRepository.findById(1L)).thenReturn(Optional.of(captain)); // captain, not pilot

        assertThatThrownBy(() -> sortieService.assignPilot(1L, 1L, "CAP001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a pilot");
    }

    @Test
    void updateStatus_invalidTransition_shouldThrow() {
        testSortie.setStatus(SortieStatus.COMPLETED);
        when(sortieRepository.findById(1L)).thenReturn(Optional.of(testSortie));

        assertThatThrownBy(() -> sortieService.updateSortieStatus(1L, SortieStatus.ACCEPTED, "PLT001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatus_validTransition_shouldSucceed() {
        testSortie.setStatus(SortieStatus.ASSIGNED);
        when(sortieRepository.findById(1L)).thenReturn(Optional.of(testSortie));
        when(sortieRepository.save(any())).thenReturn(testSortie);

        SortieDto result = sortieService.updateSortieStatus(1L, SortieStatus.ACCEPTED, "PLT001");

        assertThat(result.getStatus()).isEqualTo(SortieStatus.ACCEPTED);
    }
}
