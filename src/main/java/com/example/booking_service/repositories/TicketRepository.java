package com.example.booking_service.repositories;

import com.example.booking_service.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findAllByBookingIdOrderByIssuedAtAsc(UUID bookingId);

    Optional<Ticket> findByTicketCode(String ticketCode);
}
