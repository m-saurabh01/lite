package com.aircraft.emms.service;

import com.aircraft.emms.dto.FlightLogBookDto;
import com.aircraft.emms.dto.MeterEntryDto;
import com.aircraft.emms.entity.*;
import com.aircraft.emms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightLogBookServiceTest {

    @Mock private FlightLogBookRepository flbRepository;
    @Mock private SortieRepository sortieRepository;
    @Mock private UserRepository userRepository;
    @Mock private MeterDefinitionRepository meterDefRepository;
    @Mock private MeterEntryRepository meterEntryRepository;
    @Mock private AuditService auditService;

    @InjectMocks private FlightLogBookService flbService;

    private User pilot;
    private FlightLogBook testFlb;

    @BeforeEach
    void setUp() {
        pilot = User.builder().id(1L).serviceId("PLT001").name("Pilot").role(Role.PILOT).build();
        testFlb = FlightLogBook.builder()
                .id(1L)
                .aircraftType("F-16")
                .aircraftNumber("AC-101")
                .pilot(pilot)
                .status(FlbStatus.DRAFT)
                .meterEntries(new ArrayList<>())
                .build();
    }

    @Test
    void createFlb_shouldCalculateDuration() {
        FlightLogBookDto dto = FlightLogBookDto.builder()
                .aircraftType("F-16")
                .aircraftNumber("AC-101")
                .pilotId(1L)
                .actualTakeoffTime(LocalTime.of(9, 0))
                .actualLandingTime(LocalTime.of(11, 30))
                .meterEntries(List.of(
                        MeterEntryDto.builder().meterName("AIRFRAME_HOURS").meterValue(BigDecimal.valueOf(150)).build()
                ))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(pilot));
        when(flbRepository.save(any(FlightLogBook.class))).thenAnswer(inv -> {
            FlightLogBook f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        FlightLogBookDto result = flbService.createFlb(dto, "PLT001");

        assertThat(result.getDurationMinutes()).isEqualTo(150L);
        assertThat(result.getAircraftType()).isEqualTo("F-16");
    }

    @Test
    void closeFlb_notOpen_shouldThrow() {
        testFlb.setStatus(FlbStatus.DRAFT);
        when(flbRepository.findById(1L)).thenReturn(Optional.of(testFlb));

        assertThatThrownBy(() -> flbService.closeFlb(1L, "PLT001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPEN");
    }

    @Test
    void closeFlb_shouldSucceedWhenOpen() {
        testFlb.setStatus(FlbStatus.OPEN);
        when(flbRepository.findById(1L)).thenReturn(Optional.of(testFlb));
        when(flbRepository.save(any())).thenReturn(testFlb);
        when(meterDefRepository.findByAircraftTypeAndActiveTrueOrderByDisplayOrderAsc("F-16"))
                .thenReturn(List.of());

        FlightLogBookDto result = flbService.closeFlb(1L, "PLT001");

        assertThat(result.getStatus()).isEqualTo(FlbStatus.CLOSED);
    }
}
