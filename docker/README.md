# Flujo recomendado

`docker-compose.yml` levanta solo la infraestructura compartida para desarrollo:

- MySQL
- Redis
- RabbitMQ
- WordPress

La aplicacion Spring Boot debe correrse fuera de Docker durante desarrollo para evitar reconstruir la imagen en cada cambio.

## Desarrollo local

1. Levanta la infraestructura:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d
```

2. Ejecuta la aplicacion localmente desde la raiz del proyecto:

```powershell
./mvnw spring-boot:run
```

La configuracion de `src/main/resources/application.properties` ya apunta por defecto a `localhost` para MySQL, Redis y RabbitMQ.

## Todo en Docker

Si quieres correr tambien la aplicacion dentro de Docker, usa ambos archivos:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml -f docker/docker-compose.app.yml up -d --build
```

## Apagado

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml down
```
