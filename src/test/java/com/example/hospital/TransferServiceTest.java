package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private Patient patient(int acuity) {
        return new Patient("P1", "Patient 1", new AcuityScore(acuity), "M");
    }

    private Bed bed(String id, WardType type, int min, int max) {
        Ward ward = new Ward(
            "W-" + id, "Ward " + id, type, min, max
        );
        Room room = new Room(
            "R-" + id, ward, false, false, true
        );
        return new Bed(id, room);
    }

    private TransferRequest occupiedIcuRequest(
            Patient patient, Bed source) {
        source.reserve();
        source.occupy(patient);
        return new TransferRequest("T1", patient, source);
    }

    private TransferService service() {
        return new TransferService(new BedMatcher(), 40);
    }

    @Test
    void patientBelowThresholdIsEligibleForStepDown() {
        Patient patient = patient(20);
        Bed source = bed("ICU1", WardType.ICU, 0, 100);
        TransferRequest request = occupiedIcuRequest(patient, source);

        assertTrue(service().isEligibleForStepDown(request));
    }

    @Test
    void patientAtThresholdIsNotEligible() {
        Patient patient = patient(40);
        Bed source = bed("ICU1", WardType.ICU, 0, 100);
        TransferRequest request = occupiedIcuRequest(patient, source);

        assertFalse(service().isEligibleForStepDown(request));
    }

    @Test
    void reservesTargetBeforeReleasingSource() {
        Patient patient = patient(20);
        Bed source = bed("ICU1", WardType.ICU, 0, 100);
        Bed target = bed("SD1", WardType.STEP_DOWN, 0, 39);
        TransferRequest request = occupiedIcuRequest(patient, source);
        TransferService service = service();

        TransferReservation reservation = service.reserveTarget(
            request, List.of(source, target)
        );

        assertSame(target, reservation.targetBed());
        assertEquals(BedState.OCCUPIED, source.state());
        assertEquals(BedState.RESERVED, target.state());

        service.completeTransfer(request.id());

        assertEquals(BedState.CLEANING, source.state());
        assertNull(source.patient());
        assertEquals(BedState.OCCUPIED, target.state());
        assertSame(patient, target.patient());
    }

    @Test
    void cancellingTransferReleasesReservedTargetAndKeepsSource() {
        Patient patient = patient(20);
        Bed source = bed("ICU1", WardType.ICU, 0, 100);
        Bed target = bed("SD1", WardType.STEP_DOWN, 0, 39);
        TransferRequest request = occupiedIcuRequest(patient, source);
        TransferService service = service();

        service.reserveTarget(request, List.of(source, target));
        service.cancelTransfer(request.id());

        assertEquals(BedState.OCCUPIED, source.state());
        assertSame(patient, source.patient());
        assertEquals(BedState.AVAILABLE, target.state());
        assertNull(target.patient());
    }

    @Test
    void rejectsTransferWhenNoStepDownBedIsAvailable() {
        Patient patient = patient(20);
        Bed source = bed("ICU1", WardType.ICU, 0, 100);
        Bed target = bed("SD1", WardType.STEP_DOWN, 0, 39);
        target.reserve();

        TransferRequest request = occupiedIcuRequest(patient, source);

        assertThrows(
            IllegalStateException.class,
            () -> service().reserveTarget(request, List.of(source, target))
        );

        assertEquals(BedState.OCCUPIED, source.state());
        assertSame(patient, source.patient());
        assertEquals(BedState.RESERVED, target.state());
    }

    @Test
    void rejectsTransferFromNonIcuWard() {
        Patient patient = patient(20);
        Bed source = bed("G1", WardType.GENERAL, 0, 100);
        TransferRequest request = occupiedIcuRequest(patient, source);
        Bed target = bed("SD1", WardType.STEP_DOWN, 0, 39);

        assertThrows(
            IllegalStateException.class,
            () -> service().reserveTarget(request, List.of(source, target))
        );
    }
    @org.junit.jupiter.api.Test
    void invalidatedSourceKeepsTargetReservationUntilCancelled() {
        Ward icu = new Ward("W-ICU", "ICU", WardType.ICU, 0, 100);
        Ward stepDown = new Ward("W-SD", "Step Down", WardType.STEP_DOWN, 0, 100);
        Bed source = new Bed("B-ICU", new Room("R-ICU", icu, true, true, false));
        Bed target = new Bed("B-SD", new Room("R-SD", stepDown, false, false, true));
        Patient patient = new Patient("P-TRANSFER", "Transfer Patient",
            new AcuityScore(30), "M", IsolationType.NONE, null);

        source.reserve();
        source.occupy(patient);

        TransferRequest request = new TransferRequest("T-INVALIDATED", patient, source);
        TransferService service = new TransferService(new BedMatcher(), 60);
        service.reserveTarget(request, java.util.List.of(source, target));

        source.markPendingDischarge();

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> service.completeTransfer("T-INVALIDATED")
        );

        org.junit.jupiter.api.Assertions.assertEquals(BedState.PENDING_DISCHARGE, source.state());
        org.junit.jupiter.api.Assertions.assertSame(patient, source.patient());
        org.junit.jupiter.api.Assertions.assertEquals(BedState.RESERVED, target.state());
        org.junit.jupiter.api.Assertions.assertNull(target.patient());

        service.cancelTransfer("T-INVALIDATED");

        org.junit.jupiter.api.Assertions.assertEquals(BedState.AVAILABLE, target.state());
        org.junit.jupiter.api.Assertions.assertSame(patient, source.patient());
    }
}