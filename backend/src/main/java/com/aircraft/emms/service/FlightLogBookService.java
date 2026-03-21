package com.aircraft.emms.service;

import com.aircraft.emms.dto.FlightLogBookDto;
import com.aircraft.emms.dto.MeterEntryDto;
import com.aircraft.emms.entity.*;
import com.aircraft.emms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightLogBookService {

    private final FlightLogBookRepository flbRepository;
    private final SortieRepository sortieRepository;
    private final UserRepository userRepository;
    private final MeterDefinitionRepository meterDefRepository;
    private final MeterEntryRepository meterEntryRepository;
    private final AuditService auditService;

    @Transactional
    public FlightLogBookDto createFlb(FlightLogBookDto dto, String createdBy) {
        User pilot = userRepository.findById(dto.getPilotId())
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found"));

        FlightLogBook flb = FlightLogBook.builder()
                .aircraftType(dto.getAircraftType())
                .aircraftNumber(dto.getAircraftNumber())
                .pilot(pilot)
                .actualTakeoffTime(dto.getActualTakeoffTime())
                .actualLandingTime(dto.getActualLandingTime())
                .remarks(dto.getRemarks())
                .status(FlbStatus.DRAFT)
                .build();

        // Link to sortie if provided
        if (dto.getSortieId() != null) {
            Sortie sortie = sortieRepository.findById(dto.getSortieId())
                    .orElseThrow(() -> new IllegalArgumentException("Sortie not found"));
            flb.setSortie(sortie);
        }

        flb.calculateDuration();

        // Process meter entries
        if (dto.getMeterEntries() != null) {
            for (MeterEntryDto meDto : dto.getMeterEntries()) {
                MeterEntry entry = MeterEntry.builder()
                        .meterName(meDto.getMeterName())
                        .meterValue(meDto.getMeterValue())
                        .previousValue(meDto.getPreviousValue())
                        .build();

                if (meDto.getMeterDefinitionId() != null) {
                    MeterDefinition def = meterDefRepository.findById(meDto.getMeterDefinitionId())
                            .orElse(null);
                    entry.setMeterDefinition(def);
                }

                flb.addMeterEntry(entry);
            }
        }

        flb = flbRepository.save(flb);
        auditService.log(createdBy, "CREATE_FLB", "FlightLogBook", flb.getId(),
                "Created FLB for aircraft " + flb.getAircraftNumber());

        return toDto(flb);
    }

    @Transactional
    public FlightLogBookDto updateFlb(Long id, FlightLogBookDto dto, String updatedBy) {
        FlightLogBook flb = flbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FLB not found: " + id));

        if (flb.getStatus() != FlbStatus.DRAFT) {
            throw new IllegalStateException("Can only edit FLB in DRAFT status");
        }

        flb.setActualTakeoffTime(dto.getActualTakeoffTime());
        flb.setActualLandingTime(dto.getActualLandingTime());
        flb.setRemarks(dto.getRemarks());
        flb.calculateDuration();

        // Update meter entries - clear and re-add
        flb.getMeterEntries().clear();
        if (dto.getMeterEntries() != null) {
            for (MeterEntryDto meDto : dto.getMeterEntries()) {
                MeterEntry entry = MeterEntry.builder()
                        .meterName(meDto.getMeterName())
                        .meterValue(meDto.getMeterValue())
                        .previousValue(meDto.getPreviousValue())
                        .build();

                if (meDto.getMeterDefinitionId() != null) {
                    MeterDefinition def = meterDefRepository.findById(meDto.getMeterDefinitionId())
                            .orElse(null);
                    entry.setMeterDefinition(def);
                }

                flb.addMeterEntry(entry);
            }
        }

        flb = flbRepository.save(flb);
        auditService.log(updatedBy, "UPDATE_FLB", "FlightLogBook", flb.getId(),
                "Updated FLB for aircraft " + flb.getAircraftNumber());

        return toDto(flb);
    }

    @Transactional
    public FlightLogBookDto submitFlb(Long id, String submittedBy) {
        FlightLogBook flb = flbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FLB not found: " + id));

        if (flb.getStatus() != FlbStatus.DRAFT) {
            throw new IllegalStateException("Can only submit FLB in DRAFT status");
        }

        // Validate mandatory meters
        validateMandatoryMeters(flb);

        flb.setStatus(FlbStatus.SUBMITTED);
        flb = flbRepository.save(flb);

        auditService.log(submittedBy, "SUBMIT_FLB", "FlightLogBook", flb.getId(),
                "Submitted FLB for aircraft " + flb.getAircraftNumber());

        return toDto(flb);
    }

    @Transactional
    public FlightLogBookDto approveFlb(Long id, String approvedBy) {
        FlightLogBook flb = flbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FLB not found: " + id));

        if (flb.getStatus() != FlbStatus.SUBMITTED) {
            throw new IllegalStateException("Can only approve FLB in SUBMITTED status");
        }

        flb.setStatus(FlbStatus.APPROVED);
        flb = flbRepository.save(flb);

        auditService.log(approvedBy, "APPROVE_FLB", "FlightLogBook", flb.getId(),
                "Approved FLB for aircraft " + flb.getAircraftNumber());

        return toDto(flb);
    }

    @Transactional(readOnly = true)
    public FlightLogBookDto getFlbById(Long id) {
        FlightLogBook flb = flbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FLB not found: " + id));
        return toDto(flb);
    }

    @Transactional(readOnly = true)
    public List<FlightLogBookDto> getFlbsByPilot(Long pilotId) {
        return flbRepository.findByPilotId(pilotId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<FlightLogBookDto> getAllFlbs() {
        return flbRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MeterEntryDto> getMeterDefinitionsForAircraft(String aircraftType) {
        List<MeterDefinition> defs = meterDefRepository
                .findByAircraftTypeAndActiveTrueOrderByDisplayOrderAsc(aircraftType);

        return defs.stream().map(def -> MeterEntryDto.builder()
                .meterDefinitionId(def.getId())
                .meterName(def.getMeterName())
                .mandatory(def.isMandatory())
                .unitOfMeasure(def.getUnitOfMeasure())
                .build()
        ).toList();
    }

    private void validateMandatoryMeters(FlightLogBook flb) {
        List<MeterDefinition> mandatoryDefs = meterDefRepository
                .findByAircraftTypeAndActiveTrueOrderByDisplayOrderAsc(flb.getAircraftType())
                .stream().filter(MeterDefinition::isMandatory).toList();

        List<String> missingMeters = new ArrayList<>();
        for (MeterDefinition def : mandatoryDefs) {
            boolean found = flb.getMeterEntries().stream()
                    .anyMatch(me -> me.getMeterName().equals(def.getMeterName()) && me.getMeterValue() != null);
            if (!found) {
                missingMeters.add(def.getMeterName());
            }
        }

        if (!missingMeters.isEmpty()) {
            throw new IllegalStateException("Missing mandatory meter entries: " + String.join(", ", missingMeters));
        }
    }

    private FlightLogBookDto toDto(FlightLogBook flb) {
        List<MeterEntryDto> meterDtos = flb.getMeterEntries().stream()
                .map(me -> MeterEntryDto.builder()
                        .id(me.getId())
                        .flbId(flb.getId())
                        .meterDefinitionId(me.getMeterDefinition() != null ? me.getMeterDefinition().getId() : null)
                        .meterName(me.getMeterName())
                        .meterValue(me.getMeterValue())
                        .previousValue(me.getPreviousValue())
                        .build()
                ).toList();

        return FlightLogBookDto.builder()
                .id(flb.getId())
                .sortieId(flb.getSortie() != null ? flb.getSortie().getId() : null)
                .sortieNumber(flb.getSortie() != null ? flb.getSortie().getSortieNumber() : null)
                .aircraftType(flb.getAircraftType())
                .aircraftNumber(flb.getAircraftNumber())
                .pilotId(flb.getPilot().getId())
                .pilotName(flb.getPilot().getName())
                .actualTakeoffTime(flb.getActualTakeoffTime())
                .actualLandingTime(flb.getActualLandingTime())
                .durationMinutes(flb.getDurationMinutes())
                .remarks(flb.getRemarks())
                .status(flb.getStatus())
                .meterEntries(meterDtos)
                .build();
    }
}
