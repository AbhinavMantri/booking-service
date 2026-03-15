package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.BookingStatus;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import com.example.booking_service.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalBookingService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketRepository ticketRepository;

    @Transactional
    public Booking finalizeBooking(BookingRequest request) throws BookingConflictException {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        log.info("requestId={} finalize booking started paymentId={} lockId={}", requestId, request.getPaymentId(), request.getLockId());

        Booking existingBooking = bookingRepository.findByPaymentId(request.getPaymentId()).orElse(null);
        if (existingBooking != null) {
            if (!BookingStatus.CONFIRMED.name().equals(existingBooking.getStatus())) {
                log.warn("requestId={} payment already linked to non-confirmed booking bookingId={} status={}",
                        requestId, existingBooking.getId(), existingBooking.getStatus());
                throw new BookingConflictException("Payment already exists for a non-confirmed booking");
            }
            log.info("requestId={} returning existing confirmed booking by payment bookingId={}", requestId, existingBooking.getId());
            return existingBooking;
        }

        Booking existingLockBooking = bookingRepository.findByLockId(request.getLockId()).orElse(null);
        if (existingLockBooking != null) {
            if (!BookingStatus.CONFIRMED.name().equals(existingLockBooking.getStatus())) {
                log.warn("requestId={} lock already linked to non-confirmed booking bookingId={} status={}",
                        requestId, existingLockBooking.getId(), existingLockBooking.getStatus());
                throw new BookingConflictException("Lock already exists for a non-confirmed booking");
            }
            log.info("requestId={} returning existing confirmed booking by lock bookingId={}", requestId, existingLockBooking.getId());
            return existingLockBooking;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setEventId(request.getEventId());
        booking.setPaymentId(request.getPaymentId());
        booking.setLockId(request.getLockId());
        booking.setCurrency(request.getCurrency());
        booking.setTotalAmountMinor(request.getTotalAmountMinor());
        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.setBookedAt(now);
        Booking savedBooking = bookingRepository.save(booking);

        int createdTickets = 0;
        if (request.getSeats() != null) {
            for (var seat : request.getSeats()) {
                BookingItem bookingItem = new BookingItem();
                bookingItem.setBookingId(savedBooking.getId());
                bookingItem.setEventSeatId(seat.getEventSeatId());
                bookingItem.setSectionId(seat.getSectionId());
                bookingItem.setPriceMinor(seat.getPriceMinor());
                BookingItem savedBookingItem = bookingItemRepository.save(bookingItem);

                Ticket ticket = new Ticket();
                ticket.setBookingId(savedBooking.getId());
                ticket.setBookingItemId(savedBookingItem.getId());
                ticket.setEventId(savedBooking.getEventId());
                ticket.setUserId(savedBooking.getUserId());
                ticket.setTicketNumber("TKT-" + now.getYear() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
                ticket.setTicketCode(UUID.randomUUID().toString().replace("-", "").toUpperCase());
                ticket.setStatus(TicketStatus.ISSUED);
                ticket.setIssuedAt(now);
                ticketRepository.save(ticket);
                createdTickets++;
            }
        }

        log.info("requestId={} finalize booking created bookingId={} tickets={}", requestId, savedBooking.getId(), createdTickets);
        return savedBooking;
    }
}
