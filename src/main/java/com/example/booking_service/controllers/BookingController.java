package com.example.booking_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.BookingResponse;

@RestController
@RequestMapping("/bookings/finalize")
public class BookingController {
    @PostMapping("")
    public ResponseEntity<BookingResponse> finalizeBooking(@RequestBody BookingRequest bookingRequest) {
        return ResponseEntity.ok(new BookingResponse());
    }
}
