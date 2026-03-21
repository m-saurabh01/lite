package com.aircraft.emms.controller;

import com.aircraft.emms.dto.ApiResponse;
import com.aircraft.emms.entity.XmlImportLog;
import com.aircraft.emms.xmlimport.XmlImportResult;
import com.aircraft.emms.xmlimport.XmlImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/xml-import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class XmlImportController {

    private final XmlImportService xmlImportService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<XmlImportResult>> importZip(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only ZIP files are accepted"));
        }

        XmlImportResult result = xmlImportService.importZip(file, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Import completed", result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<XmlImportLog>>> getImportHistory() {
        return ResponseEntity.ok(ApiResponse.ok(xmlImportService.getImportHistory()));
    }
}
