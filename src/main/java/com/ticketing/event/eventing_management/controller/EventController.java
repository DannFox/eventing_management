package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.AttendanceDTO;
import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.dto.EventListResponseDTO;
import com.ticketing.event.eventing_management.service.EventService;
import com.ticketing.event.eventing_management.service.WordPressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Sincronizar eventos desde WordPress")
    public ResponseEntity<Void> syncEvents() {
        wordPressService.syncEvents();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/capacity")
    @Operation(summary = "Consultar capacidad y aforo de un evento")
    public ResponseEntity<CapacityDTO> getEventCapacity(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventCapacity(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un evento")
    public ResponseEntity<EventDTO> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    // Endpoint de asistencia en tiempo real (siguiente hito)
    @GetMapping("/{id}/attendance")
    @Operation(summary = "Obtener datos de asistencia en tiempo real")
    public ResponseEntity<AttendanceDTO> getAttendance(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventAttendance(id));
    }
}
