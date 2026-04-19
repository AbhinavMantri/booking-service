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
@Table(name = "booking_fulfillment_requests")
public class BookingFulfillmentRequest extends BaseEntity {
    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
