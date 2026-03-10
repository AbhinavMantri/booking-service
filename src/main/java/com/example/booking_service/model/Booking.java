package com.example.booking_service.model;

import com.example.booking_service.model.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "lock_id", nullable = false)
    private UUID lockId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "booked_at", nullable = false)
    private OffsetDateTime bookedAt;
}
