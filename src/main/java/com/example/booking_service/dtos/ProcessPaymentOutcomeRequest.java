package com.example.booking_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProcessPaymentOutcomeRequest {
    @NotNull(message = "paymentId is required")
    private UUID paymentId;

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "eventId is required")
    private UUID eventId;

    @NotNull(message = "lockId is required")
    private UUID lockId;

    @NotNull(message = "totalAmountMinor is required")
    private Long totalAmountMinor;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "paymentStatus is required")
    private String paymentStatus;
}
