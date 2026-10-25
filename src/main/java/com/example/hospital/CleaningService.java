package com.example.hospital;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class CleaningService {
    private final int standardDurationMinutes;
    private final int terminalDurationMinutes;

    public CleaningService(
            int standardDurationMinutes,
            int terminalDurationMinutes) {
        if (standardDurationMinutes <= 0) {
            throw new IllegalArgumentException(
                "Standard cleaning duration must be positive"
            );
        }

        if (terminalDurationMinutes <= standardDurationMinutes) {
            throw new IllegalArgumentException(
                "Terminal cleaning must take longer than standard cleaning"
            );
        }

        this.standardDurationMinutes = standardDurationMinutes;
        this.terminalDurationMinutes = terminalDurationMinutes;
    }

    public CleaningTask discharge(
            Bed bed, LocalDateTime startedAt) {
        Objects.requireNonNull(bed, "Bed is required");
        Objects.requireNonNull(startedAt, "Start time is required");

        if (bed.state() != BedState.OCCUPIED) {
            throw new IllegalStateException(
                "Only an occupied bed can be discharged"
            );
        }

        Patient leavingPatient = bed.patient();

        if (leavingPatient == null) {
            throw new IllegalStateException(
                "Occupied bed must have an assigned patient"
            );
        }

        boolean isolationRequired =
            leavingPatient.isolationType() != IsolationType.NONE;

        CleaningType type = isolationRequired
            ? CleaningType.TERMINAL
            : CleaningType.STANDARD;

        int duration = isolationRequired
            ? terminalDurationMinutes
            : standardDurationMinutes;

        bed.markPendingDischarge();
        bed.startCleaning();

        return new CleaningTask(
            UUID.randomUUID().toString(),
            bed,
            type,
            startedAt,
            duration
        );
    }
}