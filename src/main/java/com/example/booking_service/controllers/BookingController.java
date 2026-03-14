package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingDetailResponse;
import com.example.booking_service.dtos.BookingResponse;
import com.example.booking_service.dtos.TicketResponse;
import com.example.booking_service.services.BookingService;
import com.example.booking_service.services.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final TicketService ticketService;

    @GetMapping
    public BookingResponse getBookings(@RequestParam UUID userId) {
        return bookingService.getBookings(userId);
    }

    @GetMapping("/{bookingId}")
    public BookingDetailResponse getBooking(@PathVariable UUID bookingId) {
        return bookingService.getBooking(bookingId);
    }

    @GetMapping("/{bookingId}/tickets")
    public List<TicketResponse> getTicketsForBooking(@PathVariable UUID bookingId) {
        return ticketService.getTicketsForBooking(bookingId);
    }
}
