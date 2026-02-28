package com.enterprise.poodle.service;

import com.enterprise.poodle.entity.AuditLog;
import com.enterprise.poodle.enums.ActionType;
import com.enterprise.poodle.repository.AuditLogRepository;
import com.enterprise.poodle.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(ActionType actionType, String entityType, Long entityId,
                          Object oldValue, Object newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(securityUtils.getCurrentEmployeeId())
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log values: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long userId, ActionType actionType, String entityType,
                          Long entityId, Object oldValue, Object newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log values: {}", e.getMessage());
        }
    }
}
