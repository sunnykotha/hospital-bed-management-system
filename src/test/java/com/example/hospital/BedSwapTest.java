package com.example.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedSwapTest {

    private Ward ward() {
        return new Ward("W1", "General", WardType.GENERAL, 0, 100);
    }

    private Patient patient(String id, String gender) {
        return new Patient(
            id, id, new AcuityScore(50), gender, IsolationType.NONE, null
        );
    }

    private Bed occupiedBed(String id, Patient patient) {
        Bed bed = new Bed(
            id, new Room(id + "-R", ward(), false, false, true)
        );
        bed.reserve();
        bed.occupy(patient);
        return bed;
    }

    @Test
    void swapsBothPatientsWithoutChangingBedStates() {
        Patient alice = patient("P1", "F");
        Patient bob = patient("P2", "M");
        Bed first = occupiedBed("B1", alice);
        Bed second = occupiedBed("B2", bob);

        Bed.swapPatients(first, second);

        assertSame(bob, first.patient());
        assertSame(alice, second.patient());
        assertEquals(BedState.OCCUPIED, first.state());
        assertEquals(BedState.OCCUPIED, second.state());
    }

    @Test
    void rejectsSwapIfEitherBedIsNotOccupied() {
        Patient alice = patient("P1", "F");
        Bed occupied = occupiedBed("B1", alice);
        Bed available = new Bed(
            "B2", new Room("R2", ward(), false, false, true)
        );

        assertThrows(
            IllegalStateException.class,
            () -> Bed.swapPatients(occupied, available)
        );

        assertSame(alice, occupied.patient());
        assertEquals(BedState.OCCUPIED, occupied.state());
        assertEquals(BedState.AVAILABLE, available.state());
    }

    @Test
    void rejectsSwappingBedWithItselfWithoutChangingPatient() {
        Patient alice = patient("P1", "F");
        Bed bed = occupiedBed("B1", alice);

        assertThrows(
            IllegalArgumentException.class,
            () -> Bed.swapPatients(bed, bed)
        );

        assertSame(alice, bed.patient());
        assertEquals(BedState.OCCUPIED, bed.state());
    }
}