package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.WebhookResponseDTO;
import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;
import com.ticketing.event.eventing_management.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/woocommerce")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook WooCommerce", description = "Recepcion de notificaciones de pago")
public class WebhookController {
    private final TicketService ticketService;

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Validar ping inicial del webhook de WooCommerce",
            description = "WooCommerce envia application/x-www-form-urlencoded al crear el webhook.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ping aceptado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WebhookResponseDTO.class),
                            examples = @ExampleObject(name = "Ping aceptado", value = """
                                    {
                                      "status": "test",
                                      "message": "Webhook validado correctamente",
                                      "ticketId": null
                                    }
                                    """)))
    })
    public ResponseEntity<WebhookResponseDTO> handleWebhookPing(
            @RequestParam Map<String, String> formData) {
        log.info("Webhook recibido (Ping) - Content-Type: application/x-www-form-urlencoded");
        return ResponseEntity.ok(new WebhookResponseDTO("test", "Webhook validado correctamente", null));
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Procesar orden confirmada desde WooCommerce y crear ticket",
            description = "Estados processing o completed. Crea ticket(s), actualiza Redis y publica ticket.sold. "
                    + "Errores de negocio (aforo, producto desconocido) se responden con 400 y cuerpo WebhookResponseDTO status=error.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden procesada, ignorada (estado no aplicable) o payload vacio tolerado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WebhookResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "Ticket creado", value = """
                                            {
                                              "status": "success",
                                              "message": "Ticket creado",
                                              "ticketId": "5b81b393-2d36-47cb-91ec-77844b7f7d82"
                                            }
                                            """),
                                    @ExampleObject(name = "Orden ignorada", value = """
                                            {
                                              "status": "ignored",
                                              "message": "Estado no procesable cancelled",
                                              "ticketId": null
                                            }
                                            """),
                                    @ExampleObject(name = "Payload vacio", value = """
                                            {
                                              "status": "test",
                                              "message": "Payload vacio ignorado",
                                              "ticketId": null
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o regla de negocio incumplida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WebhookResponseDTO.class),
                            examples = @ExampleObject(name = "Error de negocio", value = """
                                    {
                                      "status": "error",
                                      "message": "Aforo agotado para el evento",
                                      "ticketId": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Error interno al procesar orden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WebhookResponseDTO.class),
                            examples = @ExampleObject(name = "Error interno", value = """
                                    {
                                      "status": "error",
                                      "message": "Error interno procesando la order",
                                      "ticketId": null
                                    }
                                    """)))
    })
    public ResponseEntity<WebhookResponseDTO> handleWebhookOrder(
            @RequestBody(required = false) WooCommerceOrderDTO wooCommerceOrderDTO) {
        log.info("Webhook recibido (Order) - Content-Type: application/json");

        if (wooCommerceOrderDTO == null || wooCommerceOrderDTO.getId() == null) {
            log.info("Solicitud JSON vacia o sin ID - Respondiendo OK para evitar reintentos");
            return ResponseEntity.ok(new WebhookResponseDTO("test", "Payload vacio ignorado", null));
        }

        try {
            if (!"processing".equals(wooCommerceOrderDTO.getStatus())
                    && !"completed".equals(wooCommerceOrderDTO.getStatus())
                    && !"on-hold".equals(wooCommerceOrderDTO.getStatus())) {
                return ResponseEntity.ok(new WebhookResponseDTO(
                        "ignored",
                        "Estado no procesable " + wooCommerceOrderDTO.getStatus(),
                        null));
            }

            String ticketId = ticketService.processTicketPurchase(wooCommerceOrderDTO);
            return ResponseEntity.ok(new WebhookResponseDTO("success", "Ticket creado", ticketId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Payload de WooCommerce invalido para orden ID: {}", wooCommerceOrderDTO.getId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new WebhookResponseDTO("error", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error procesando orden de WooCommerce ID: {}", wooCommerceOrderDTO.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookResponseDTO("error", "Error interno procesando la order", null));
        }
    }
}
