package com.example.hospital;

import java.time.LocalDateTime;
import java.util.Objects;

public record BoardingRecord(
        String requestId,
        Patient patient,
        LocalDateTime createdAt) {

    public BoardingRecord {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request id is required");
        }
        Objects.requireNonNull(patient, "Patient is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
    }
}