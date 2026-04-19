package com.example.booking_service.model;

import com.example.booking_service.model.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "booking_items")
public class BookingItem extends BaseEntity {
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "event_seat_id", nullable = false)
    private UUID eventSeatId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "price_minor", nullable = false)
    private Long priceMinor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
