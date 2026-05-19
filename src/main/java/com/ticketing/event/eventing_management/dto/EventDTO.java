package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evento del catalogo (sincronizado desde WordPress)")
public class EventDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String venue;
    private Integer capacity;
    private String category;
    private String imageUrl;
    private Boolean active;
    private String status;
}
