package com.example.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedTest {

    private Bed createBed() {
        Ward ward = new Ward(
            "W1", "General Ward", WardType.GENERAL, 0, 60
        );
        Room room = new Room("R1", ward, false, false, false);
        return new Bed("B1", room);
    }

    @Test
    void startsAvailable() {
        assertEquals(BedState.AVAILABLE, createBed().state());
    }

    @Test
    void supportsReservationAndCancellation() {
        Bed bed = createBed();

        bed.reserve();
        assertEquals(BedState.RESERVED, bed.state());

        bed.releaseReservation();
        assertEquals(BedState.AVAILABLE, bed.state());
    }

    @Test
    void supportsOccupancyDischargeAndCleaning() {
        Bed bed = createBed();

        bed.reserve();
        bed.occupy();
        bed.markPendingDischarge();
        bed.startCleaning();
        bed.finishCleaning();

        assertEquals(BedState.AVAILABLE, bed.state());
    }

    @Test
    void supportsMaintenanceBlocking() {
        Bed bed = createBed();

        bed.block();
        assertEquals(BedState.BLOCKED, bed.state());

        bed.unblock();
        assertEquals(BedState.AVAILABLE, bed.state());
    }

    @Test
    void rejectsInvalidTransitionsWithoutChangingState() {
        Bed bed = createBed();

        assertThrows(IllegalStateException.class, bed::occupy);
        assertEquals(BedState.AVAILABLE, bed.state());

        bed.reserve();

        assertThrows(IllegalStateException.class, bed::startCleaning);
        assertEquals(BedState.RESERVED, bed.state());
    }
}