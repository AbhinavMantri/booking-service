package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.FinalizeBookingResponse;
import com.example.booking_service.dtos.FinalizeBookingTicketResponse;
import com.example.booking_service.dtos.TicketSummary;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.TicketRepository;
import com.example.booking_service.services.InternalBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/bookings")
public class InternalBookingController {
    private final InternalBookingService internalBookingService;
    private final TicketRepository ticketRepository;

    @PostMapping("/finalize")
    public ResponseEntity<FinalizeBookingResponse> finalizeBooking(@Valid @RequestBody BookingRequest request) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        log.info("requestId={} finalize booking request received paymentId={} lockId={} userId={} eventId={}",
                requestId, request.getPaymentId(), request.getLockId(), request.getUserId(), request.getEventId());

        FinalizeBookingResponse response = new FinalizeBookingResponse();
        try {
            Booking booking = internalBookingService.finalizeBooking(request);
            List<FinalizeBookingTicketResponse> tickets = ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(booking.getId()).stream()
                    .map(this::toFinalizeBookingTicketResponse)
                    .toList();
            response.setBookingId(booking.getId());
            response.setStatus(ResponseStatus.SUCCESS);
            response.setTickets(tickets);
            log.info("requestId={} finalize booking request completed bookingId={} tickets={}",
                    requestId, booking.getId(), tickets.size());
            return ResponseEntity.ok(response);
        } catch (BookingConflictException ex) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.warn("requestId={} finalize booking request conflicted reason={}", requestId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    private FinalizeBookingTicketResponse toFinalizeBookingTicketResponse(Ticket ticket) {
        
        FinalizeBookingTicketResponse response = new FinalizeBookingTicketResponse();
        response.setTicketSummary(TicketSummary.builder()
                .ticketId(ticket.getBookingId())
                .ticketNumber(ticket.getTicketNumber())
                .build());
        return response;
    }
}
