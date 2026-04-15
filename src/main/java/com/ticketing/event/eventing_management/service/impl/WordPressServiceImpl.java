package com.ticketing.event.eventing_management.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.service.WooCommerceService;
import com.ticketing.event.eventing_management.service.WordPressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordPressServiceImpl implements WordPressService {
    private static final List<DateTimeFormatter> EVENT_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private final RestClient.Builder restClientBuilder;
    private final EventRepository eventRepository;
    private final WooCommerceService wooCommerceService;

    @Value("${wordpress.api.base-url}")
    private String wordpressBaseUrl;

    @Override
    @CacheEvict(value = "events", allEntries = true)
    public void syncEvents() {
        String eventsEndpoint = buildEventsEndpoint();
        RestClient restClient = restClientBuilder.build();

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(eventsEndpoint)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "No se pudo consultar WordPress en \"" + eventsEndpoint + "\". " +
                            "Verifica WORDPRESS_API_URL y que el host sea accesible desde donde corre la API.",
                    ex
            );
        }

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

            if (event.getWooProductId() == null) {
                Long wooProductId = wooCommerceService.createProduct(event);
                event.setWooProductId(wooProductId);
            }

            eventRepository.save(event);
            log.info("Evento sincronizado: {}", title);
        } catch (Exception e) {
            log.error("Error procesando evento de WordPress: {}", e.getMessage(), e);
        }
    }

    private String buildEventsEndpoint() {
        if (wordpressBaseUrl == null || wordpressBaseUrl.isBlank()) {
            throw new IllegalStateException("La propiedad wordpress.api.base-url no está configurada");
        }

        String url = wordpressBaseUrl.trim();
        // Elimina la barra final si existe
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Asegura que termine en /events según tu configuración de CPT UI
        if (!url.endsWith("/events")) {
            url += "/events";
        }

        return url + "?per_page=100";
    }

    private LocalDateTime parseEventDate(String eventDateStr) {
        if (eventDateStr == null || eventDateStr.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : EVENT_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(eventDateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Intentamos con el siguiente formato soportado.
            }
        }

        log.warn("No se pudo parsear la fecha del evento: {}", eventDateStr);
        return null;
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
