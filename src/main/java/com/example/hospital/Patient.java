package com.example.hospital;

import java.util.Objects;

public record Patient(
        String id,
        String name,
        AcuityScore acuityScore,
        String gender,
        IsolationType isolationType,
        String infection) {

    public Patient {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Patient id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Patient name is required");
        }

        Objects.requireNonNull(acuityScore, "Acuity score is required");
        Objects.requireNonNull(isolationType, "Isolation type is required");

        if (gender == null || gender.isBlank()) {
            throw new IllegalArgumentException("Patient gender is required");
        }

        if (isolationType == IsolationType.CONTACT
                && (infection == null || infection.isBlank())) {
            throw new IllegalArgumentException(
                "Contact precautions require an infection identifier"
            );
        }
    }

    public Patient(
            String id,
            String name,
            AcuityScore acuityScore,
            String gender) {
        this(id, name, acuityScore, gender, IsolationType.NONE, null);
    }
}