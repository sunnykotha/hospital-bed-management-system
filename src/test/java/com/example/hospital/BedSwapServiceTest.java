package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BedSwapServiceTest {

    private Ward ward(int minimum, int maximum) {
        return new Ward("W1", "General", WardType.GENERAL, minimum, maximum);
    }

    private Patient patient(
            String id, int acuity, String gender, IsolationType isolation, String infection) {
        return new Patient(
            id, id, new AcuityScore(acuity), gender, isolation, infection
        );
    }

    private Bed occupied(
            String id, Room room, Patient patient) {
        Bed bed = new Bed(id, room);
        bed.reserve();
        bed.occupy(patient);
        return bed;
    }

    @Test
    void swapsPatientsWhenBothDestinationPlacementsAreValid() {
        Room room1 = new Room("R1", ward(0, 100), false, false, true);
        Room room2 = new Room("R2", ward(0, 100), false, false, true);
        Patient p1 = patient("P1", 40, "F", IsolationType.NONE, null);
        Patient p2 = patient("P2", 50, "M", IsolationType.NONE, null);
        Bed b1 = occupied("B1", room1, p1);
        Bed b2 = occupied("B2", room2, p2);

        new BedSwapService().swapPatients(b1, b2, List.of(b1, b2));

        assertSame(p2, b1.patient());
        assertSame(p1, b2.patient());
    }

    @Test
    void rejectsSwapWhenPatientAcuityDoesNotFitDestinationWard() {
        Room room1 = new Room("R1", ward(0, 30), false, false, true);
        Room room2 = new Room("R2", ward(0, 100), false, false, true);
        Patient p1 = patient("P1", 20, "F", IsolationType.NONE, null);
        Patient p2 = patient("P2", 80, "M", IsolationType.NONE, null);
        Bed b1 = occupied("B1", room1, p1);
        Bed b2 = occupied("B2", room2, p2);

        assertThrows(
            IllegalArgumentException.class,
            () -> new BedSwapService().swapPatients(b1, b2, List.of(b1, b2))
        );

        assertSame(p1, b1.patient());
        assertSame(p2, b2.patient());
    }

    @Test
    void rejectsAirbornePatientDestinationWithoutNegativePressure() {
        Room ordinary = new Room("R1", ward(0, 100), false, false, true);
        Room other = new Room("R2", ward(0, 100), false, false, true);
        Patient airborne = patient("P1", 40, "F", IsolationType.AIRBORNE, null);
        Patient ordinaryPatient = patient("P2", 40, "M", IsolationType.NONE, null);
        Bed b1 = occupied("B1", ordinary, airborne);
        Bed b2 = occupied("B2", other, ordinaryPatient);

        assertThrows(
            IllegalArgumentException.class,
            () -> new BedSwapService().swapPatients(b1, b2, List.of(b1, b2))
        );

        assertSame(airborne, b1.patient());
        assertSame(ordinaryPatient, b2.patient());
    }

    @Test
    void rejectsSwapThatWouldCreateMixedGenderSharedBay() {
        Ward ward = ward(0, 100);
        Room shared = new Room("R1", ward, false, false, false);
        Room other = new Room("R2", ward, false, false, true);

        Patient maleInShared = patient("P1", 40, "M", IsolationType.NONE, null);
        Patient femaleInOther = patient("P2", 40, "F", IsolationType.NONE, null);
        Patient maleRoommate = patient("P3", 40, "M", IsolationType.NONE, null);

        Bed first = occupied("B1", shared, maleInShared);
        Bed second = occupied("B2", other, femaleInOther);
        Bed roommate = occupied("B3", shared, maleRoommate);

        assertThrows(
            IllegalArgumentException.class,
            () -> new BedSwapService().swapPatients(
                first, second, List.of(first, second, roommate)
            )
        );

        assertSame(maleInShared, first.patient());
        assertSame(femaleInOther, second.patient());
    }
}