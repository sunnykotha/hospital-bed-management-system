package com.example.hospital;

import java.util.Objects;

public record Ward(
        String id,
        String name,
        WardType type,
        int minimumAcuity,
        int maximumAcuity) {

    public Ward {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Ward id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ward name is required");
        }

        Objects.requireNonNull(type, "Ward type is required");

        if (minimumAcuity < 0 || maximumAcuity > 100
                || minimumAcuity > maximumAcuity) {
            throw new IllegalArgumentException(
                "Invalid ward acuity range"
            );
        }
    }

    public boolean accepts(AcuityScore acuityScore) {
        Objects.requireNonNull(acuityScore, "Acuity score is required");
        return acuityScore.value() >= minimumAcuity
                && acuityScore.value() <= maximumAcuity;
    }
}