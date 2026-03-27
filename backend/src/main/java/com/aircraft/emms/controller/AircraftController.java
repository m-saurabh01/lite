package com.aircraft.emms.controller;

import com.aircraft.emms.dto.ApiResponse;
import com.aircraft.emms.entity.AircraftDataSet;
import com.aircraft.emms.service.AircraftDataSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aircraft")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftDataSetService aircraftService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<AircraftDataSet>> getActiveAircraft() {
        return aircraftService.getActiveDataset()
                .map(ds -> ResponseEntity.ok(ApiResponse.ok(ds)))
                .orElse(ResponseEntity.ok(ApiResponse.ok("No active aircraft", null)));
    }
}
