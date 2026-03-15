package com.example.booking_service.controllers;

import com.example.booking_service.exceptions.BookingConflictException;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.repositories.TicketRepository;
import com.example.booking_service.services.InternalBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalBookingControllerApiTest {
    @Mock
    private InternalBookingService internalBookingService;

    @Mock
    private TicketRepository ticketRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalBookingController controller = new InternalBookingController(internalBookingService, ticketRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new RequestCorrelationFilter())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void finalizeBookingReturnsOkAndPropagatesRequestIdHeader() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);

        Ticket ticket = new Ticket();
        ticket.setBookingId(bookingId);
        ticket.setTicketNumber("TKT-2026-API12345");
        ticket.setIssuedAt(OffsetDateTime.now());

        when(internalBookingService.finalizeBooking(any())).thenReturn(booking);
        when(ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(bookingId)).thenReturn(List.of(ticket));

        mockMvc.perform(post("/internal/bookings/finalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "api-request-123")
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, "api-request-123"))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.tickets", hasSize(1)));
    }

    @Test
    void finalizeBookingReturnsConflictResponse() throws Exception {
        when(internalBookingService.finalizeBooking(any()))
                .thenThrow(new BookingConflictException("Payment already exists for a non-confirmed booking"));

        mockMvc.perform(post("/internal/bookings/finalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isConflict())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Payment already exists for a non-confirmed booking"));
    }

    private String requestJson() {
        return """
                {
                  \"paymentId\": \"11111111-1111-1111-1111-111111111111\",
                  \"userId\": \"22222222-2222-2222-2222-222222222222\",
                  \"eventId\": \"33333333-3333-3333-3333-333333333333\",
                  \"lockId\": \"44444444-4444-4444-4444-444444444444\",
                  \"currency\": \"USD\",
                  \"totalAmountMinor\": 1000
                }
                """;
    }
}
