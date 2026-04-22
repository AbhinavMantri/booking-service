package com.example.booking_service.dtos;

import java.util.UUID;

public record BookingSummary(
        UUID bookingId,
        UUID eventId,
        String status,
        String currency,
        Long totalAmountMinor
) {
}
