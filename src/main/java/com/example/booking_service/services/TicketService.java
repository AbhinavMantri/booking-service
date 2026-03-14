package com.example.booking_service.services;

import com.example.booking_service.dtos.ScanTicketRequest;
import com.example.booking_service.dtos.ScanTicketResponse;
import com.example.booking_service.dtos.TicketResponse;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsForBooking(UUID bookingId) {
        return ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId) {
        return toResponse(getExistingTicket(ticketId));
    }

    @Transactional
    public ScanTicketResponse scanTicket(ScanTicketRequest request) {
        Ticket ticket = ticketRepository.findByTicketCode(request.ticketCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.setStatus(TicketStatus.USED);
        ticket.setCheckedInAt(OffsetDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);
        return new ScanTicketResponse(savedTicket.getId(), savedTicket.getStatus());
    }

    private Ticket getExistingTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTicketNumber(), ticket.getTicketCode(), ticket.getStatus());
    }
}
