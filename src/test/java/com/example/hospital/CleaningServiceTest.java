package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CleaningServiceTest {

    private static final LocalDateTime START =
        LocalDateTime.of(2026, 10, 25, 10, 0);

    private Bed occupiedBed(IsolationType isolationType) {
        Ward ward = new Ward(
            "W1", "General", WardType.GENERAL, 0, 100
        );
        Room room = new Room("R1", ward, false, false, true);
        Bed bed = new Bed("B1", room);

        Patient patient = new Patient(
            "P1",
            "Patient 1",
            new AcuityScore(50),
            "M",
            isolationType,
            isolationType == IsolationType.CONTACT ? "infection-A" : null
        );

        bed.reserve();
        bed.occupy(patient);
        return bed;
    }

    @Test
    void ordinaryDischargeCreatesStandardCleaningTask() {
        Bed bed = occupiedBed(IsolationType.NONE);
        CleaningService service = new CleaningService(30, 90);

        CleaningTask task = service.discharge(bed, START);

        assertEquals(CleaningType.STANDARD, task.type());
        assertEquals(30, task.durationMinutes());
        assertEquals(BedState.CLEANING, bed.state());
        assertNull(bed.patient());
    }

    @Test
    void airborneIsolationRequiresNinetyMinuteTerminalClean() {
        Bed bed = occupiedBed(IsolationType.AIRBORNE);
        CleaningService service = new CleaningService(30, 90);

        CleaningTask task = service.discharge(bed, START);

        assertEquals(CleaningType.TERMINAL, task.type());
        assertEquals(90, task.durationMinutes());
        assertEquals(BedState.CLEANING, bed.state());
    }

    @Test
    void contactIsolationAlsoRequiresTerminalClean() {
        Bed bed = occupiedBed(IsolationType.CONTACT);
        CleaningService service = new CleaningService(30, 90);

        CleaningTask task = service.discharge(bed, START);

        assertEquals(CleaningType.TERMINAL, task.type());
        assertEquals(90, task.durationMinutes());
    }

    @Test
    void bedCannotBecomeAvailableBeforeCleaningDurationElapses() {
        Bed bed = occupiedBed(IsolationType.AIRBORNE);
        CleaningTask task =
            new CleaningService(30, 90).discharge(bed, START);

        assertThrows(
            IllegalStateException.class,
            () -> task.complete(START.plusMinutes(89))
        );

        assertEquals(BedState.CLEANING, bed.state());
        assertFalse(task.isCompleted());
    }

    @Test
    void bedBecomesAvailableAfterTerminalCleaningCompletes() {
        Bed bed = occupiedBed(IsolationType.AIRBORNE);
        CleaningTask task =
            new CleaningService(30, 90).discharge(bed, START);

        LocalDateTime completed = START.plusMinutes(90);
        task.complete(completed);

        assertTrue(task.isCompleted());
        assertEquals(completed, task.completedAt());
        assertEquals(BedState.AVAILABLE, bed.state());
    }

    @Test
    void standardCleaningCompletesAfterItsConfiguredDuration() {
        Bed bed = occupiedBed(IsolationType.NONE);
        CleaningTask task =
            new CleaningService(30, 90).discharge(bed, START);

        task.complete(START.plusMinutes(30));

        assertEquals(BedState.AVAILABLE, bed.state());
        assertTrue(task.isCompleted());
    }

    @Test
    void rejectsTerminalDurationNotLongerThanStandard() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CleaningService(90, 90)
        );
    }

    @Test
    void rejectsDischargeFromAnUnoccupiedBed() {
        Ward ward = new Ward(
            "W1", "General", WardType.GENERAL, 0, 100
        );
        Bed bed = new Bed(
            "B1", new Room("R1", ward, false, false, true)
        );

        assertThrows(
            IllegalStateException.class,
            () -> new CleaningService(30, 90).discharge(bed, START)
        );

        assertEquals(BedState.AVAILABLE, bed.state());
    }
}