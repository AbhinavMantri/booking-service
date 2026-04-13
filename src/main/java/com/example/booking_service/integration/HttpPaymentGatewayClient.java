package com.example.booking_service.integration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpPaymentGatewayClient implements PaymentGatewayClient {
    private final RestClient restClient;
    private final String internalSharedSecret;

    public HttpPaymentGatewayClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.payment.base-url:http://localhost:8084}") String baseUrl,
            @Value("${integration.payment.internal-shared-secret:dev-shared-secret-change-me}") String internalSharedSecret
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.internalSharedSecret = internalSharedSecret;
    }

    @Override
    public PaymentSnapshot initiatePayment(String idempotencyKey, PaymentInitiationPayload request) {
        PaymentSummaryEnvelope response = restClient.post()
                .uri("/internal/v1/payments/initiate")
                .header("X-Internal-Auth", internalSharedSecret)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PaymentSummaryEnvelope.class);
        if (response == null || response.getPayment() == null) {
            throw new IllegalStateException("Payment service returned an empty initiate response");
        }
        return response.getPayment();
    }

    @Data
    private static class PaymentSummaryEnvelope {
        private String message;
        private String status;
        private PaymentSnapshot payment;
    }
}
