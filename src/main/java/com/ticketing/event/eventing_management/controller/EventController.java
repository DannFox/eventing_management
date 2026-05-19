package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.ApiErrorResponseDTO;
import com.ticketing.event.eventing_management.dto.AttendanceDTO;
import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.dto.EventListResponseDTO;
import com.ticketing.event.eventing_management.service.EventService;
import com.ticketing.event.eventing_management.service.WordPressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Catálogo y gestión de eventos")
public class EventController {
    private final EventService eventService;
    private final WordPressService wordPressService;

    @GetMapping
    @Operation(
            summary = "Listar eventos activos paginados",
            description = "Regla de filtros: si se envia date (YYYY-MM), se lista todo ese mes (incluye eventos pasados y futuros del mes). "
                    + "Si no se envia date, includePast=false excluye eventos pasados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista paginada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventListResponseDTO.class),
                            examples = @ExampleObject(name = "Eventos encontrados", value = """
                                    {
                                      "events": [
                                        {
                                          "id": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                          "title": "Concierto de verano",
                                          "description": "Evento musical en vivo",
                                          "eventDate": "2026-06-15T20:00:00",
                                          "venue": "Teatro Nacional",
                                          "capacity": 300,
                                          "category": "MUSIC",
                                          "imageUrl": "https://example.com/evento.jpg",
                                          "active": true,
                                          "status": "ACTIVE"
                                        }
                                      ],
                                      "pagination": {
                                        "page": 0,
                                        "size": 20,
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "elementsOnPage": 1,
                                        "first": true,
                                        "last": true,
                                        "sort": "eventDate: ASC"
                                      },
                                      "filters": {
                                        "category": "MUSIC",
                                        "date": "2026-06",
                                        "includePast": false,
                                        "includeInactive": false
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Filtro date con formato invalido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Filtro invalido", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 400,
                                      "error": "Solicitud invalida",
                                      "message": "El parametro date debe tener formato YYYY-MM"
                                    }
                                    """)))
    })
    public ResponseEntity<EventListResponseDTO> getEvents(
            @Parameter(description = "Categoria del evento. Ejemplo: MUSIC")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filtro por mes en formato YYYY-MM. Ejemplo: 2026-04")
            @RequestParam(required = false) String date,
            @Parameter(description = "Incluye eventos pasados cuando no se envia date. Default: false")
            @RequestParam(defaultValue = "false") boolean includePast,
            @Parameter(description = "Incluye eventos inactivos. Default: false")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getEvents(category, date, includePast, includeInactive, pageable));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sincronizar eventos desde WordPress",
            description = "Lee el CPT de eventos via REST y actualiza productos WooCommerce. Invalida cache Redis de eventos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Sincronizacion iniciada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "Sincronizacion aceptada", value = """
                                    {
                                      "status": "accepted",
                                      "message": "Sincronizacion de eventos iniciada"
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "WordPress no disponible o configuracion invalida (IllegalStateException)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Conflicto", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 409,
                                      "error": "Conflicto de estado",
                                      "message": "WordPress no disponible"
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
                                      "message": "Error inesperado sincronizando eventos"
                                    }
                                    """)))
    })
    public ResponseEntity<Map<String, String>> syncEvents() {
        wordPressService.syncEvents();
        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "message", "Sincronizacion de eventos iniciada"));
    }

    @GetMapping("/{id}/capacity")
    @Operation(summary = "Consultar capacidad y aforo de un evento",
            description = "Expuesto para Grupo B: vendidos, disponibles y cupo total.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Capacidad del evento",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CapacityDTO.class),
                            examples = @ExampleObject(name = "Capacidad disponible", value = """
                                    {
                                      "eventId": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                      "sold": 120,
                                      "available": 180,
                                      "total": 300
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Evento no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Evento no encontrado", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 404,
                                      "error": "Recurso no encontrado",
                                      "message": "Evento no encontrado"
                                    }
                                    """)))
    })
    public ResponseEntity<CapacityDTO> getEventCapacity(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventCapacity(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle del evento",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventDTO.class),
                            examples = @ExampleObject(name = "Evento encontrado", value = """
                                    {
                                      "id": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                      "title": "Concierto de verano",
                                      "description": "Evento musical en vivo",
                                      "eventDate": "2026-06-15T20:00:00",
                                      "venue": "Teatro Nacional",
                                      "capacity": 300,
                                      "category": "MUSIC",
                                      "imageUrl": "https://example.com/evento.jpg",
                                      "active": true,
                                      "status": "ACTIVE"
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Evento no encontrado", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 404,
                                      "error": "Recurso no encontrado",
                                      "message": "Evento no encontrado"
                                    }
                                    """)))
    })
    public ResponseEntity<EventDTO> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/{id}/attendance")
    @Operation(summary = "Obtener datos de asistencia en tiempo real",
            description = "Tickets vendidos vs validados y porcentaje de ocupacion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metricas de asistencia",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttendanceDTO.class),
                            examples = @ExampleObject(name = "Asistencia actual", value = """
                                    {
                                      "eventId": "7e9b7a2a-77ce-4b67-93f8-14f8b2dcf7d2",
                                      "eventName": "Concierto de verano",
                                      "sold": 120,
                                      "validated": 85,
                                      "available": 180,
                                      "total": 300,
                                      "occupancyPercentage": 28.33
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class),
                            examples = @ExampleObject(name = "Evento no encontrado", value = """
                                    {
                                      "timestamp": "2026-05-18T22:15:30",
                                      "status": 404,
                                      "error": "Recurso no encontrado",
                                      "message": "Evento no encontrado"
                                    }
                                    """)))
    })
    public ResponseEntity<AttendanceDTO> getAttendance(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventAttendance(id));
    }
}
