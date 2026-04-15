package com.ticketing.event.eventing_management.controller;

import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.service.EventService;
import com.ticketing.event.eventing_management.service.WordPressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Catálogo y gestión de eventos")
public class EventController {
    private final EventService eventService;
    private final WordPressService wordPressService;

    @GetMapping
    @Operation(summary = "Listar eventos activos paginados")
    public ResponseEntity<Page<EventDTO>> getEvents(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getActiveEvents(category, pageable));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sincronizar eventos desde WordPress")
    public ResponseEntity<Void> syncEvents() {
        wordPressService.syncEvents();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/capacity")
    @Operation(summary = "Consultar capacidad y aforo de un evento")
    public ResponseEntity<CapacityDTO> getEventCapacity(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventCapacity(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un evento")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    // Endpoint de asistencia en tiempo real (siguiente hito)
    @GetMapping("/{id}/attendance")
    @Operation(summary = "Obtener datos de asistencia en tiempo real")
    public ResponseEntity<?> getAttendance(@PathVariable Long id) {
        // Implementación posterior
        return ResponseEntity.ok().build();
    }
}
