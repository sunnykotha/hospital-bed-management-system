package com.example.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedAcuityUpdateTest {

    private Bed occupiedIcuBed(Patient patient) {
        Ward icu = new Ward(
            "ICU1", "Intensive Care", WardType.ICU, 0, 100
        );
        Room room = new Room("R1", icu, false, false, true);
        Bed bed = new Bed("B1", room);
        bed.reserve();
        bed.occupy(patient);
        return bed;
    }

    @Test
    void acuityImprovementMakesPatientEligibleForStepDown() {
        Patient patient = new Patient(
            "P1", "Patient 1", new AcuityScore(60),
            "M", IsolationType.NONE, null
        );
        Bed source = occupiedIcuBed(patient);
        TransferService service = new TransferService(new BedMatcher(), 40);

        assertFalse(service.isEligibleForStepDown(
            new TransferRequest("T1", source.patient(), source)
        ));

        source.updatePatientAcuity(new AcuityScore(30));

        assertEquals(30, source.patient().acuityScore().value());
        assertTrue(service.isEligibleForStepDown(
            new TransferRequest("T2", source.patient(), source)
        ));
        assertEquals(BedState.OCCUPIED, source.state());
    }

    @Test
    void cannotUpdateAcuityWhenBedIsNotOccupied() {
        Ward ward = new Ward(
            "W1", "General", WardType.GENERAL, 0, 100
        );
        Bed bed = new Bed(
            "B1", new Room("R1", ward, false, false, true)
        );

        assertThrows(
            IllegalStateException.class,
            () -> bed.updatePatientAcuity(new AcuityScore(20))
        );

        assertEquals(BedState.AVAILABLE, bed.state());
        assertNull(bed.patient());
    }
}