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
    private final AircraftDataSetService aircraftService;

    @Transactional
    public FlightLogBookDto createFlb(FlightLogBookDto dto, String createdBy) {
        AircraftDataSet dataset = aircraftService.requireActiveDataset();

        User pilot = userRepository.findById(dto.getPilotId())
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found"));

        FlightLogBook flb = FlightLogBook.builder()
                .aircraftType(dataset.getAircraftName())
                .aircraftNumber(dataset.getAssetNum())
                .pilot(pilot)
                .actualTakeoffTime(dto.getActualTakeoffTime())
                .actualLandingTime(dto.getActualLandingTime())
                .remarks(dto.getRemarks())
                .status(FlbStatus.DRAFT)
                .dataset(dataset)
                .build();

        if (dto.getSortieId() != null) {
            Sortie sortie = sortieRepository.findById(dto.getSortieId())
                    .orElseThrow(() -> new IllegalArgumentException("Sortie not found"));
            flb.setSortie(sortie);
        }

        // Validate landing time after takeoff time
        if (flb.getActualTakeoffTime() != null && flb.getActualLandingTime() != null
                && !flb.getActualLandingTime().isAfter(flb.getActualTakeoffTime())) {
            throw new IllegalArgumentException("Landing time must be after takeoff time");
        }

        flb.calculateDuration();

        if (dto.getMeterEntries() != null) {
            for (MeterEntryDto meDto : dto.getMeterEntries()) {
                MeterEntry entry = MeterEntry.builder()
                        .meterName(meDto.getMeterName())
                        .meterValue(meDto.getMeterValue())
                        .previousValue(meDto.getPreviousValue())
                        .build();
                if (meDto.getMeterDefinitionId() != null) {
                    MeterDefinition def = meterDefRepository.findById(meDto.getMeterDefinitionId()).orElse(null);
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

        if (flb.getStatus() != FlbStatus.OPEN && flb.getStatus() != FlbStatus.DRAFT) {
            throw new IllegalStateException("Can only edit FLB in OPEN/DRAFT status");
        }

        flb.setActualTakeoffTime(dto.getActualTakeoffTime());
        flb.setActualLandingTime(dto.getActualLandingTime());
        flb.setRemarks(dto.getRemarks());
        flb.calculateDuration();

        // Auto-transition: DRAFT -> OPEN when times are filled
        if (flb.getStatus() == FlbStatus.DRAFT
                && flb.getActualTakeoffTime() != null
                && flb.getActualLandingTime() != null) {
            flb.setStatus(FlbStatus.OPEN);
        }

        flb.getMeterEntries().clear();
        if (dto.getMeterEntries() != null) {
            for (MeterEntryDto meDto : dto.getMeterEntries()) {
                MeterEntry entry = MeterEntry.builder()
                        .meterName(meDto.getMeterName())
                        .meterValue(meDto.getMeterValue())
                        .previousValue(meDto.getPreviousValue())
                        .build();
                if (meDto.getMeterDefinitionId() != null) {
                    MeterDefinition def = meterDefRepository.findById(meDto.getMeterDefinitionId()).orElse(null);
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
    public FlightLogBookDto closeFlb(Long id, String closedBy) {
        FlightLogBook flb = flbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FLB not found: " + id));

        if (flb.getStatus() != FlbStatus.OPEN) {
            throw new IllegalStateException("Can only close FLB in OPEN status");
        }

        validateMandatoryMeters(flb);

        flb.setStatus(FlbStatus.CLOSED);
        flb = flbRepository.save(flb);

        auditService.log(closedBy, "CLOSE_FLB", "FlightLogBook", flb.getId(),
                "Closed FLB for aircraft " + flb.getAircraftNumber());

        return toDto(flb);
    }

    @Transactional
    public FlightLogBookDto abortFlb(Long id, String abortedBy) {
        FlightLogBook flb = flbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FLB not found: " + id));

        if (flb.getStatus() != FlbStatus.OPEN && flb.getStatus() != FlbStatus.DRAFT) {
            throw new IllegalStateException("Can only abort FLB in DRAFT/OPEN status");
        }

        flb.setStatus(FlbStatus.ABORTED);
        flb = flbRepository.save(flb);

        auditService.log(abortedBy, "ABORT_FLB", "FlightLogBook", flb.getId(),
                "Aborted FLB for aircraft " + flb.getAircraftNumber());

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

    /**
     * Fetch meter definitions for the active aircraft's dataset.
     */
    @Transactional(readOnly = true)
    public List<MeterEntryDto> getMeterDefinitionsForActiveAircraft() {
        AircraftDataSet dataset = aircraftService.requireActiveDataset();
        List<MeterDefinition> defs = meterDefRepository
                .findByDatasetIdAndActiveTrueOrderByDisplayOrderAsc(dataset.getId());

        return defs.stream().map(def -> MeterEntryDto.builder()
                .meterDefinitionId(def.getId())
                .meterName(def.getMeterName())
                .mandatory(def.isMandatory())
                .unitOfMeasure(def.getUnitOfMeasure())
                .build()
        ).toList();
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
        // Get mandatory meters from dataset if available, else from aircraft type
        List<MeterDefinition> mandatoryDefs;
        if (flb.getDataset() != null) {
            mandatoryDefs = meterDefRepository
                    .findByDatasetIdAndActiveTrueOrderByDisplayOrderAsc(flb.getDataset().getId())
                    .stream().filter(MeterDefinition::isMandatory).toList();
        } else {
            mandatoryDefs = meterDefRepository
                    .findByAircraftTypeAndActiveTrueOrderByDisplayOrderAsc(flb.getAircraftType())
                    .stream().filter(MeterDefinition::isMandatory).toList();
        }

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
                        .mandatory(me.getMeterDefinition() != null && me.getMeterDefinition().isMandatory())
                        .unitOfMeasure(me.getMeterDefinition() != null ? me.getMeterDefinition().getUnitOfMeasure() : null)
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
