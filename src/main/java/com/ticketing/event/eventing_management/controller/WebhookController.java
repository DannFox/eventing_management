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

    //1. Endpoint para el Ping de validación inicial de WooCommerce
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<WebhookResponseDTO> handleWebhookPing(
            @RequestParam Map<String, String> formData) {
        log.info("Webhook recibido (Ping) - Content-Type: application/x-www-form-urlencoded");

        return ResponseEntity.ok(new WebhookResponseDTO("test", "Webhook validado correctamente", null));
    }

    // 2. Endpoint para las notificaciones de órdenes reales (application/json)
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookResponseDTO> handleWebhookOrder(
            @RequestParam WooCommerceOrderDTO wooCommerceOrderDTO) {
        log.info("Webhook recibido (Order) - Content-Type: application/json");

        if (wooCommerceOrderDTO == null || wooCommerceOrderDTO.getId() == null) {
            log.info("Solicitud JSON vacía o sin ID - Respondiendo OK para evitar reintentos");

            return ResponseEntity.ok(new WebhookResponseDTO("test", "Payload vacío ignorado", null));
        }

        try {
            if (!"processing".equals(wooCommerceOrderDTO.getStatus()) && !"completed".equals(wooCommerceOrderDTO.getStatus())) {
                return ResponseEntity.ok(new WebhookResponseDTO("ignored", "Estado no procesable" + wooCommerceOrderDTO.getStatus(), null));
            }

            String ticketId = ticketService.processTicketPurchase(wooCommerceOrderDTO);
            return ResponseEntity.ok(new WebhookResponseDTO("success", "Ticket creado", ticketId));
        } catch (Exception e) {
            log.error("Error procesando orden de WooCommerce ID: {}", wooCommerceOrderDTO.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookResponseDTO("error", "Error interno procesando la order", null));
        }
    }
}
