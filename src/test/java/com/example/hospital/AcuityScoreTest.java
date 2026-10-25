package com.example.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcuityScoreTest {

    @Test
    void acceptsBoundaryScores() {
        assertEquals(0, new AcuityScore(0).value());
        assertEquals(100, new AcuityScore(100).value());
    }

    @Test
    void rejectsScoresOutsideRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AcuityScore(-1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AcuityScore(101)
        );
    }
}