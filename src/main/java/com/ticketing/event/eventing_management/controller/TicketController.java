package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.ApiErrorResponseDTO;
import com.ticketing.event.eventing_management.dto.TicketDTO;
import com.ticketing.event.eventing_management.dto.TicketValidationRequestDTO;
import com.ticketing.event.eventing_management.dto.TicketValidationResponseDTO;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    @Operation(summary = "Obtener detalle de ticket por su ID",
            description = "Usado por Grupo B para sincronizar wallet; incluye estado ACTIVE/USED/CANCELLED y qrCode.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketDTO.class),
                            examples = @ExampleObject(name = "Ticket activo", value = """
                                    {
                                      "ticketId": "5b81b393-2d36-47cb-91ec-77844b7f7d82",
                                      "eventId": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                      "eventName": "Concierto de verano",
                                      "attendeeId": "9b0cb16a-53ec-4c25-81b4-66c0fdf34531",
                                      "attendeeEmail": "cliente@example.com",
                                      "attendeeName": "Cliente Demo",
                                      "status": "ACTIVE",
                                      "qrCode": "NWI4MWIzOTMtMmQzNi00N2NiLTkxZWMtNzc4NDRiN2Y3ZDgy",
                                      "seatInfo": "General",
                                      "purchasedAt": "2026-05-18T21:30:00",
                                      "validatedAt": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Ticket no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Ticket no encontrado", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 404,
                                      "error": "Recurso no encontrado",
                                      "message": "Ticket no encontrado"
                                    }
                                    """)))
    })
    public ResponseEntity<TicketDTO> getTicketById(
            @Parameter(description = "UUID del ticket", required = true) @PathVariable UUID ticketId) {
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
    @Operation(summary = "Validar ticket por ticketId o qrCode",
            description = "Primera validacion: estado USED y emision RabbitMQ ticket.validated. Ticket ya USED: respuesta 200 idempotente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validacion exitosa o ticket ya validado previamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketValidationResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "Validacion exitosa", value = """
                                            {
                                              "ticketId": "5b81b393-2d36-47cb-91ec-77844b7f7d82",
                                              "eventId": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                              "status": "USED",
                                              "validatedAt": "2026-05-18T22:10:00",
                                              "message": "Ticket validado correctamente"
                                            }
                                            """),
                                    @ExampleObject(name = "Ya validado", value = """
                                            {
                                              "ticketId": "5b81b393-2d36-47cb-91ec-77844b7f7d82",
                                              "eventId": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                              "status": "USED",
                                              "validatedAt": "2026-05-18T22:10:00",
                                              "message": "Ticket ya fue validado previamente"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "Payload invalido (ticketId/qrCode, evento no coincide)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Payload invalido", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 400,
                                      "error": "Solicitud invalida",
                                      "message": "Debe enviar ticketId o qrCode"
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Ticket no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Ticket no encontrado", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 404,
                                      "error": "Recurso no encontrado",
                                      "message": "Ticket no encontrado"
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "Ticket cancelado u otra regla de negocio (IllegalStateException)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Ticket cancelado", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 409,
                                      "error": "Conflicto de estado",
                                      "message": "Ticket cancelado"
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Error interno", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 500,
                                      "error": "Error interno",
                                      "message": "Error inesperado validando ticket"
                                    }
                                    """)))
    })
    public ResponseEntity<TicketValidationResponseDTO> validateTicket(
            @RequestBody TicketValidationRequestDTO request) {
        return ResponseEntity.ok(ticketService.validateTicket(request));
    }
}
