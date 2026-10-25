package com.example.hospital;

import java.time.LocalDateTime;
import java.util.Objects;

public record EscalationRecord(
        String requestId,
        String reason,
        LocalDateTime createdAt) {

    public EscalationRecord {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request id is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Escalation reason is required");
        }
        Objects.requireNonNull(createdAt, "Creation time is required");
    }
}