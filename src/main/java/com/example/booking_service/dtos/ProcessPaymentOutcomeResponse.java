package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessPaymentOutcomeResponse extends ApiResponse {
    private UUID bookingId;
    private String action;
}
