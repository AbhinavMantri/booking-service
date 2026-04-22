package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingItemRepository bookingItemRepository;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MDC.put("requestId", "booking-service-test");
    }

    @Test
    void getBookingsReturnsMappedSummaries() {
        UUID userId = UUID.randomUUID();
        Booking booking = new Booking();
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        booking.setId(bookingId);
        booking.setEventId(eventId);
        booking.setStatus("CONFIRMED");
        booking.setCurrency("USD");
        booking.setTotalAmountMinor(2500L);

        when(bookingRepository.findAllByUserIdOrderByBookedAtDesc(userId)).thenReturn(List.of(booking));

        List<BookingSummary> result = bookingService.getBookings(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().bookingId()).isEqualTo(bookingId);
        assertThat(result.getFirst().eventId()).isEqualTo(eventId);
        assertThat(result.getFirst().status()).isEqualTo("CONFIRMED");
        assertThat(result.getFirst().currency()).isEqualTo("USD");
        assertThat(result.getFirst().totalAmountMinor()).isEqualTo(2500L);
    }

    @Test
    void getBookingReturnsBookingWhenFound() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        Booking result = bookingService.getBooking(bookingId);

        assertThat(result).isSameAs(booking);
    }

    @Test
    void getBookingThrowsWhenMissing() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking not found: " + bookingId);
    }

    @Test
    void getBookingItemsReturnsItems() {
        UUID bookingId = UUID.randomUUID();
        BookingItem item = new BookingItem();
        item.setId(UUID.randomUUID());
        item.setBookingId(bookingId);

        when(bookingItemRepository.findAllByBookingIdOrderByIdAsc(bookingId)).thenReturn(List.of(item));

        List<BookingItem> result = bookingService.getBookingItems(bookingId);

        assertThat(result).containsExactly(item);
    }

    @Test
    void getTicketsForBookingReturnsTicketsWhenBookingExists() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());

        when(bookingRepository.existsById(bookingId)).thenReturn(true);
        when(ticketService.getTicketsForBooking(bookingId)).thenReturn(List.of(ticket));

        List<Ticket> result = bookingService.getTicketsForBooking(bookingId);

        assertThat(result).containsExactly(ticket);
        verify(ticketService).getTicketsForBooking(bookingId);
    }

    @Test
    void getTicketsForBookingThrowsWhenBookingMissing() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.existsById(bookingId)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.getTicketsForBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking not found: " + bookingId);
    }
}
