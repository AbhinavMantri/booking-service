package com.example.booking_service.dtos;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingDetailResponse {
    private UUID bookingId;
    private UUID eventId;
    private String status;
    private List<BookingDetailItem> items;
}
