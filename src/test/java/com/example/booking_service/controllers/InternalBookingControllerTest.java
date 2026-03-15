package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.FinalizeBookingResponse;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.TicketRepository;
import com.example.booking_service.services.InternalBookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalBookingControllerTest {
    @Mock
    private InternalBookingService internalBookingService;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private InternalBookingController internalBookingController;

    @Test
    void finalizeBookingReturnsSuccessResponse() {
        BookingRequest request = new BookingRequest();
        request.setPaymentId(UUID.randomUUID());
        request.setLockId(UUID.randomUUID());
        request.setUserId(UUID.randomUUID());
        request.setEventId(UUID.randomUUID());

        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);

        Ticket ticket = new Ticket();
        ticket.setBookingId(bookingId);
        ticket.setTicketNumber("TKT-2026-ABC12345");
        ticket.setIssuedAt(OffsetDateTime.now());

        when(internalBookingService.finalizeBooking(request)).thenReturn(booking);
        when(ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(bookingId)).thenReturn(List.of(ticket));

        ResponseEntity<FinalizeBookingResponse> response = internalBookingController.finalizeBooking(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().getBookingId()).isEqualTo(bookingId);
        assertThat(response.getBody().getTickets()).hasSize(1);
    }

    @Test
    void finalizeBookingReturnsConflictResponse() {
        BookingRequest request = new BookingRequest();
        request.setPaymentId(UUID.randomUUID());
        request.setLockId(UUID.randomUUID());

        when(internalBookingService.finalizeBooking(request))
                .thenThrow(new BookingConflictException("Lock already exists for a non-confirmed booking"));

        ResponseEntity<FinalizeBookingResponse> response = internalBookingController.finalizeBooking(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.FAILURE);
        assertThat(response.getBody().getMessage()).isEqualTo("Lock already exists for a non-confirmed booking");
    }
}
