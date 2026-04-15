package com.ticketing.event.eventing_management.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WebhookResponseDTO {
    private String status;
    private String message;
    private String ticketId;
}
