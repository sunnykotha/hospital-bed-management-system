package com.example.hospital;

import java.util.Objects;
import java.util.Optional;

public record AdmissionResult(
        String requestId,
        Status status,
        Bed bed,
        BoardingRecord boardingRecord,
        EscalationRecord escalationRecord) {

    public enum Status {
        PLACED,
        BOARDING
    }

    public AdmissionResult {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request id is required");
        }
        Objects.requireNonNull(status, "Status is required");

        if (status == Status.PLACED && (bed == null || boardingRecord != null
                || escalationRecord != null)) {
            throw new IllegalArgumentException("Invalid placed result");
        }

        if (status == Status.BOARDING && (bed != null
                || boardingRecord == null || escalationRecord == null)) {
            throw new IllegalArgumentException("Invalid boarding result");
        }
    }

    public Optional<Bed> assignedBed() {
        return Optional.ofNullable(bed);
    }

    public Optional<BoardingRecord> boarding() {
        return Optional.ofNullable(boardingRecord);
    }

    public Optional<EscalationRecord> escalation() {
        return Optional.ofNullable(escalationRecord);
    }
}