package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class AdmissionConcurrencyTest {

    @Test
    void simultaneousAdmissionsCannotAssignTheSameBedTwice()
            throws Exception {
        Ward ward = new Ward(
            "W1", "General", WardType.GENERAL, 0, 100
        );
        Room room = new Room("R1", ward, false, false, true);
        Bed bed = new Bed("B1", room);

        Patient firstPatient = new Patient(
            "P1", "Patient 1", new AcuityScore(40),
            "M", IsolationType.NONE, null
        );
        Patient secondPatient = new Patient(
            "P2", "Patient 2", new AcuityScore(50),
            "M", IsolationType.NONE, null
        );

        AdmissionRequest firstRequest =
            new AdmissionRequest("A1", firstPatient, 5, true);
        AdmissionRequest secondRequest =
            new AdmissionRequest("A2", secondPatient, 5, true);

        AdmissionService service = new AdmissionService(new BedMatcher());
        List<Bed> beds = List.of(bed);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<AdmissionResult> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.allocate(firstRequest, beds);
            });

            Future<AdmissionResult> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.allocate(secondRequest, beds);
            });

            ready.await();
            start.countDown();

            AdmissionResult firstResult = first.get();
            AdmissionResult secondResult = second.get();

            long placedCount = List.of(firstResult, secondResult).stream()
                .filter(result -> result.status() == AdmissionResult.Status.PLACED)
                .count();

            long boardingCount = List.of(firstResult, secondResult).stream()
                .filter(result -> result.status() == AdmissionResult.Status.BOARDING)
                .count();

            assertEquals(1, placedCount);
            assertEquals(1, boardingCount);
            assertNotNull(bed.patient());
            assertTrue(
                bed.patient().equals(firstPatient)
                    || bed.patient().equals(secondPatient)
            );

            AdmissionResult boarding = firstResult.status()
                    == AdmissionResult.Status.BOARDING
                ? firstResult : secondResult;

            assertNotNull(boarding.boardingRecord());
            assertNotNull(boarding.escalationRecord());
            assertEquals(BedState.OCCUPIED, bed.state());
        }
    }
}