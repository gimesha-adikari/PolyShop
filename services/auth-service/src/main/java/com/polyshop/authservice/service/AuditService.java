package com.polyshop.authservice.service;

public interface AuditService {
    void record(String eventType, Long userId, String message);
}
