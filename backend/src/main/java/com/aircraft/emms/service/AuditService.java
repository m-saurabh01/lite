package com.aircraft.emms.service;

import com.aircraft.emms.entity.AuditLog;
import com.aircraft.emms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String userServiceId, String action, String entityType, Long entityId, String details) {
        AuditLog entry = AuditLog.builder()
                .userServiceId(userServiceId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress("127.0.0.1")
                .build();
        auditLogRepository.save(entry);
    }

    public void logSync(String userServiceId, String action, String entityType, Long entityId, String details) {
        AuditLog entry = AuditLog.builder()
                .userServiceId(userServiceId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress("127.0.0.1")
                .build();
        auditLogRepository.save(entry);
    }
}
