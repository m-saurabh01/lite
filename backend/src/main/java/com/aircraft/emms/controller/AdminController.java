package com.aircraft.emms.controller;

import com.aircraft.emms.dto.ApiResponse;
import com.aircraft.emms.dto.UserDto;
import com.aircraft.emms.entity.AircraftDataSet;
import com.aircraft.emms.entity.Role;
import com.aircraft.emms.entity.User;
import com.aircraft.emms.repository.UserRepository;
import com.aircraft.emms.service.AircraftDataSetService;
import com.aircraft.emms.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AircraftDataSetService aircraftService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ---- Aircraft Dataset Management ----

    @GetMapping("/aircraft")
    public ResponseEntity<ApiResponse<List<AircraftDataSet>>> listAircraft() {
        return ResponseEntity.ok(ApiResponse.ok(aircraftService.getAllDatasets()));
    }

    @GetMapping("/aircraft/active")
    public ResponseEntity<ApiResponse<AircraftDataSet>> getActiveAircraft() {
        return aircraftService.getActiveDataset()
                .map(ds -> ResponseEntity.ok(ApiResponse.ok(ds)))
                .orElse(ResponseEntity.ok(ApiResponse.ok("No active aircraft", null)));
    }

    @PostMapping("/aircraft/{id}/activate")
    public ResponseEntity<ApiResponse<AircraftDataSet>> activateAircraft(
            @PathVariable Long id, Authentication auth) {
        AircraftDataSet ds = aircraftService.activate(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Aircraft activated", ds));
    }

    @DeleteMapping("/aircraft/{id}/truncate")
    public ResponseEntity<ApiResponse<Void>> truncateAircraft(
            @PathVariable Long id, Authentication auth) {
        aircraftService.truncateDataset(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Aircraft data truncated", null));
    }

    // ---- User Role Assignment ----

    @GetMapping("/aircraft-users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAircraftUsers() {
        AircraftDataSet dataset = aircraftService.requireActiveDataset();
        List<UserDto> users = userRepository.findByDatasetIdAndActiveTrue(dataset.getId()).stream()
                .map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/assign-role")
    public ResponseEntity<ApiResponse<UserDto>> assignRole(
            @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = Long.parseLong(body.get("userId"));
        String roleStr = body.get("role");

        Role role = Role.valueOf(roleStr.toUpperCase());
        if (role == Role.ADMIN) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot assign ADMIN role through this endpoint"));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Verify user belongs to active aircraft
        AircraftDataSet dataset = aircraftService.requireActiveDataset();
        if (user.getDataset() == null || !user.getDataset().getId().equals(dataset.getId())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User does not belong to the active aircraft dataset"));
        }

        // Add role (multi-role: does not remove existing roles)
        user.addRole(role);
        userRepository.save(user);

        auditService.log(auth.getName(), "ASSIGN_ROLE", "User", userId,
                "Assigned role " + role + " to user " + user.getServiceId() + " (roles: " + user.getRoles() + ")");

        return ResponseEntity.ok(ApiResponse.ok("Role assigned", toDto(user)));
    }

    @PostMapping("/remove-role")
    public ResponseEntity<ApiResponse<UserDto>> removeRole(
            @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = Long.parseLong(body.get("userId"));
        String roleStr = body.get("role");

        Role role = Role.valueOf(roleStr.toUpperCase());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        AircraftDataSet dataset = aircraftService.requireActiveDataset();
        if (user.getDataset() == null || !user.getDataset().getId().equals(dataset.getId())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User does not belong to the active aircraft dataset"));
        }

        user.removeRole(role);
        if (user.getRoleSet().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User must have at least one role"));
        }
        userRepository.save(user);

        auditService.log(auth.getName(), "REMOVE_ROLE", "User", userId,
                "Removed role " + role + " from user " + user.getServiceId());

        return ResponseEntity.ok(ApiResponse.ok("Role removed", toDto(user)));
    }

    // ---- Export Placeholder ----

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<String>> exportPlaceholder() {
        return ResponseEntity.ok(ApiResponse.ok("Export feature coming soon", "NOT_IMPLEMENTED"));
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .serviceId(user.getServiceId())
                .name(user.getName())
                .role(user.getRole())
                .roles(user.getRoleSet().stream().map(Enum::name).toList())
                .active(user.isActive())
                .build();
    }
}
