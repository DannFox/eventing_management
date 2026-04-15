package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.WebhookResponseDTO;
import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;
import com.ticketing.event.eventing_management.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/woocommerce")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook WooCommerce", description = "Recepcion de notificaciones de pago")
public class WebhookController {

    private final TicketService ticketService;

    @PostMapping("/webhook")
    @Operation(summary = "Recibe notificacion de orden pagada desde WooCommerce")
    public ResponseEntity<WebhookResponseDTO> handleWebhook(@RequestBody WooCommerceOrderDTO order) {
        try {
            log.info("Webhook recibido: orden #{} con estado '{}'",
                    order != null ? order.getId() : null,
                    order != null ? order.getStatus() : null);

            if (order == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(buildResponse("error", "Payload vacio", null));
            }

            if (!"processing".equals(order.getStatus()) && !"completed".equals(order.getStatus())) {
                return ResponseEntity.ok(buildResponse("ignored", "Estado no procesable", null));
            }

            String ticketId = ticketService.processTicketPurchase(order);
            return ResponseEntity.ok(buildResponse("success", "Ticket creado", ticketId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error de negocio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse("error", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error interno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildResponse("error", "Error interno", null));
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
