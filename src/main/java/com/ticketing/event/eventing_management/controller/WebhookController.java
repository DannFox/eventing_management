package com.ticketing.event.eventing_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.event.eventing_management.dto.WebhookResponseDTO;
import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;
import com.ticketing.event.eventing_management.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/woocommerce")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook WooCommerce", description = "Recepcion de notificaciones de pago")
public class WebhookController {

    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/webhook",
            consumes = {MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<WebhookResponseDTO> handleWebhook(
            @RequestParam(required = false) Map<String, String> formData,
            @RequestBody(required = false) WooCommerceOrderDTO order,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        log.info("Webhook recibido - Content-Type: {}", contentType);

        // Para cualquier solicitud de prueba, responder OK inmediatamente
        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            log.info("Solicitud de prueba - respondiendo OK");
            return ResponseEntity.ok(new WebhookResponseDTO("test", "Webhook validado", null));
        }

        // Para solicitudes JSON sin datos (prueba)
        if (order == null || order.getId() == null) {
            log.info("Solicitud de prueba (JSON vacío) - respondiendo OK");
            return ResponseEntity.ok(new WebhookResponseDTO("test", "Webhook validado", null));
        }

        // Lógica normal para órdenes reales
        try {
            if (!"processing".equals(order.getStatus()) && !"completed".equals(order.getStatus())) {
                return ResponseEntity.ok(new WebhookResponseDTO("ignored", "Estado no procesable", null));
            }

            String ticketId = ticketService.processTicketPurchase(order);
            return ResponseEntity.ok(new WebhookResponseDTO("success", "Ticket creado", ticketId));

        } catch (Exception e) {
            log.error("Error procesando orden", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookResponseDTO("error", e.getMessage(), null));
        }
    }

    private WebhookResponseDTO buildResponse(String status, String message, String ticketId) {
        WebhookResponseDTO response = new WebhookResponseDTO();
        response.setStatus(status);
        response.setMessage(message);
        response.setTicketId(ticketId);
        return response;
    }
}
