package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingDetailItemResponse;
import com.example.booking_service.dtos.BookingDetailResponse;
import com.example.booking_service.dtos.BookingResponse;
import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;

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

        BookingDetailResponse response = new BookingDetailResponse();
        response.setBookingId(booking.getId());
        response.setEventId(booking.getEventId());
        response.setStatus(booking.getStatus());
        response.setItems(items);
        return response;
    }

    private BookingDetailItemResponse toItemResponse(BookingItem bookingItem) {
        return BookingDetailItemResponse.builder()
                .bookingItemId(bookingItem.getId())
                .eventSeatId(bookingItem.getEventSeatId())
                .sectionId(bookingItem.getSectionId())
                .priceMinor(bookingItem.getPriceMinor())
                .build();
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
