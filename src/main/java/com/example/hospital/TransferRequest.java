package com.example.hospital;

import java.util.Objects;

public record TransferRequest(
        String id,
        Patient patient,
        Bed sourceBed) {

    public TransferRequest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Transfer id is required");
        }
        Objects.requireNonNull(patient, "Patient is required");
        Objects.requireNonNull(sourceBed, "Source bed is required");
    }
}