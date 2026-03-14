package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingDetailItemResponse;
import com.example.booking_service.dtos.BookingDetailResponse;
import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.BookingResponse;
import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.BookingStatus;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import com.example.booking_service.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public BookingResponse getBookings(UUID userId) {
        List<BookingSummary> bookings = bookingRepository.findAllByUserIdOrderByBookedAtDesc(userId).stream()
                .map(this::toSummaryResponse)
                .toList();
        return new BookingResponse(bookings);
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        List<BookingDetailItemResponse> items = bookingItemRepository.findAllByBookingIdOrderByIdAsc(bookingId).stream()
                .map(this::toItemResponse)
                .toList();
        return new BookingDetailResponse(booking.getId(), booking.getEventId(), booking.getStatus(), items);
    }

    @Transactional
    public Booking finalizeBooking(BookingRequest request) {
        Booking existingBooking = bookingRepository.findByPaymentId(request.getPaymentId()).orElse(null);
        if (existingBooking != null) {
            if (!BookingStatus.CONFIRMED.name().equals(existingBooking.getStatus())) {
                throw new BookingConflictException("Payment already exists for a non-confirmed booking");
            }
            return existingBooking;
        }

        Booking existingLockBooking = bookingRepository.findByLockId(request.getLockId()).orElse(null);
        if (existingLockBooking != null) {
            if (!BookingStatus.CONFIRMED.name().equals(existingLockBooking.getStatus())) {
                throw new BookingConflictException("Lock already exists for a non-confirmed booking");
            }
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
            }
        }

        return savedBooking;
    }

    private BookingDetailItemResponse toItemResponse(BookingItem bookingItem) {
        return new BookingDetailItemResponse(
                bookingItem.getId(),
                bookingItem.getEventSeatId(),
                bookingItem.getSectionId(),
                bookingItem.getPriceMinor()
        );
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
