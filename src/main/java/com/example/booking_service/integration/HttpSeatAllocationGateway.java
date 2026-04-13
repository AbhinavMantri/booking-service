package com.example.booking_service.integration;

import com.example.booking_service.services.PaymentOutcomeAction;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class HttpSeatAllocationGateway implements SeatAllocationGateway {
    private final RestClient restClient;
    private final String serviceName;
    private final String serviceToken;

    public HttpSeatAllocationGateway(
            RestClient.Builder restClientBuilder,
            @Value("${integration.seats-allocation.base-url:http://localhost:8083/seats-allocation-service/v1}") String baseUrl,
            @Value("${integration.seats-allocation.service-name:booking-service}") String serviceName,
            @Value("${integration.seats-allocation.service-token:seats-strong-dev-token}") String serviceToken
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.serviceName = serviceName;
        this.serviceToken = serviceToken;
    }

    @Override
    public void lockSeats(UUID eventId, UUID bookingId, UUID userId, String idempotencyKey, List<UUID> seatIds) {
        LockSeatsPayload request = new LockSeatsPayload();
        request.setBookingId(bookingId);
        request.setUserId(userId);
        request.setIdempotencyKey(idempotencyKey);
        request.setSeatIds(seatIds);
        restClient.post()
                .uri("/internal/seats/{eventId}/locks", eventId)
                .header("X-Service-Name", serviceName)
                .header("X-Service-Token", serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public LockSnapshot getLockDetails(UUID bookingId) {
        LockDetailResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/locks").queryParam("bookingId", bookingId).build())
                .retrieve()
                .body(LockDetailResponse.class);
        if (response == null || response.getResult() == null) {
            throw new IllegalStateException("Seat allocation returned an empty lock detail response");
        }
        return response.getResult();
    }

    @Override
    public LockSnapshot getLockDetailsOrNull(UUID bookingId) {
        try {
            return getLockDetails(bookingId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public void confirmSeats(UUID eventId, UUID bookingId, UUID paymentId, List<UUID> seatIds, String idempotencyKey) {
        ConfirmSeatsPayload request = new ConfirmSeatsPayload();
        request.setEventId(eventId);
        request.setBookingId(bookingId);
        request.setPaymentId(paymentId);
        request.setSeatIds(seatIds);
        request.setConfirmedAt(Instant.now());
        restClient.post()
                .uri("/internal/seats/confirm")
                .header("X-Service-Name", serviceName)
                .header("X-Service-Token", serviceToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void releaseSeats(UUID eventId, UUID bookingId, List<UUID> seatIds, PaymentOutcomeAction action, String idempotencyKey) {
        ReleaseSeatsPayload request = new ReleaseSeatsPayload();
        request.setEventId(eventId.toString());
        request.setBookingId(bookingId.toString());
        request.setSeatIds(seatIds.stream().map(UUID::toString).toList());
        request.setReason(action == PaymentOutcomeAction.CANCELLED ? "ORDER_CANCELLED" : "PAYMENT_FAILED");
        restClient.post()
                .uri("/internal/seats/release")
                .header("X-Service-Name", serviceName)
                .header("X-Service-Token", serviceToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Data
    private static class LockSeatsPayload {
        private UUID bookingId;
        private UUID userId;
        private String idempotencyKey;
        private List<UUID> seatIds;
    }

    @Data
    private static class ConfirmSeatsPayload {
        private UUID eventId;
        private UUID bookingId;
        private UUID paymentId;
        private List<UUID> seatIds;
        private Instant confirmedAt;
    }

    @Data
    private static class ReleaseSeatsPayload {
        private String eventId;
        private String bookingId;
        private List<String> seatIds;
        private String reason;
    }

    @Data
    private static class LockDetailResponse {
        private String message;
        private String status;
        private LockSnapshot result;
    }
}
