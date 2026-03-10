package com.example.booking_service.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class BookingSeatRequest {
    private UUID eventSeatId;
    private UUID sectionId;
    private Long priceMinor;
}