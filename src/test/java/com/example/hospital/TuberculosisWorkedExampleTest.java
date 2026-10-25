package com.example.hospital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TuberculosisWorkedExampleTest {

    @Test
    void suspectedTbPatientUsesNegativePressureRoomAndNinetyMinuteTerminalCleaning() {
        Ward ward = new Ward("W1", "General", WardType.GENERAL, 0, 100);
        Room room = new Room("4B", ward, true, true, false);
        Bed bed = new Bed("B4B", room);

        Patient patient = new Patient(
                "P-TB", "Suspected TB Patient", new AcuityScore(50),
                "M", IsolationType.AIRBORNE, null
        );

        AdmissionService service = new AdmissionService(new BedMatcher());
        service.allocate(
                new AdmissionRequest("ED-TB-1", patient, 10, true),
                List.of(bed)
        );

        assertEquals(BedState.OCCUPIED, bed.state());
        assertEquals(patient, bed.patient());
        assertEquals("4B", bed.room().id());
        assertEquals(true, bed.room().singleRoom());
        assertEquals(true, bed.room().negativePressure());

        LocalDateTime dischargeTime = LocalDateTime.of(2026, 10, 25, 10, 0);
        CleaningService cleaningService = new CleaningService(30, 90);
        CleaningTask task = cleaningService.discharge(bed, dischargeTime);

        assertEquals(CleaningType.TERMINAL, task.type());
        assertEquals(90, task.durationMinutes());
        assertEquals(BedState.CLEANING, bed.state());

        assertThrows(
                IllegalStateException.class,
                () -> task.complete(dischargeTime.plusMinutes(89))
        );

        task.complete(dischargeTime.plusMinutes(90));
        assertEquals(BedState.AVAILABLE, bed.state());
    }
}