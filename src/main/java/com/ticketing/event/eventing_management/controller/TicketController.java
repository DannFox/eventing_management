package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.TicketDTO;
import com.ticketing.event.eventing_management.dto.TicketValidationRequestDTO;
import com.ticketing.event.eventing_management.dto.TicketValidationResponseDTO;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Operaciones de consulta y validacion de tickets")
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/{ticketId}")
    @Operation(summary = "Obtener detalle de ticket por su ID")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable UUID ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);

        TicketDTO response = TicketDTO.builder()
                .ticketId(ticket.getId())
                .eventId(ticket.getEvent().getId())
                .eventName(ticket.getEvent().getTitle())
                .attendeeId(ticket.getAttendeeId())
                .attendeeEmail(ticket.getAttendeeEmail())
                .attendeeName(ticket.getAttendeeName())
                .status(ticket.getStatus().name())
                .qrCode(ticket.getQrCode())
                .seatInfo(ticket.getSeatInfo())
                .purchasedAt(ticket.getPurchasedAt())
                .validatedAt(ticket.getValidatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar ticket por ticketId o qrCode")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket validado o ya validado",
                    content = @Content(schema = @Schema(implementation = TicketValidationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida"),
            @ApiResponse(responseCode = "404", description = "Ticket no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<TicketValidationResponseDTO> validateTicket(
            @RequestBody TicketValidationRequestDTO request) {
        return ResponseEntity.ok(ticketService.validateTicket(request));
    }
}
