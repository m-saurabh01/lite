package com.aircraft.emms.controller;

import com.aircraft.emms.dto.ApiResponse;
import com.aircraft.emms.dto.FlightLogBookDto;
import com.aircraft.emms.dto.MeterEntryDto;
import com.aircraft.emms.service.FlightLogBookService;
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
@RequestMapping("/api/flb")
@RequiredArgsConstructor
public class FlightLogBookController {

    private final FlightLogBookService flbService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<FlightLogBookDto>> createFlb(
            @Valid @RequestBody FlightLogBookDto dto,
            Authentication auth) {
        FlightLogBookDto created = flbService.createFlb(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("FLB created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightLogBookDto>> updateFlb(
            @PathVariable Long id,
            @Valid @RequestBody FlightLogBookDto dto,
            Authentication auth) {
        FlightLogBookDto updated = flbService.updateFlb(id, dto, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("FLB updated", updated));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<FlightLogBookDto>> submitFlb(
            @PathVariable Long id,
            Authentication auth) {
        FlightLogBookDto submitted = flbService.submitFlb(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("FLB submitted", submitted));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAPTAIN')")
    public ResponseEntity<ApiResponse<FlightLogBookDto>> approveFlb(
            @PathVariable Long id,
            Authentication auth) {
        FlightLogBookDto approved = flbService.approveFlb(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("FLB approved", approved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightLogBookDto>> getFlbById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(flbService.getFlbById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightLogBookDto>>> getAllFlbs() {
        return ResponseEntity.ok(ApiResponse.ok(flbService.getAllFlbs()));
    }

    @GetMapping("/my-flbs")
    public ResponseEntity<ApiResponse<List<FlightLogBookDto>>> getMyFlbs(Authentication auth) {
        var user = userService.getUserByServiceId(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(flbService.getFlbsByPilot(user.getId())));
    }

    @GetMapping("/meter-definitions/{aircraftType}")
    public ResponseEntity<ApiResponse<List<MeterEntryDto>>> getMeterDefinitions(
            @PathVariable String aircraftType) {
        return ResponseEntity.ok(ApiResponse.ok(flbService.getMeterDefinitionsForAircraft(aircraftType)));
    }
}
