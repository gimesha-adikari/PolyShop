package com.polyshop.authservice.service.impl;

import com.polyshop.authservice.domain.AuditEvent;
import com.polyshop.authservice.repository.AuditEventRepository;
import com.polyshop.authservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbAuditService implements AuditService {

    private final AuditEventRepository repo;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void record(String eventType, Long userId, String message) {
        AuditEvent e = new AuditEvent();
        e.setEventType(eventType);
        e.setUserId(userId);
        e.setMessage(message);
        repo.save(e);

        try {
            if (kafkaTemplate != null) {
                kafkaTemplate.send("auth.audit", eventType, message);
            }
        } catch (Exception ignored) {}
    }
}
