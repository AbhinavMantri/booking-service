package com.example.booking_service.repositories;

import com.example.booking_service.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findAllByUserIdOrderByBookedAtDesc(UUID userId);

    Optional<Booking> findByPaymentId(UUID paymentId);

    Optional<Booking> findByLockId(UUID lockId);
}
