package com.example.hospital;

import java.util.Objects;

public record AdmissionRequest(
        String id,
        Patient patient,
        int urgency,
        boolean fromEmergencyDepartment) {

    public AdmissionRequest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Admission request id is required");
        }
        Objects.requireNonNull(patient, "Patient is required");

        if (urgency < 0) {
            throw new IllegalArgumentException("Urgency cannot be negative");
        }
    }
}