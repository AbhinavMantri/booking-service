package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingDetailItem;
import com.example.booking_service.dtos.BookingDetailResponse;
import com.example.booking_service.dtos.BookingResponse;
import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.dtos.BookingTicketResponse;
import com.example.booking_service.dtos.TicketDetails;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.logging.PublicApiAuthenticationFilter;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.services.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<BookingResponse> getBookings(HttpServletRequest request) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} get bookings request received", requestId);

        UUID userId = UUID.fromString(String.valueOf(request.getAttribute(PublicApiAuthenticationFilter.AUTHENTICATED_USER_ID)));

        List<BookingSummary> bookings = bookingService.getBookings(userId);
        BookingResponse response = new BookingResponse();
        response.setBookings(bookings);
        response.setMessage("Bookings retrieved successfully");
        response.setStatus(ResponseStatus.SUCCESS);
        log.info("requestId={} get bookings request completed userId={} bookings={} latencyMs={}",
                requestId, userId, bookings.size(), System.currentTimeMillis() - startTime);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBooking(@PathVariable UUID bookingId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} get booking request received bookingId={}", requestId, bookingId);

        BookingDetailResponse response = new BookingDetailResponse();
        try {
            Booking booking = bookingService.getBooking(bookingId);
            List<BookingItem> items = bookingService.getBookingItems(bookingId);
            response.setBookingId(booking.getId());
            response.setEventId(booking.getEventId());
            response.setStatus(booking.getStatus());
            response.setItems(toBookingDetailItems(items));
            log.info("requestId={} get booking request completed bookingId={} items={} latencyMs={}",
                    requestId, bookingId, items.size(), System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (BookingNotFoundException e) {
            log.warn("requestId={} get booking request failed bookingId={} latencyMs={} reason={}",
                    requestId, bookingId, System.currentTimeMillis() - startTime, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{bookingId}/tickets")
    public ResponseEntity<BookingTicketResponse> getTicketsForBooking(@PathVariable UUID bookingId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} get booking tickets request received bookingId={}", requestId, bookingId);

        BookingTicketResponse response = new BookingTicketResponse();
        try {
            List<TicketDetails> tickets = bookingService.getTicketsForBooking(bookingId).stream()
                    .map(this::toTicketDetails)
                    .toList();
            response.setTickets(tickets);
            response.setMessage("Tickets retrieved successfully");
            response.setStatus(ResponseStatus.SUCCESS);
            log.info("requestId={} get booking tickets request completed bookingId={} tickets={} latencyMs={}",
                    requestId, bookingId, tickets.size(), System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (BookingNotFoundException e) {
            response.setMessage(e.getMessage());
            response.setStatus(ResponseStatus.FAILURE);
            log.warn("requestId={} get booking tickets request failed bookingId={} latencyMs={} reason={}",
                    requestId, bookingId, System.currentTimeMillis() - startTime, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    private TicketDetails toTicketDetails(Ticket ticket) {
        return TicketDetails.builder()
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .ticketStatus(ticket.getStatus())
                .ticketNumber(ticket.getTicketNumber())
                .build();
    }

    private List<BookingDetailItem> toBookingDetailItems(List<BookingItem> bookingItems) {
        return bookingItems.stream()
                .map(item -> BookingDetailItem.builder()
                        .bookingItemId(item.getId())
                        .eventSeatId(item.getEventSeatId())
                        .sectionId(item.getSectionId())
                        .priceMinor(item.getPriceMinor())
                        .build())
                .toList();
    }
}
