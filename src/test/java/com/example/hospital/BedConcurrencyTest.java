package com.example.hospital;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedConcurrencyTest {

    @Test
    void onlyOneCoordinatorCanReserveTheSameBed() throws Exception {
        Ward ward = new Ward(
            "W1", "General", WardType.GENERAL, 0, 100
        );
        Room room = new Room("R1", ward, false, false, true);
        Bed bed = new Bed("B1", room);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successfulReservations = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> {
                attemptReservation(bed, ready, start, successfulReservations);
            });
            Future<?> second = executor.submit(() -> {
                attemptReservation(bed, ready, start, successfulReservations);
            });

            ready.await();
            start.countDown();

            first.get();
            second.get();
        }

        assertEquals(1, successfulReservations.get());
        assertEquals(BedState.RESERVED, bed.state());
    }

    private void attemptReservation(
            Bed bed,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successfulReservations) {
        ready.countDown();

        try {
            start.await();
            bed.reserve();
            successfulReservations.incrementAndGet();
        } catch (IllegalStateException ignored) {
            // Another coordinator reserved the bed first.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }
}