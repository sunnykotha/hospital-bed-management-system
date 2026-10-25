package com.example.hospital;

import java.time.LocalDateTime;
import java.util.Objects;

public final class CleaningTask {
    private final String id;
    private final Bed bed;
    private final CleaningType type;
    private final LocalDateTime startedAt;
    private final int durationMinutes;
    private LocalDateTime completedAt;

    public CleaningTask(
            String id,
            Bed bed,
            CleaningType type,
            LocalDateTime startedAt,
            int durationMinutes) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cleaning task id is required");
        }

        this.bed = Objects.requireNonNull(bed, "Bed is required");
        this.type = Objects.requireNonNull(type, "Cleaning type is required");
        this.startedAt = Objects.requireNonNull(
            startedAt, "Start time is required"
        );

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                "Cleaning duration must be positive"
            );
        }

        this.id = id;
        this.durationMinutes = durationMinutes;
    }

    public String id() {
        return id;
    }

    public Bed bed() {
        return bed;
    }

    public CleaningType type() {
        return type;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public synchronized LocalDateTime completedAt() {
        return completedAt;
    }

    public synchronized boolean isCompleted() {
        return completedAt != null;
    }

    public synchronized boolean isReadyAt(LocalDateTime time) {
        Objects.requireNonNull(time, "Time is required");

        return !time.isBefore(startedAt.plusMinutes(durationMinutes));
    }

    public synchronized void complete(LocalDateTime time) {
        Objects.requireNonNull(time, "Completion time is required");

        if (isCompleted()) {
            throw new IllegalStateException(
                "Cleaning task is already completed"
            );
        }

        if (time.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                "Completion cannot precede task start"
            );
        }

        if (time.isBefore(startedAt.plusMinutes(durationMinutes))) {
            throw new IllegalStateException(
                "Minimum cleaning duration has not elapsed"
            );
        }

        if (bed.state() != BedState.CLEANING) {
            throw new IllegalStateException(
                "Bed must be in CLEANING state"
            );
        }

        bed.finishCleaning();
        completedAt = time;
    }
}