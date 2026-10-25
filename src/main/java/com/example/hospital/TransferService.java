package com.example.hospital;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class TransferService {
    private final BedMatcher bedMatcher;
    private final int stepDownThreshold;
    private final Map<String, TransferReservation> reservations =
        new ConcurrentHashMap<>();

    public TransferService(BedMatcher bedMatcher, int stepDownThreshold) {
        this.bedMatcher = Objects.requireNonNull(
            bedMatcher, "Bed matcher is required"
        );

        if (stepDownThreshold < 0 || stepDownThreshold > 100) {
            throw new IllegalArgumentException(
                "Step-down threshold must be between 0 and 100"
            );
        }

        this.stepDownThreshold = stepDownThreshold;
    }

    public boolean isEligibleForStepDown(TransferRequest request) {
        Objects.requireNonNull(request, "Transfer request is required");

        return request.sourceBed().room().ward().type() == WardType.ICU
            && request.sourceBed().state() == BedState.OCCUPIED
            && request.patient().equals(request.sourceBed().patient())
            && request.patient().acuityScore().value() < stepDownThreshold;
    }

    public synchronized TransferReservation reserveTarget(
            TransferRequest request, List<Bed> beds) {
        Objects.requireNonNull(request, "Transfer request is required");
        Objects.requireNonNull(beds, "Bed list is required");

        if (!isEligibleForStepDown(request)) {
            throw new IllegalStateException(
                "Patient is not eligible for ICU step-down"
            );
        }

        if (reservations.containsKey(request.id())) {
            throw new IllegalStateException(
                "Transfer already has a target reservation"
            );
        }

        List<Bed> candidates = bedMatcher.eligibleBeds(
            request.patient(), beds
        );

        for (Bed target : candidates) {
            if (target == request.sourceBed()
                    || target.room().ward().type() != WardType.STEP_DOWN) {
                continue;
            }

            try {
                target.reserve();

                TransferReservation reservation =
                    new TransferReservation(request, target);

                reservations.put(request.id(), reservation);
                return reservation;
            } catch (IllegalStateException exception) {
                // Another coordinator may have reserved this target.
            }
        }

        throw new IllegalStateException(
            "No eligible step-down bed is available"
        );
    }

    public synchronized void completeTransfer(String transferId) {
        TransferReservation reservation = reservations.get(transferId);

        if (reservation == null) {
            throw new IllegalStateException(
                "No active target reservation for transfer: " + transferId
            );
        }

        TransferRequest request = reservation.request();
        Bed source = request.sourceBed();
        Bed target = reservation.targetBed();

        // Revalidate before changing either bed.
        if (!isEligibleForStepDown(request)
                || target.state() != BedState.RESERVED) {
            throw new IllegalStateException(
                "Transfer is no longer valid"
            );
        }

        Bed.completeTransfer(source, target, request.patient());
        reservations.remove(transferId);
    }

    public synchronized void cancelTransfer(String transferId) {
        TransferReservation reservation = reservations.get(transferId);

        if (reservation == null) {
            throw new IllegalStateException(
                "No active target reservation for transfer: " + transferId
            );
        }

        reservation.targetBed().releaseReservation();
        reservations.remove(transferId);
    }
}