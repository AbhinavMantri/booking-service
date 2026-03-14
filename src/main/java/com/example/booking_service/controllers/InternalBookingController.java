package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingDetailItemResponse;
import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.FinalizeBookingResponse;
import com.example.booking_service.dtos.FinalizeBookingTicketResponse;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.TicketRepository;
import com.example.booking_service.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/bookings/finalize")
public class InternalBookingController {
    private final BookingService bookingService;
    private final TicketRepository ticketRepository;

    @PostMapping
    public ResponseEntity<FinalizeBookingResponse> finalizeBooking(@Valid @RequestBody BookingRequest request) {
        FinalizeBookingResponse response = new FinalizeBookingResponse();
        try {
            Booking booking = bookingService.finalizeBooking(request);
            response.setBookingId(booking.getId());
            response.setStatus(ResponseStatus.SUCCESS);
            response.setTickets(ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(booking.getId()).stream()
                    .map(this::toFinalizeBookingTicketResponse)
                    .toList());
            return ResponseEntity.ok(response);
        } catch (BookingConflictException ex) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    private FinalizeBookingTicketResponse toFinalizeBookingTicketResponse(Ticket ticket) {
        return FinalizeBookingTicketResponse.builder()
                .ticketId(ticket.getBookingId())
                .ticketNumber(ticket.getTicketNumber())
                .build();
    }
}
