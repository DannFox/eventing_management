package com.ticketing.event.eventing_management.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.service.WooCommerceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WooCommerceServiceImpl implements WooCommerceService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${woocommerce.api.url}")
    private String wooCommerceApiUrl;

    @Value("${woocommerce.consumer.key}")
    private String consumerKey;

    @Value("${woocommerce.consumer.secret}")
    private String consumerSecret;

    @Override
    public Long createProduct(Event event) {
        if (wooCommerceApiUrl == null || wooCommerceApiUrl.isBlank()
                || consumerKey == null || consumerKey.isBlank()
                || consumerSecret == null || consumerSecret.isBlank()) {
            log.error("Configuracion de WooCommerce incompleta. Verifica woocommerce.api.url, woocommerce.consumer.key y woocommerce.consumer.secret");
            return null;
        }

        String auth = consumerKey + ":" + consumerSecret;
        String encodeAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        RestClient restClient = restClientBuilder
                .baseUrl(wooCommerceApiUrl)
                .defaultHeader("Authorization", "Basic " + encodeAuth)
                .build();

        Map<String, Object> productData = new HashMap<>();
        productData.put("name", event.getTitle());
        productData.put("type", "simple");
        productData.put("regular_price", "10.00"); // Precio por defecto
        productData.put("description", event.getDescription());
        productData.put("short_description", "Ticket para " + event.getTitle());
        productData.put("manage_stock", true);
        productData.put("stock_quantity", event.getCapacity());
        productData.put("stock_status", "instock");

        try {
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/products")
                            // En entornos locales HTTP, WooCommerce suele aceptar mejor las credenciales por query params.
                            .queryParam("consumer_key", consumerKey)
                            .queryParam("consumer_secret", consumerSecret)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(productData)
                    .retrieve()
                    .body(String.class);

            JsonNode jsonResponse = objectMapper.readTree(response);
            Long productId = jsonResponse.get("id").asLong();

            log.info("Producto creado en WooCommerce: ID {} para evento {}", productId, event.getTitle());
            return productId;

        } catch (Exception e) {
            log.error("Error al crear producto en WooCommerce en URL {}: {}", wooCommerceApiUrl, e.getMessage());
            return null;
        }
    }
}
