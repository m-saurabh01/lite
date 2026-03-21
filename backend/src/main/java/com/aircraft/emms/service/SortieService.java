package com.aircraft.emms.service;

import com.aircraft.emms.dto.SortieDto;
import com.aircraft.emms.entity.*;
import com.aircraft.emms.repository.SortieRepository;
import com.aircraft.emms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SortieService {

    private final SortieRepository sortieRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public SortieDto createSortie(SortieDto dto, String createdBy) {
        if (sortieRepository.existsBySortieNumber(dto.getSortieNumber())) {
            throw new IllegalArgumentException("Sortie number already exists: " + dto.getSortieNumber());
        }

        User creator = userRepository.findByServiceId(createdBy)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        Sortie sortie = Sortie.builder()
                .sortieNumber(dto.getSortieNumber())
                .aircraftType(dto.getAircraftType())
                .aircraftNumber(dto.getAircraftNumber())
                .scheduledDate(dto.getScheduledDate())
                .scheduledStart(dto.getScheduledStart())
                .scheduledEnd(dto.getScheduledEnd())
                .status(SortieStatus.CREATED)
                .remarks(dto.getRemarks())
                .createdBy(creator)
                .build();

        // Set captain if provided
        if (dto.getCaptainId() != null) {
            User captain = userRepository.findById(dto.getCaptainId())
                    .orElseThrow(() -> new IllegalArgumentException("Captain not found"));
            sortie.setCaptain(captain);
        } else {
            // If creator is captain, auto-assign
            if (creator.getRole() == Role.CAPTAIN) {
                sortie.setCaptain(creator);
            }
        }

        sortie = sortieRepository.save(sortie);
        auditService.log(createdBy, "CREATE_SORTIE", "Sortie", sortie.getId(),
                "Created sortie: " + sortie.getSortieNumber());

        return toDto(sortie);
    }

    @Transactional
    public SortieDto assignPilot(Long sortieId, Long pilotId, String assignedBy) {
        Sortie sortie = sortieRepository.findById(sortieId)
                .orElseThrow(() -> new IllegalArgumentException("Sortie not found: " + sortieId));

        User pilot = userRepository.findById(pilotId)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + pilotId));

        if (pilot.getRole() != Role.PILOT) {
            throw new IllegalArgumentException("User is not a pilot: " + pilot.getServiceId());
        }

        // Check for scheduling clashes
        if (sortie.getScheduledStart() != null && sortie.getScheduledEnd() != null) {
            List<Sortie> clashes = sortieRepository.findClashingSorties(
                    pilotId, sortie.getScheduledDate(),
                    sortie.getScheduledStart(), sortie.getScheduledEnd(), sortie.getId());
            if (!clashes.isEmpty()) {
                String clashInfo = clashes.stream()
                        .map(s -> s.getSortieNumber() + " (" + s.getScheduledStart() + "-" + s.getScheduledEnd() + ")")
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Scheduling clash detected with: " + clashInfo);
            }
        }

        sortie.setPilot(pilot);
        sortie.setStatus(SortieStatus.ASSIGNED);
        sortie = sortieRepository.save(sortie);

        auditService.log(assignedBy, "ASSIGN_PILOT", "Sortie", sortie.getId(),
                "Assigned pilot " + pilot.getServiceId() + " to sortie " + sortie.getSortieNumber());

        return toDto(sortie);
    }

    @Transactional
    public SortieDto updateSortieStatus(Long sortieId, SortieStatus newStatus, String updatedBy) {
        Sortie sortie = sortieRepository.findById(sortieId)
                .orElseThrow(() -> new IllegalArgumentException("Sortie not found: " + sortieId));

        validateStatusTransition(sortie.getStatus(), newStatus);
        sortie.setStatus(newStatus);
        sortie = sortieRepository.save(sortie);

        auditService.log(updatedBy, "UPDATE_SORTIE_STATUS", "Sortie", sortie.getId(),
                "Status changed to " + newStatus + " for sortie " + sortie.getSortieNumber());

        return toDto(sortie);
    }

    @Transactional(readOnly = true)
    public SortieDto getSortieById(Long id) {
        Sortie sortie = sortieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sortie not found: " + id));
        SortieDto dto = toDto(sortie);
        enrichWithClashInfo(dto, sortie);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<SortieDto> getAllSorties() {
        return sortieRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SortieDto> getSortiesByPilot(Long pilotId) {
        return sortieRepository.findByPilotId(pilotId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SortieDto> getSortiesByCaptain(Long captainId) {
        return sortieRepository.findByCaptainId(captainId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SortieDto> getPendingSortiesForPilot(Long pilotId) {
        return sortieRepository.findByPilotIdAndStatusIn(pilotId,
                List.of(SortieStatus.ASSIGNED)).stream().map(this::toDto).toList();
    }

    private void validateStatusTransition(SortieStatus current, SortieStatus next) {
        boolean valid = switch (current) {
            case CREATED -> next == SortieStatus.ASSIGNED || next == SortieStatus.CANCELLED;
            case ASSIGNED -> next == SortieStatus.ACCEPTED || next == SortieStatus.REJECTED || next == SortieStatus.CANCELLED;
            case ACCEPTED -> next == SortieStatus.IN_PROGRESS || next == SortieStatus.CANCELLED;
            case IN_PROGRESS -> next == SortieStatus.COMPLETED || next == SortieStatus.CANCELLED;
            case REJECTED -> next == SortieStatus.ASSIGNED || next == SortieStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + current + " to " + next);
        }
    }

    private void enrichWithClashInfo(SortieDto dto, Sortie sortie) {
        if (sortie.getPilot() != null && sortie.getScheduledStart() != null && sortie.getScheduledEnd() != null) {
            List<Sortie> clashes = sortieRepository.findClashingSorties(
                    sortie.getPilot().getId(), sortie.getScheduledDate(),
                    sortie.getScheduledStart(), sortie.getScheduledEnd(), sortie.getId());
            if (!clashes.isEmpty()) {
                dto.setHasClash(true);
                dto.setClashDetails(clashes.stream()
                        .map(s -> s.getSortieNumber() + " (" + s.getScheduledStart() + "-" + s.getScheduledEnd() + ")")
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    private SortieDto toDto(Sortie sortie) {
        return SortieDto.builder()
                .id(sortie.getId())
                .sortieNumber(sortie.getSortieNumber())
                .aircraftType(sortie.getAircraftType())
                .aircraftNumber(sortie.getAircraftNumber())
                .captainId(sortie.getCaptain() != null ? sortie.getCaptain().getId() : null)
                .captainName(sortie.getCaptain() != null ? sortie.getCaptain().getName() : null)
                .pilotId(sortie.getPilot() != null ? sortie.getPilot().getId() : null)
                .pilotName(sortie.getPilot() != null ? sortie.getPilot().getName() : null)
                .scheduledDate(sortie.getScheduledDate())
                .scheduledStart(sortie.getScheduledStart())
                .scheduledEnd(sortie.getScheduledEnd())
                .status(sortie.getStatus())
                .remarks(sortie.getRemarks())
                .build();
    }
}
