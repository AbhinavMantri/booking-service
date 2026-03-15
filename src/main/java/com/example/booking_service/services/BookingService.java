package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingDetailResponse;
import com.example.booking_service.dtos.BookingResponse;
import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.dtos.BookingTicketResponse;
import com.example.booking_service.dtos.TicketDetails;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketService ticketService;

    @Transactional(readOnly = true)
    public List<BookingSummary> getBookings(UUID userId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} booking service getBookings started userId={}", requestId, userId);

        List<BookingSummary> bookings = bookingRepository.findAllByUserIdOrderByBookedAtDesc(userId).stream()
                .map(this::toSummaryResponse)
                .toList();
        log.info("requestId={} booking service getBookings completed userId={} bookings={} latencyMs={}",
                requestId, userId, bookings.size(), System.currentTimeMillis() - startTime);
        return bookings;
    }

    @Transactional(readOnly = true)
    public Booking getBooking(UUID bookingId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} booking service getBooking started bookingId={}", requestId, bookingId);

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException(bookingId));
            log.info("requestId={} booking service getBooking completed bookingId={} latencyMs={}",
                    requestId, bookingId, System.currentTimeMillis() - startTime);
            return booking;
        } catch (BookingNotFoundException ex) {
            log.warn("requestId={} booking service getBooking failed bookingId={} latencyMs={} reason={}",
                    requestId, bookingId, System.currentTimeMillis() - startTime, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<BookingItem> getBookingItems(UUID bookingId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} booking service getBookingItems started bookingId={}", requestId, bookingId);

        List<BookingItem> items = bookingItemRepository.findAllByBookingIdOrderByIdAsc(bookingId).stream()
                .toList();
        log.info("requestId={} booking service getBookingItems completed bookingId={} items={} latencyMs={}",
                requestId, bookingId, items.size(), System.currentTimeMillis() - startTime);
        return items;
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsForBooking(UUID bookingId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} booking service getTicketsForBooking started bookingId={}", requestId, bookingId);

        if (!bookingRepository.existsById(bookingId)) {
            log.warn("requestId={} booking service getTicketsForBooking failed bookingId={} latencyMs={} reason={}",
                    requestId, bookingId, System.currentTimeMillis() - startTime, "Booking not found");
            throw new BookingNotFoundException(bookingId);
        }

        List<Ticket> tickets = ticketService.getTicketsForBooking(bookingId).stream()
                .toList();
        log.info("requestId={} booking service getTicketsForBooking completed bookingId={} tickets={} latencyMs={}",
                requestId, bookingId, tickets.size(), System.currentTimeMillis() - startTime);
        return tickets;
    }

    private BookingSummary toSummaryResponse(Booking booking) {
        return new BookingSummary(
                booking.getId(),
                booking.getEventId(),
                booking.getStatus(),
                booking.getCurrency(),
                booking.getTotalAmountMinor()
        );
    }
}
