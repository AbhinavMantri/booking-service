package com.example.booking_service.integration;

import com.example.booking_service.services.PaymentOutcomeAction;

import java.util.List;
import java.util.UUID;

public interface SeatAllocationGateway {
    void lockSeats(UUID eventId, UUID bookingId, UUID userId, String idempotencyKey, List<UUID> seatIds);

    LockSnapshot getLockDetails(UUID bookingId);

    LockSnapshot getLockDetailsOrNull(UUID bookingId);

    void confirmSeats(UUID eventId, UUID bookingId, UUID paymentId, List<UUID> seatIds, String idempotencyKey);

    void releaseSeats(UUID eventId, UUID bookingId, List<UUID> seatIds, PaymentOutcomeAction action, String idempotencyKey);
}
