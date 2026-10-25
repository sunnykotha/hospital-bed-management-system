package com.example.hospital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdmissionPriorityRequirementTest {

    @Test
    void emergencyDepartmentAdmissionsAreOrderedByDescendingUrgency() {
        AdmissionService service = new AdmissionService(new BedMatcher());

        Patient low = new Patient("P1", "Patient 1", new AcuityScore(30), "M");
        Patient high = new Patient("P2", "Patient 2", new AcuityScore(60), "M");
        Patient medium = new Patient("P3", "Patient 3", new AcuityScore(45), "M");

        List<AdmissionRequest> requests = List.of(
                new AdmissionRequest("A1", low, 2, true),
                new AdmissionRequest("A2", high, 9, true),
                new AdmissionRequest("A3", medium, 5, true)
        );

        List<AdmissionResult> results =
                service.allocateInPriorityOrder(requests, List.of());

        assertEquals(
                List.of("A2", "A3", "A1"),
                results.stream().map(AdmissionResult::requestId).toList()
        );
    }
}