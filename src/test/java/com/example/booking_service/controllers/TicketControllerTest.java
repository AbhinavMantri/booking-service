package com.example.booking_service.controllers;

import com.example.booking_service.dtos.ScanTicketRequest;
import com.example.booking_service.dtos.ScanTicketResponse;
import com.example.booking_service.dtos.TicketResponse;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.TicketNotFoundException;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.services.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {
    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    @Test
    void getTicketReturnsSuccessResponse() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-2026-XYZ");
        ticket.setTicketCode("CODE-XYZ");
        ticket.setStatus(TicketStatus.ISSUED);

        when(ticketService.getTicket(ticketId)).thenReturn(ticket);

        ResponseEntity<TicketResponse> response = ticketController.getTicket(ticketId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().getTicketDetails().getTicketId()).isEqualTo(ticketId);
    }

    @Test
    void getTicketReturnsNotFoundResponse() {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.getTicket(ticketId)).thenThrow(new TicketNotFoundException(ticketId));

        ResponseEntity<TicketResponse> response = ticketController.getTicket(ticketId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.FAILURE);
        assertThat(response.getBody().getMessage()).isEqualTo("Ticket not found: " + ticketId);
    }

    @Test
    void scanTicketReturnsSuccessResponse() {
        ScanTicketRequest request = new ScanTicketRequest("CODE-123");
        Ticket ticket = new Ticket();
        UUID ticketId = UUID.randomUUID();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.USED);

        when(ticketService.scanTicket(request)).thenReturn(ticket);

        ResponseEntity<ScanTicketResponse> response = ticketController.scanTicket(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().getTicketId()).isEqualTo(ticketId);
        assertThat(response.getBody().getTicketStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    void scanTicketReturnsNotFoundResponse() {
        ScanTicketRequest request = new ScanTicketRequest("MISSING");
        when(ticketService.scanTicket(request)).thenThrow(new TicketNotFoundException("MISSING"));

        ResponseEntity<ScanTicketResponse> response = ticketController.scanTicket(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ResponseStatus.FAILURE);
        assertThat(response.getBody().getMessage()).isEqualTo("Ticket not found: MISSING");
    }
}
