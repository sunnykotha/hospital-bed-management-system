package com.example.hospital;

import java.util.Objects;

public record Room(
        String id,
        Ward ward,
        boolean singleRoom,
        boolean negativePressure,
        boolean mixedGenderBay) {

    public Room {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Room id is required");
        }
        Objects.requireNonNull(ward, "Ward is required");

        if (negativePressure && !singleRoom) {
            throw new IllegalArgumentException(
                "A negative-pressure room must be a single room"
            );
        }
    }
}