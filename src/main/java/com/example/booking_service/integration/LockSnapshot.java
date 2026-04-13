package com.example.booking_service.integration;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class LockSnapshot {
    private UUID bookingId;
    private UUID eventId;
    private List<LockedSeatSnapshot> seats;
    private Long totalAmountMinor;
    private String currency;
}
