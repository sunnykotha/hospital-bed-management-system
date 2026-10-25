package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdmissionServiceTest {

    private Patient patient(String id, int acuity) {
        return new Patient(
            id, id, new AcuityScore(acuity), "M"
        );
    }

    private Bed bed(String id, int min, int max) {
        Ward ward = new Ward(
            "W-" + id, "Ward " + id, WardType.GENERAL, min, max
        );
        Room room = new Room(
            "R-" + id, ward, false, false, true
        );
        return new Bed(id, room);
    }

    private AdmissionService service() {
        return new AdmissionService(new BedMatcher());
    }

    @Test
    void allocatesAndOccupiesAnEligibleBed() {
        Bed bed = bed("B1", 0, 100);
        AdmissionRequest request = new AdmissionRequest(
            "A1", patient("P1", 50), 5, true
        );

        AdmissionResult result = service().allocate(request, List.of(bed));

        assertEquals(AdmissionResult.Status.PLACED, result.status());
        assertSame(bed, result.bed());
        assertEquals(BedState.OCCUPIED, bed.state());
        assertEquals("P1", bed.patient().id());
        assertTrue(result.boarding().isEmpty());
        assertTrue(result.escalation().isEmpty());
    }

    @Test
    void boardsAndEscalatesWhenNoBedIsAvailable() {
        Bed bed = bed("B1", 0, 40);
        AdmissionRequest request = new AdmissionRequest(
            "A1", patient("P1", 80), 10, true
        );

        AdmissionResult result = service().allocate(request, List.of(bed));

        assertEquals(AdmissionResult.Status.BOARDING, result.status());
        assertNull(result.bed());
        assertTrue(result.boarding().isPresent());
        assertTrue(result.escalation().isPresent());
        assertEquals("A1", result.boarding().orElseThrow().requestId());
        assertEquals("A1", result.escalation().orElseThrow().requestId());
        assertEquals(BedState.AVAILABLE, bed.state());
    }

    @Test
    void skipsReservedBedsAndUsesAnotherEligibleBed() {
        Bed reserved = bed("B1", 0, 100);
        reserved.reserve();

        Bed available = bed("B2", 0, 100);
        AdmissionRequest request = new AdmissionRequest(
            "A1", patient("P1", 50), 5, true
        );

        AdmissionResult result = service().allocate(
            request, List.of(reserved, available)
        );

        assertEquals(AdmissionResult.Status.PLACED, result.status());
        assertSame(available, result.bed());
        assertEquals(BedState.RESERVED, reserved.state());
        assertEquals(BedState.OCCUPIED, available.state());
    }

    @Test
    void processesHigherUrgencyRequestsFirst() {
        Bed first = bed("B1", 0, 100);
        Bed second = bed("B2", 0, 100);

        AdmissionRequest low = new AdmissionRequest(
            "A1", patient("P1", 50), 1, true
        );
        AdmissionRequest high = new AdmissionRequest(
            "A2", patient("P2", 50), 10, true
        );

        List<AdmissionResult> results =
            service().allocateInPriorityOrder(
                List.of(low, high), List.of(first, second)
            );

        assertEquals("A2", results.get(0).requestId());
        assertEquals("A1", results.get(1).requestId());
        assertEquals("P2", first.patient().id());
        assertEquals("P1", second.patient().id());
    }

    @Test
    void rejectsNegativeUrgency() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AdmissionRequest(
                "A1", patient("P1", 50), -1, true
            )
        );
    }
}