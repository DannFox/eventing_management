# Docker — Grupo A Event Management

## Puertos (por defecto)

| Servicio | Puerto host | Uso |
|----------|-------------|-----|
| API Spring Boot | 8081 | REST, [Swagger UI](http://localhost:8081/swagger-ui.html) |
| WordPress | 8080 | Admin y tienda WooCommerce |
| MySQL eventos | 3307 | Base `eventdb` |
| Redis | 6379 | Cache |
| RabbitMQ AMQP | 5672 | Mensajes |
| RabbitMQ Management | 15672 | UI colas y exchanges |

Variables de entorno: copia [`.env.example`](.env.example) a `docker/.env` y completa las claves WooCommerce.

## Opcion A — Stack completo (recomendado para entrega Final)

Un solo archivo compose con infraestructura + aplicacion:

```powershell
copy docker\.env.example docker\.env
# Edita docker\.env (WOOCOMMERCE_CONSUMER_KEY / SECRET)

docker compose --env-file docker/.env -f docker/docker-compose.full.yml up -d --build
```

Apagado:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.full.yml down
```

## Opcion B — Solo infraestructura (desarrollo)

`docker-compose.yml` levanta MySQL, Redis, RabbitMQ y WordPress. La aplicacion Spring puede ejecutarse en la maquina host con `./mvnw spring-boot:run`.

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d
```

## Opcion C — Infra + app en Docker (dos archivos)

Equivalente a la opcion A pero usando los compose originales:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml -f docker/docker-compose.app.yml up -d --build
```

Apagado:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml -f docker/docker-compose.app.yml down
```

## Flujo E2E y demo

Ver [DEMO_CHECKLIST.md](DEMO_CHECKLIST.md) para sincronizacion WordPress, webhook WooCommerce, RabbitMQ y validacion de tickets (integracion con Grupo B).
