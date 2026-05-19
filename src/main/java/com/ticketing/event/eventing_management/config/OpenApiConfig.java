package com.ticketing.event.eventing_management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Management API — Grupo A")
                        .description("Catalogo (WordPress), venta WooCommerce, control de aforo (Redis), tickets y publicacion "
                                + "RabbitMQ (routing: ticket.sold, ticket.validated en exchange tickets.exchange). "
                                + "Contrato de integracion con Grupo B: ver CONTRATO_TECNICO_GRUPO_A_B.md en la raiz del proyecto.")
                        .version("1.0.0")
                        .contact(new Contact().name("Grupo A — Event Management")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Desarrollo local / Docker (puerto publicado)")));
    }
}
