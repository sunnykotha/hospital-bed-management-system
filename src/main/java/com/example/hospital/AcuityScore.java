package com.example.hospital;

public record AcuityScore(int value) {
    public AcuityScore {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(
                "Acuity score must be between 0 and 100"
            );
        }
    }
}