package com.example.booking_service.repositories;

import com.example.booking_service.model.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
    List<BookingItem> findAllByBookingIdOrderByIdAsc(UUID bookingId);
}
