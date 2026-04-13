package com.example.booking_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequest {
    @NotNull(message = "eventId is required")
    private UUID eventId;

    @NotBlank(message = "provider is required")
    private String provider;

    @NotEmpty(message = "seatIds are required")
    private List<@NotNull(message = "Each seatId is required") UUID> seatIds;
}
