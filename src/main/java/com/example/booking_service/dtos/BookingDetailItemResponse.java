package com.example.booking_service.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BookingDetailItemResponse {
    private UUID bookingItemId;
    private UUID eventSeatId;
    private UUID sectionId;
    private Long priceMinor;
}
