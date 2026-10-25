package com.example.hospital;

import java.util.Objects;

public record TransferReservation(
        TransferRequest request,
        Bed targetBed) {

    public TransferReservation {
        Objects.requireNonNull(request, "Transfer request is required");
        Objects.requireNonNull(targetBed, "Target bed is required");

        if (request.sourceBed() == targetBed) {
            throw new IllegalArgumentException(
                "Source and target beds must differ"
            );
        }
    }
}