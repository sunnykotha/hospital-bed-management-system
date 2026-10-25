package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BedMatcherTest {

    private Ward ward() {
        return new Ward("W1", "General", WardType.GENERAL, 0, 100);
    }

    private Patient patient(
            String id, String gender, IsolationType isolation, String infection) {
        return new Patient(
            id, id, new AcuityScore(50), gender, isolation, infection
        );
    }

    private Bed bed(String id, Room room) {
        return new Bed(id, room);
    }

    @Test
    void rejectsPatientOutsideWardAcuityRange() {
        Ward limited = new Ward(
            "W2", "Step Down", WardType.STEP_DOWN, 20, 60
        );
        Room room = new Room("R1", limited, false, false, false);
        Bed bed = bed("B1", room);
        Patient patient = new Patient(
            "P1", "P1", new AcuityScore(10), "M",
            IsolationType.NONE, null
        );

        assertFalse(new BedMatcher().isEligible(patient, bed, List.of(bed)));
    }

    @Test
    void airbornePatientRequiresNegativePressureSingleRoom() {
        Bed ordinary = bed(
            "B1", new Room("R1", ward(), false, false, true)
        );
        Bed negativePressure = bed(
            "B2", new Room("R2", ward(), true, true, false)
        );
        Patient patient = patient("P1", "M", IsolationType.AIRBORNE, null);
        BedMatcher matcher = new BedMatcher();

        assertFalse(matcher.isEligible(patient, ordinary, List.of(ordinary)));
        assertTrue(matcher.isEligible(
            patient, negativePressure, List.of(negativePressure)
        ));
    }

    @Test
    void rejectsBedsThatAreNotAvailable() {
        Bed bed = bed("B1", new Room("R1", ward(), false, false, true));
        bed.reserve();

        Patient patient = patient("P1", "M", IsolationType.NONE, null);

        assertFalse(new BedMatcher().isEligible(patient, bed, List.of(bed)));
    }

    @Test
    void allowsContactPrecautionPatientsWithSameInfectionToShare() {
        Room room = new Room("R1", ward(), false, false, true);
        Bed occupied = bed("B1", room);
        Bed candidate = bed("B2", room);

        occupied.reserve();
        occupied.occupy(
            patient("P1", "M", IsolationType.CONTACT, "infection-A")
        );

        Patient incoming = patient(
            "P2", "F", IsolationType.CONTACT, "infection-A"
        );

        assertTrue(new BedMatcher().isEligible(
            incoming, candidate, List.of(occupied, candidate)
        ));
    }

    @Test
    void rejectsContactPrecautionPatientsWithDifferentInfections() {
        Room room = new Room("R1", ward(), false, false, true);
        Bed occupied = bed("B1", room);
        Bed candidate = bed("B2", room);

        occupied.reserve();
        occupied.occupy(
            patient("P1", "M", IsolationType.CONTACT, "infection-A")
        );

        Patient incoming = patient(
            "P2", "F", IsolationType.CONTACT, "infection-B"
        );

        assertFalse(new BedMatcher().isEligible(
            incoming, candidate, List.of(occupied, candidate)
        ));
    }

    @Test
    void rejectsMixedGenderInNonMixedSharedBay() {
        Room room = new Room("R1", ward(), false, false, false);
        Bed occupied = bed("B1", room);
        Bed candidate = bed("B2", room);

        occupied.reserve();
        occupied.occupy(patient("P1", "M", IsolationType.NONE, null));

        Patient incoming = patient("P2", "F", IsolationType.NONE, null);

        assertFalse(new BedMatcher().isEligible(
            incoming, candidate, List.of(occupied, candidate)
        ));
    }

    @Test
    void allowsMixedGenderInExplicitlyMixedBay() {
        Room room = new Room("R1", ward(), false, false, true);
        Bed occupied = bed("B1", room);
        Bed candidate = bed("B2", room);

        occupied.reserve();
        occupied.occupy(patient("P1", "M", IsolationType.NONE, null));

        Patient incoming = patient("P2", "F", IsolationType.NONE, null);

        assertTrue(new BedMatcher().isEligible(
            incoming, candidate, List.of(occupied, candidate)
        ));
    }

    @Test
    void eligibleBedsReturnsOnlyMatchingAvailableBeds() {
        Room room1 = new Room("R1", ward(), false, false, true);
        Room room2 = new Room("R2", ward(), false, false, true);
        Bed available = bed("B1", room1);
        Bed reserved = bed("B2", room2);
        reserved.reserve();

        Patient patient = patient("P1", "M", IsolationType.NONE, null);

        assertEquals(
            List.of(available),
            new BedMatcher().eligibleBeds(
                patient, List.of(available, reserved)
            )
        );
    }
}