package com.example.hospital;

import java.util.Objects;

public final class Bed {
    private final String id;
    private final Room room;
    private volatile BedState state;
    private volatile Patient patient;

    public Bed(String id, Room room) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Bed id is required");
        }

        this.id = id;
        this.room = Objects.requireNonNull(room, "Room is required");
        this.state = BedState.AVAILABLE;
    }

    public String id() {
        return id;
    }

    public Room room() {
        return room;
    }

    public BedState state() {
        return state;
    }

    public Patient patient() {
        return patient;
    }

    public synchronized void reserve() {
        transitionTo(BedState.RESERVED);
    }

    public synchronized void occupy() {
        transitionTo(BedState.OCCUPIED);
    }

    public synchronized void occupy(Patient patient) {
        Objects.requireNonNull(patient, "Patient is required");
        transitionTo(BedState.OCCUPIED);
        this.patient = patient;
    }

    public synchronized void releaseReservation() {
        transitionTo(BedState.AVAILABLE);
    }

    public synchronized void markPendingDischarge() {
        transitionTo(BedState.PENDING_DISCHARGE);
    }

    public synchronized void startCleaning() {
        transitionTo(BedState.CLEANING);
        patient = null;
    }

    public synchronized void finishCleaning() {
        transitionTo(BedState.AVAILABLE);
    }

    public synchronized void block() {
        transitionTo(BedState.BLOCKED);
    }

    public synchronized void unblock() {
        transitionTo(BedState.AVAILABLE);
    }

    private void transitionTo(BedState next) {
        Objects.requireNonNull(next, "Next bed state is required");

        boolean allowed = switch (state) {
            case AVAILABLE ->
                next == BedState.RESERVED || next == BedState.BLOCKED;
            case RESERVED ->
                next == BedState.OCCUPIED || next == BedState.AVAILABLE;
            case OCCUPIED ->
                next == BedState.PENDING_DISCHARGE;
            case PENDING_DISCHARGE ->
                next == BedState.CLEANING;
            case CLEANING ->
                next == BedState.AVAILABLE || next == BedState.BLOCKED;
            case BLOCKED ->
                next == BedState.AVAILABLE;
        };

        if (!allowed) {
            throw new IllegalStateException(
                "Invalid bed transition: " + state + " -> " + next
            );
        }

        state = next;
    }

    public static void swapPatients(Bed first, Bed second) {
        Objects.requireNonNull(first, "First bed is required");
        Objects.requireNonNull(second, "Second bed is required");

        if (first == second) {
            throw new IllegalArgumentException(
                "Cannot swap a bed with itself"
            );
        }

        withOrderedLocks(first, second, () -> {
            if (first.state != BedState.OCCUPIED
                    || second.state != BedState.OCCUPIED) {
                throw new IllegalStateException(
                    "Both beds must be occupied"
                );
            }

            if (first.patient == null || second.patient == null) {
                throw new IllegalStateException(
                    "Both beds must have assigned patients"
                );
            }

            Patient firstPatient = first.patient;
            first.patient = second.patient;
            second.patient = firstPatient;
        });
    }

    public static void completeTransfer(
            Bed source, Bed target, Patient patient) {
        Objects.requireNonNull(source, "Source bed is required");
        Objects.requireNonNull(target, "Target bed is required");
        Objects.requireNonNull(patient, "Patient is required");

        if (source == target) {
            throw new IllegalArgumentException(
                "Source and target beds must be different"
            );
        }

        withOrderedLocks(source, target, () -> {
            if (source.state != BedState.OCCUPIED
                    || !patient.equals(source.patient)) {
                throw new IllegalStateException(
                    "Source bed must contain the transferring patient"
                );
            }

            if (target.state != BedState.RESERVED) {
                throw new IllegalStateException(
                    "Target bed must be reserved"
                );
            }

            target.transitionTo(BedState.OCCUPIED);
            target.patient = patient;

            source.transitionTo(BedState.PENDING_DISCHARGE);
            source.transitionTo(BedState.CLEANING);
            source.patient = null;
        });
    }

    private static void withOrderedLocks(
            Bed first, Bed second, Runnable operation) {
        Bed lockFirst = first.id.compareTo(second.id) <= 0 ? first : second;
        Bed lockSecond = lockFirst == first ? second : first;

        synchronized (lockFirst) {
            synchronized (lockSecond) {
                operation.run();
            }
        }
    }

    public synchronized void updatePatientAcuity(AcuityScore newAcuityScore) {
        Objects.requireNonNull(
            newAcuityScore, "New acuity score is required"
        );

        if (state != BedState.OCCUPIED || patient == null) {
            throw new IllegalStateException(
                "Acuity can only be updated for an occupied bed with a patient"
            );
        }

        patient = new Patient(
            patient.id(),
            patient.name(),
            newAcuityScore,
            patient.gender(),
            patient.isolationType(),
            patient.infection()
        );
    }
}