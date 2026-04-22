package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.BookingSeatRequest;
import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingFulfillmentRequest;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.BookingStatus;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.BookingFulfillmentRequestRepository;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import com.example.booking_service.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalBookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingItemRepository bookingItemRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingFulfillmentRequestRepository fulfillmentRequestRepository;

    @InjectMocks
    private InternalBookingService internalBookingService;

    @BeforeEach
    void setUp() {
        MDC.put("requestId", "service-test-request");
    }

    @Test
    void finalizeBookingCreatesBookingItemsAndTickets() {
        BookingRequest request = bookingRequest();
        Booking savedBooking = new Booking();
        UUID bookingId = UUID.randomUUID();
        savedBooking.setId(bookingId);
        savedBooking.setUserId(request.getUserId());
        savedBooking.setEventId(request.getEventId());

        BookingItem savedItem = new BookingItem();
        savedItem.setId(UUID.randomUUID());

        when(bookingRepository.findByPaymentId(request.getPaymentId())).thenReturn(Optional.empty());
        when(bookingRepository.findByLockId(request.getLockId())).thenReturn(Optional.empty());
        when(fulfillmentRequestRepository.findByPaymentId(request.getPaymentId())).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingItemRepository.save(any(BookingItem.class))).thenReturn(savedItem);

        Booking result = internalBookingService.finalizeBooking(request);

        assertThat(result).isSameAs(savedBooking);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking persistedBooking = bookingCaptor.getValue();
        assertThat(persistedBooking.getPaymentId()).isEqualTo(request.getPaymentId());
        assertThat(persistedBooking.getLockId()).isEqualTo(request.getLockId());
        assertThat(persistedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED.name());
        assertThat(persistedBooking.getBookedAt()).isNotNull();

        verify(bookingItemRepository).save(any(BookingItem.class));
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket ticket = ticketCaptor.getValue();
        assertThat(ticket.getBookingId()).isEqualTo(bookingId);
        assertThat(ticket.getEventId()).isEqualTo(request.getEventId());
        assertThat(ticket.getUserId()).isEqualTo(request.getUserId());
        assertThat(ticket.getIssuedAt()).isNotNull();
        assertThat(ticket.getTicketNumber()).startsWith("TKT-");
        assertThat(ticket.getTicketCode()).isNotBlank();

        ArgumentCaptor<BookingFulfillmentRequest> fulfillmentCaptor = ArgumentCaptor.forClass(BookingFulfillmentRequest.class);
        verify(fulfillmentRequestRepository).save(fulfillmentCaptor.capture());
        BookingFulfillmentRequest fulfillmentRequest = fulfillmentCaptor.getValue();
        assertThat(fulfillmentRequest.getPaymentId()).isEqualTo(request.getPaymentId());
        assertThat(fulfillmentRequest.getBookingId()).isEqualTo(bookingId);
        assertThat(fulfillmentRequest.getRequestHash()).hasSize(64);
    }

    @Test
    void finalizeBookingReturnsExistingConfirmedBookingForPayment() {
        BookingRequest request = bookingRequest();
        Booking existingBooking = new Booking();
        existingBooking.setId(UUID.randomUUID());
        existingBooking.setStatus(BookingStatus.CONFIRMED.name());

        when(bookingRepository.findByPaymentId(request.getPaymentId())).thenReturn(Optional.of(existingBooking));
        when(fulfillmentRequestRepository.findByPaymentId(request.getPaymentId())).thenReturn(Optional.empty());

        Booking result = internalBookingService.finalizeBooking(request);

        assertThat(result).isSameAs(existingBooking);
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingItemRepository, never()).save(any(BookingItem.class));
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(fulfillmentRequestRepository).save(any(BookingFulfillmentRequest.class));
    }

    @Test
    void finalizeBookingThrowsConflictForNonConfirmedLockBooking() {
        BookingRequest request = bookingRequest();
        Booking existingBooking = new Booking();
        existingBooking.setId(UUID.randomUUID());
        existingBooking.setStatus("PENDING");

        when(bookingRepository.findByPaymentId(request.getPaymentId())).thenReturn(Optional.empty());
        when(bookingRepository.findByLockId(request.getLockId())).thenReturn(Optional.of(existingBooking));

        assertThatThrownBy(() -> internalBookingService.finalizeBooking(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Lock already exists for a non-confirmed booking");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingItemRepository, never()).save(any(BookingItem.class));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    private BookingRequest bookingRequest() {
        BookingSeatRequest seat = new BookingSeatRequest();
        seat.setEventSeatId(UUID.randomUUID());
        seat.setSectionId(UUID.randomUUID());
        seat.setPriceMinor(1250L);

        BookingRequest request = new BookingRequest();
        request.setPaymentId(UUID.randomUUID());
        request.setUserId(UUID.randomUUID());
        request.setEventId(UUID.randomUUID());
        request.setLockId(UUID.randomUUID());
        request.setCurrency("USD");
        request.setTotalAmountMinor(1250L);
        request.setSeats(List.of(seat));
        return request;
    }
}
