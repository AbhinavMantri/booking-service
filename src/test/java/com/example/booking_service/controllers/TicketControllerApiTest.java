package com.example.booking_service.controllers;

import com.example.booking_service.exceptions.TicketNotFoundException;
import com.example.booking_service.logging.PublicApiAuthenticationFilter;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.services.JWTService;
import com.example.booking_service.services.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerApiTest {
    private static final String API_PREFIX = "/booking-service/v1";

    @Mock
    private TicketService ticketService;

    @Mock
    private JWTService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TicketController controller = new TicketController(ticketService);
        PublicApiAuthenticationFilter authFilter = new PublicApiAuthenticationFilter(jwtService);
        ReflectionTestUtils.setField(authFilter, "apiPrefix", API_PREFIX);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new RequestCorrelationFilter(), authFilter)
                .build();
    }

    @Test
    void getTicketReturnsOkPayload() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-2026-API");
        ticket.setTicketCode("CODE-123");
        ticket.setStatus(TicketStatus.ISSUED);

        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(ticketService.getTicket(ticketId)).thenReturn(ticket);

        mockMvc.perform(get(API_PREFIX + "/tickets/{ticketId}", ticketId)
                        .contextPath(API_PREFIX)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "ticket-api-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, "ticket-api-1"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.ticketDetails.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.ticketDetails.ticketCode").value("CODE-123"));
    }

    @Test
    void getTicketReturnsUnauthorizedWithoutBearerToken() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/tickets/{ticketId}", UUID.randomUUID()).contextPath(API_PREFIX))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void getTicketReturnsNotFoundPayload() throws Exception {
        UUID ticketId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(ticketService.getTicket(ticketId)).thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get(API_PREFIX + "/tickets/{ticketId}", ticketId)
                        .contextPath(API_PREFIX)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Ticket not found: " + ticketId));
    }

    @Test
    void scanTicketReturnsOkPayload() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.USED);

        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(ticketService.scanTicket(any())).thenReturn(ticket);

        mockMvc.perform(post(API_PREFIX + "/tickets/scan")
                        .contextPath(API_PREFIX)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketCode": "CODE-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.ticketStatus").value("USED"));
    }

    @Test
    void scanTicketReturnsNotFoundPayload() throws Exception {
        when(jwtService.validateAndExtractClaims("api-token")).thenReturn(Map.of("userId", UUID.randomUUID().toString()));
        when(ticketService.scanTicket(any())).thenThrow(new TicketNotFoundException("MISSING"));

        mockMvc.perform(post(API_PREFIX + "/tickets/scan")
                        .contextPath(API_PREFIX)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer api-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketCode": "MISSING"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Ticket not found: MISSING"));
    }
}
