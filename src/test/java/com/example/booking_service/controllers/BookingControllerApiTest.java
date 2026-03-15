package com.example.booking_service.controllers;

import com.example.booking_service.dtos.BookingSummary;
import com.example.booking_service.exceptions.BookingNotFoundException;
import com.example.booking_service.logging.PublicApiAuthenticationFilter;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.services.BookingService;
import com.example.booking_service.services.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerApiTest {
    @Mock
    private BookingService bookingService;

    @Mock
    private JWTService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController(bookingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new RequestCorrelationFilter(), new PublicApiAuthenticationFilter(jwtService, new ObjectMapper()))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void getBookingsReturnsOkAndPropagatesRequestIdHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        BookingSummary summary = new BookingSummary(UUID.randomUUID(), UUID.randomUUID(), "CONFIRMED", "USD", 1000L);

        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", userId.toString()));
        when(bookingService.getBookings(userId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "booking-api-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, "booking-api-123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Bookings retrieved successfully"))
                .andExpect(jsonPath("$.bookings", hasSize(1)))
                .andExpect(jsonPath("$.bookings[0].bookingId").value(summary.bookingId().toString()));
    }

    @Test
    void getBookingsReturnsUnauthorizedWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void getBookingReturnsOkWithDetailPayload() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setEventId(eventId);
        booking.setStatus("CONFIRMED");

        BookingItem item = new BookingItem();
        item.setId(itemId);
        item.setEventSeatId(UUID.randomUUID());
        item.setSectionId(UUID.randomUUID());
        item.setPriceMinor(1800L);

        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(bookingService.getBooking(bookingId)).thenReturn(booking);
        when(bookingService.getBookingItems(bookingId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].bookingItemId").value(itemId.toString()));
    }

    @Test
    void getBookingReturnsNotFoundWhenMissing() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(bookingService.getBooking(bookingId)).thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER));
    }

    @Test
    void getTicketsForBookingReturnsOkPayload() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTicketCode("CODE-123");
        ticket.setTicketNumber("TKT-2026-API");
        ticket.setStatus(TicketStatus.ISSUED);

        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(bookingService.getTicketsForBooking(bookingId)).thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}/tickets", bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.tickets", hasSize(1)))
                .andExpect(jsonPath("$.tickets[0].ticketId").value(ticket.getId().toString()));
    }

    @Test
    void getTicketsForBookingReturnsNotFoundPayload() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(bookingService.getTicketsForBooking(bookingId)).thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}/tickets", bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Booking not found: " + bookingId));
    }
}
