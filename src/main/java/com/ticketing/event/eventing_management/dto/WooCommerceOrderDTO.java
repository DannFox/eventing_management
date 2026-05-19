package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Cuerpo JSON de orden WooCommerce (webhook). Campos usados: id, status, line_items, billing, customer_id")
public class WooCommerceOrderDTO {
    private Long id;
    private String status;
    private Long customer_id;
    private Billing billing;
    private List<LineItem> line_items;

    @Data
    @Schema(description = "Datos de facturacion del comprador")
    public static class Billing {
        private String email;
        private String first_name;
        private String last_name;
    }

    @Data
    @Schema(description = "Linea de pedido; product_id enlaza con evento sincronizado")
    public static class LineItem {
        private Long product_id;
        private Integer quantity;
    }
}
