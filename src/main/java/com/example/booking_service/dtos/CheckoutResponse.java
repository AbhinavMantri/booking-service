package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class CheckoutResponse extends ApiResponse {
    private UUID bookingId;
    private UUID paymentId;
    private UUID eventId;
    private Long totalAmountMinor;
    private String currency;
    private String paymentStatus;
    private String providerKeyId;
    private String providerOrderId;
    private List<BookingSeatRequest> seats;
}
