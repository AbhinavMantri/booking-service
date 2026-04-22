package com.example.booking_service.dtos;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingRequest {
    private UUID paymentId;
    private UUID userId;
    private UUID eventId;
    private UUID lockId;
    private String currency;
    private Long totalAmountMinor;
    private List<BookingSeatRequest> seats;
}
