package com.example.hospital;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AdmissionService {
    private final BedMatcher bedMatcher;

    public AdmissionService(BedMatcher bedMatcher) {
        this.bedMatcher = Objects.requireNonNull(
            bedMatcher, "Bed matcher is required"
        );
    }

    public AdmissionResult allocate(
            AdmissionRequest request, List<Bed> beds) {
        Objects.requireNonNull(request, "Admission request is required");
        Objects.requireNonNull(beds, "Bed list is required");

        List<Bed> candidates = bedMatcher.eligibleBeds(
            request.patient(), beds
        );

        // Prefer the ward whose acuity range most closely fits the patient.
        candidates = candidates.stream()
            .sorted(Comparator
                .comparingInt((Bed bed) ->
                    bed.room().ward().maximumAcuity()
                    - bed.room().ward().minimumAcuity())
                .thenComparing(Bed::id))
            .toList();

        for (Bed candidate : candidates) {
            try {
                candidate.reserve();
                candidate.occupy(request.patient());

                return new AdmissionResult(
                    request.id(),
                    AdmissionResult.Status.PLACED,
                    candidate,
                    null,
                    null
                );
            } catch (IllegalStateException exception) {
                // Another coordinator may have reserved this bed first.
                // Try the next eligible candidate.
            }
        }

        LocalDateTime now = LocalDateTime.now();

        BoardingRecord boarding = new BoardingRecord(
            request.id(), request.patient(), now
        );

        EscalationRecord escalation = new EscalationRecord(
            request.id(),
            "No eligible bed is currently available",
            now
        );

        return new AdmissionResult(
            request.id(),
            AdmissionResult.Status.BOARDING,
            null,
            boarding,
            escalation
        );
    }

    public List<AdmissionResult> allocateInPriorityOrder(
            List<AdmissionRequest> requests, List<Bed> beds) {
        Objects.requireNonNull(requests, "Admission requests are required");
        Objects.requireNonNull(beds, "Bed list is required");

        List<AdmissionRequest> ordered = new ArrayList<>(requests);

        ordered.sort(
            Comparator.comparingInt(AdmissionRequest::urgency).reversed()
                .thenComparing(
                    AdmissionRequest::fromEmergencyDepartment,
                    Comparator.reverseOrder()
                )
                .thenComparing(AdmissionRequest::id)
        );

        return ordered.stream()
            .map(request -> allocate(request, beds))
            .toList();
    }
}