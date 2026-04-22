package com.example.booking_service.repositories;

import com.example.booking_service.model.BookingFulfillmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingFulfillmentRequestRepository extends JpaRepository<BookingFulfillmentRequest, UUID> {
    Optional<BookingFulfillmentRequest> findByPaymentId(UUID paymentId);
}
