package com.aircraft.emms.controller;

import com.aircraft.emms.dto.ApiResponse;
import com.aircraft.emms.dto.SortieDto;
import com.aircraft.emms.entity.SortieStatus;
import com.aircraft.emms.service.SortieService;
import com.aircraft.emms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sorties")
@RequiredArgsConstructor
public class SortieController {

    private final SortieService sortieService;
    private final UserService userService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTAIN')")
    public ResponseEntity<ApiResponse<SortieDto>> createSortie(
            @Valid @RequestBody SortieDto dto,
            Authentication auth) {
        SortieDto created = sortieService.createSortie(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sortie created", created));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTAIN')")
    public ResponseEntity<ApiResponse<SortieDto>> assignPilot(
            @RequestParam Long sortieId,
            @RequestParam Long pilotId,
            Authentication auth) {
        SortieDto updated = sortieService.assignPilot(sortieId, pilotId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Pilot assigned", updated));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('PILOT')")
    public ResponseEntity<ApiResponse<SortieDto>> acceptSortie(
            @PathVariable Long id,
            Authentication auth) {
        SortieDto updated = sortieService.updateSortieStatus(id, SortieStatus.ACCEPTED, auth.getName(), null);
        return ResponseEntity.ok(ApiResponse.ok("Sortie accepted", updated));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PILOT')")
    public ResponseEntity<ApiResponse<SortieDto>> rejectSortie(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            Authentication auth) {
        SortieDto updated = sortieService.updateSortieStatus(id, SortieStatus.REJECTED, auth.getName(), remarks);
        return ResponseEntity.ok(ApiResponse.ok("Sortie rejected", updated));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<SortieDto>> startSortie(
            @PathVariable Long id,
            Authentication auth) {
        SortieDto updated = sortieService.updateSortieStatus(id, SortieStatus.IN_PROGRESS, auth.getName(), null);
        return ResponseEntity.ok(ApiResponse.ok("Sortie started", updated));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<SortieDto>> completeSortie(
            @PathVariable Long id,
            Authentication auth) {
        SortieDto updated = sortieService.updateSortieStatus(id, SortieStatus.COMPLETED, auth.getName(), null);
        return ResponseEntity.ok(ApiResponse.ok("Sortie completed", updated));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTAIN')")
    public ResponseEntity<ApiResponse<SortieDto>> cancelSortie(
            @PathVariable Long id,
            Authentication auth) {
        SortieDto updated = sortieService.updateSortieStatus(id, SortieStatus.CANCELLED, auth.getName(), null);
        return ResponseEntity.ok(ApiResponse.ok("Sortie cancelled", updated));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<SortieDto>> closeSortie(
            @PathVariable Long id,
            Authentication auth) {
        SortieDto updated = sortieService.updateSortieStatus(id, SortieStatus.CLOSED, auth.getName(), null);
        return ResponseEntity.ok(ApiResponse.ok("Sortie closed", updated));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SortieDto>>> getAllSorties() {
        return ResponseEntity.ok(ApiResponse.ok(sortieService.getAllSorties()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SortieDto>> getSortieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sortieService.getSortieById(id)));
    }

    @GetMapping("/my-sorties")
    public ResponseEntity<ApiResponse<List<SortieDto>>> getMySorties(Authentication auth) {
        var user = userService.getUserByServiceId(auth.getName());
        var roles = user.getRoles() != null ? user.getRoles() : List.of(user.getRole().name());
        List<SortieDto> sorties;
        if (roles.contains("ADMIN")) {
            sorties = sortieService.getAllSorties();
        } else if (roles.contains("CAPTAIN") && roles.contains("PILOT")) {
            // User has both roles — fetch sorties where they are captain OR pilot
            sorties = sortieService.getSortiesByCaptainOrPilot(user.getId());
        } else if (roles.contains("CAPTAIN")) {
            sorties = sortieService.getSortiesByCaptain(user.getId());
        } else if (roles.contains("PILOT")) {
            sorties = sortieService.getSortiesByPilot(user.getId());
        } else {
            sorties = sortieService.getAllSorties();
        }
        return ResponseEntity.ok(ApiResponse.ok(sorties));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('PILOT')")
    public ResponseEntity<ApiResponse<List<SortieDto>>> getPendingSorties(Authentication auth) {
        var user = userService.getUserByServiceId(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(sortieService.getPendingSortiesForPilot(user.getId())));
    }
}
