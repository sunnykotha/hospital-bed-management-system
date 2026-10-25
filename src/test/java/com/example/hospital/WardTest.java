package com.example.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WardTest {

    private final Ward ward = new Ward(
        "W1", "Critical Care", WardType.ICU, 70, 100
    );

    @Test
    void acceptsAcuityWithinConfiguredRange() {
        assertTrue(ward.accepts(new AcuityScore(70)));
        assertTrue(ward.accepts(new AcuityScore(85)));
        assertTrue(ward.accepts(new AcuityScore(100)));
    }

    @Test
    void rejectsAcuityOutsideConfiguredRange() {
        assertFalse(ward.accepts(new AcuityScore(69)));
    }

    @Test
    void rejectsInvalidRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Ward("W2", "Invalid", WardType.GENERAL, 80, 20)
        );
    }
}