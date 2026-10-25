package com.example.hospital;

import java.util.List;
import java.util.Objects;

public final class BedMatcher {

    public boolean isEligible(Patient patient, Bed candidate, List<Bed> allBeds) {
        Objects.requireNonNull(patient, "Patient is required");
        Objects.requireNonNull(candidate, "Candidate bed is required");
        Objects.requireNonNull(allBeds, "Bed list is required");

        if (candidate.state() != BedState.AVAILABLE) {
            return false;
        }

        Room room = candidate.room();

        if (!room.ward().accepts(patient.acuityScore())) {
            return false;
        }

        if (patient.isolationType() == IsolationType.AIRBORNE
                && (!room.singleRoom() || !room.negativePressure())) {
            return false;
        }

        for (Bed other : allBeds) {
            if (other == candidate || !other.room().equals(room)) {
                continue;
            }

            if (other.state() == BedState.AVAILABLE
                    || other.state() == BedState.BLOCKED
                    || other.state() == BedState.CLEANING) {
                continue;
            }

            Patient occupant = other.patient();

            // Unknown occupants or reservations cannot be safely cohorted.
            if (occupant == null) {
                return false;
            }

            if (patient.isolationType() == IsolationType.AIRBORNE
                    || occupant.isolationType() == IsolationType.AIRBORNE) {
                return false;
            }

            if (patient.isolationType() == IsolationType.CONTACT
                    || occupant.isolationType() == IsolationType.CONTACT) {
                if (patient.isolationType() != IsolationType.CONTACT
                        || occupant.isolationType() != IsolationType.CONTACT
                        || !Objects.equals(
                            patient.infection(), occupant.infection())) {
                    return false;
                }
            }

            if (!room.mixedGenderBay()
                    && !patient.gender().equalsIgnoreCase(occupant.gender())) {
                return false;
            }
        }

        return true;
    }

    public List<Bed> eligibleBeds(
            Patient patient, List<Bed> beds) {
        Objects.requireNonNull(beds, "Bed list is required");

        return beds.stream()
                .filter(bed -> isEligible(patient, bed, beds))
                .toList();
    }
}