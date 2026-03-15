package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookingResponse extends ApiResponse {
    private List<BookingSummary> bookings;
}
