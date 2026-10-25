package com.example.hospital;

import java.util.List;
import java.util.Objects;

public final class BedSwapService {

    public void swapPatients(Bed first, Bed second, List<Bed> allBeds) {
        Objects.requireNonNull(first, "First bed is required");
        Objects.requireNonNull(second, "Second bed is required");
        Objects.requireNonNull(allBeds, "Bed list is required");

        if (first == second) {
            throw new IllegalArgumentException("Cannot swap a bed with itself");
        }

        Bed lockFirst = first.id().compareTo(second.id()) <= 0 ? first : second;
        Bed lockSecond = lockFirst == first ? second : first;

        synchronized (lockFirst) {
            synchronized (lockSecond) {
                if (first.state() != BedState.OCCUPIED
                        || second.state() != BedState.OCCUPIED) {
                    throw new IllegalStateException("Both beds must be occupied");
                }

                Patient firstPatient = Objects.requireNonNull(
                    first.patient(), "First bed must have a patient"
                );
                Patient secondPatient = Objects.requireNonNull(
                    second.patient(), "Second bed must have a patient"
                );

                validatePlacement(secondPatient, first, second, allBeds);
                validatePlacement(firstPatient, second, first, allBeds);

                Bed.swapPatients(first, second);
            }
        }
    }

    private void validatePlacement(
            Patient patient, Bed destination, Bed otherSwapBed, List<Bed> allBeds) {
        Room room = destination.room();

        if (!room.ward().accepts(patient.acuityScore())) {
            throw new IllegalArgumentException(
                "Patient acuity is outside the destination ward range"
            );
        }

        if (patient.isolationType() == IsolationType.AIRBORNE
                && (!room.singleRoom() || !room.negativePressure())) {
            throw new IllegalArgumentException(
                "Airborne isolation requires a single negative-pressure room"
            );
        }

        for (Bed other : allBeds) {
            if (other == destination) {
                continue;
            }

            boolean isOtherSwapBed = other == otherSwapBed;

            if ((!isOtherSwapBed && other.state() != BedState.OCCUPIED)
                    || !other.room().id().equals(room.id())) {
                continue;
            }

            Patient occupant = isOtherSwapBed
                ? destination.patient()
                : other.patient();

            if (occupant == null) {
                throw new IllegalArgumentException(
                    "Cannot validate a room with an unknown occupant"
                );
            }

            if (room.singleRoom()) {
                throw new IllegalArgumentException(
                    "A single room cannot contain another occupied bed"
                );
            }

            if (patient.isolationType() == IsolationType.AIRBORNE
                    || occupant.isolationType() == IsolationType.AIRBORNE) {
                throw new IllegalArgumentException(
                    "Airborne isolation patients cannot share a room"
                );
            }

            if (patient.isolationType() == IsolationType.CONTACT
                    || occupant.isolationType() == IsolationType.CONTACT) {
                boolean sameInfection =
                    patient.isolationType() == IsolationType.CONTACT
                    && occupant.isolationType() == IsolationType.CONTACT
                    && Objects.equals(patient.infection(), occupant.infection());

                if (!sameInfection) {
                    throw new IllegalArgumentException(
                        "Contact precautions require matching infections"
                    );
                }
            }

            if (!room.mixedGenderBay()
                    && !patient.gender().equals(occupant.gender())) {
                throw new IllegalArgumentException(
                    "Mixed-gender sharing is not allowed in this room"
                );
            }
        }
    }
}