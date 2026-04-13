package com.example.booking_service.integration;

import lombok.Data;

import java.util.UUID;

@Data
public class LockedSeatSnapshot {
    private UUID eventSeatId;
    private UUID sectionId;
    private Integer priceCents;
}
