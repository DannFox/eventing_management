package com.ticketing.event.eventing_management.dto;

import lombok.Data;

import java.util.List;

@Data
public class WooCommerceOrderDTO {
    private Long id;
    private String status;
    private Billing billing;
    private List<LineItem> line_items;

    @Data
    public static class Billing {
        private String email;
        private String first_name;
        private String last_name;
    }

    @Data
    public static class LineItem {
        private Long product_id;
        private Integer quantity;
    }
}
