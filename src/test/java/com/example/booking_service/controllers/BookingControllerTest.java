package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingDetailResponse;
import com.example.booking_service.dtos.BookingResponse;
import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.dtos.BookingTicketResponse;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.logging.PublicApiAuthenticationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.services.BookingService;
import com.example.booking_service.services.CheckoutService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {
    @Mock
    private BookingService bookingService;

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private BookingController bookingController;

    @Test
    void getBookingsReturnsSuccessResponse() {
        UUID userId = UUID.randomUUID();
        BookingSummary summary = new BookingSummary(UUID.randomUUID(), UUID.randomUUID(), "CONFIRMED", "USD", 1500L);

        when(request.getAttribute(PublicApiAuthenticationFilter.AUTHENTICATED_USER_ID)).thenReturn(userId.toString());
        when(bookingService.getBookings(userId)).thenReturn(List.of(summary));

        ResponseEntity<BookingResponse> response = bookingController.getBookings(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().getMessage()).isEqualTo("Bookings retrieved successfully");
        assertThat(response.getBody().getBookings()).containsExactly(summary);
    }

    @Test
    void getBookingReturnsDetailResponse() {
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID eventSeatId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setEventId(eventId);
        booking.setStatus("CONFIRMED");

        BookingItem item = new BookingItem();
        item.setId(itemId);
        item.setEventSeatId(eventSeatId);
        item.setSectionId(sectionId);
        item.setPriceMinor(2200L);

        when(bookingService.getBooking(bookingId)).thenReturn(booking);
        when(bookingService.getBookingItems(bookingId)).thenReturn(List.of(item));

        ResponseEntity<BookingDetailResponse> response = bookingController.getBooking(bookingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBookingId()).isEqualTo(bookingId);
        assertThat(response.getBody().getEventId()).isEqualTo(eventId);
        assertThat(response.getBody().getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getBody().getItems()).hasSize(1);
        assertThat(response.getBody().getItems().getFirst().getBookingItemId()).isEqualTo(itemId);
    }

    @Test
    void getBookingReturnsNotFoundWhenMissing() {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.getBooking(bookingId)).thenThrow(new BookingNotFoundException(bookingId));

        ResponseEntity<BookingDetailResponse> response = bookingController.getBooking(bookingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getTicketsForBookingReturnsSuccessResponse() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTicketCode("CODE-1");
        ticket.setTicketNumber("TKT-2026-TEST");
        ticket.setStatus(TicketStatus.ISSUED);

        when(bookingService.getTicketsForBooking(bookingId)).thenReturn(List.of(ticket));

        ResponseEntity<BookingTicketResponse> response = bookingController.getTicketsForBooking(bookingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().getTickets()).hasSize(1);
        assertThat(response.getBody().getTickets().getFirst().getTicketId()).isEqualTo(ticket.getId());
    }

    @Test
    void getTicketsForBookingReturnsNotFoundResponse() {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.getTicketsForBooking(bookingId)).thenThrow(new BookingNotFoundException(bookingId));

        ResponseEntity<BookingTicketResponse> response = bookingController.getTicketsForBooking(bookingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.FAILURE);
        assertThat(response.getBody().getMessage()).isEqualTo("Booking not found: " + bookingId);
    }
}
