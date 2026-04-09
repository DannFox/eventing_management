package com.ticketing.event.eventing_management.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.service.WordPressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordPressServiceImplementation implements WordPressService {

    private final RestClient.Builder restClientBuilder;
    private final EventRepository eventRepository;

    @Value("${wordpress.api.base-url}")
    private String wordpressBaseUrl;

    @Override
    @CacheEvict(value = "events", allEntries = true)
    public void syncEvents() {
        RestClient restClient = restClientBuilder.baseUrl(wordpressBaseUrl).build();

        JsonNode response = restClient.get()
                .uri("/events?per_page=100")
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.isArray()) {
            log.warn("La respuesta de WordPress no contiene una lista de eventos");
            return;
        }

        for (JsonNode jsonEvent : response) {
            processEvent(jsonEvent);
        }
    }

    private void processEvent(JsonNode jsonEvent) {
        try {
            JsonNode acfNode = jsonEvent.path("acf");

            String wpId = jsonEvent.path("id").asText(null);
            String title = jsonEvent.path("title").path("rendered").asText("");
            String description = jsonEvent.path("content").path("rendered").asText("");
            LocalDateTime eventDate = parseEventDate(acfNode.path("event_date").asText(null));
            String venue = acfNode.path("venue").asText("");
            int capacity = acfNode.path("capacity").asInt(0);
            String category = extractCategory(jsonEvent);
            String imageUrl = jsonEvent.path("featured_media_url").asText("");

            if (wpId == null || wpId.isBlank()) {
                log.warn("Evento omitido: id de WordPress ausente");
                return;
            }

            if (eventDate == null) {
                log.warn("Evento omitido {}: fecha invalida o ausente", wpId);
                return;
            }

            Event event = eventRepository.findByWpId(wpId)
                    .orElse(Event.builder().wpId(wpId).build());

            event.setTitle(title);
            event.setDescription(description);
            event.setEventDate(eventDate);
            event.setVenue(venue);
            event.setCapacity(capacity);
            event.setCategory(category);
            event.setImageUrl(imageUrl);
            event.setActive(true);

            eventRepository.save(event);
            log.info("Evento sincronizado: {}", title);
        } catch (Exception e) {
            log.error("Error procesando evento de WordPress: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime parseEventDate(String eventDateStr) {
        if (eventDateStr == null || eventDateStr.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(eventDateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            log.warn("No se pudo parsear la fecha del evento: {}", eventDateStr);
            return null;
        }
    }

    private String extractCategory(JsonNode jsonEvent) {
        JsonNode categoriesNode = jsonEvent.path("categories");
        if (!categoriesNode.isArray() || categoriesNode.isEmpty()) {
            return "";
        }

        JsonNode firstCategory = categoriesNode.get(0);
        if (firstCategory == null) {
            return "";
        }

        if (firstCategory.isObject()) {
            return firstCategory.path("name").asText("");
        }

        return firstCategory.asText("");
    }
}
