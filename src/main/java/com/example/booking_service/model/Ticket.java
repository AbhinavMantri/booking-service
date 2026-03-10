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
@Table(name = "tickets")
public class Ticket extends BaseEntity {
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "booking_item_id", nullable = false, unique = true)
    private UUID bookingItemId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 64)
    private String ticketNumber;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 128)
    private String ticketCode;

    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;
}
