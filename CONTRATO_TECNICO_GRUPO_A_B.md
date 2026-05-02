# Contrato Tecnico de Integracion - Grupo A y Grupo B

## 1) Objetivo
Definir el contrato de integracion entre:
- Grupo A (Event Management): productor de eventos de negocio de tickets y exposicion de catalogo/capacidad.
- Grupo B (Attendee Experience): consumidor de eventos y cliente REST de consulta.

Este documento aplica al Avance 2 y sirve como base para la entrega final.

## 2) Version del contrato
- `contractVersion`: `1.1.0` (identificador de evento unificado como UUID en API y mensajes)
- Fecha efectiva: `2026-05-02`
- Compatibilidad: cambios breaking deben crear nueva version mayor.

## 3) Convenciones generales
- Encoding: `UTF-8`
- Fechas/hora: `ISO-8601` (ejemplo: `2026-05-30T18:00:00`)
- Identificador de evento (`eventId`): **UUID v4** (RFC 4122), representado en JSON y URLs como **string** canonica (ej. `a1b2c3d4-e5f6-7890-abcd-ef1234567890`).
- Correlation recomendada: incluir `ticketId` y `eventId` en logs de ambos grupos.
- Idempotencia: Grupo B debe procesar eventos de RabbitMQ de forma idempotente (pueden llegar duplicados).
- Trazabilidad: registrar `routingKey`, timestamp de recepcion y resultado del procesamiento.

---

## 4) Contrato RabbitMQ

### 4.1 Infraestructura de mensajeria
- Exchange: `tickets.exchange`
- Tipo de exchange: `topic`
- Routing keys:
  - `ticket.sold`
  - `ticket.validated`

### 4.2 Evento `ticket.sold` (Grupo A -> Grupo B)
Evento emitido cuando una compra confirmada genera ticket(s).

Campos requeridos:
- `ticketId` (string UUID) - requerido
- `attendeeId` (string UUID) - requerido
- `attendeeEmail` (string email) - requerido
- `attendeeName` (string) - requerido
- `eventId` (string UUID) - requerido
- `eventName` (string) - requerido
- `eventDate` (string ISO-8601) - requerido
- `venue` (string) - requerido
- `qrCode` (string base64) - requerido
- `seatInfo` (string o null) - opcional

Ejemplo:
```json
{
  "ticketId": "8f6cf3a7-6d53-4b4d-9af8-c0f5f18f2812",
  "attendeeId": "f4d83a9e-4fbf-3f65-a4a0-35abf30f9f02",
  "attendeeEmail": "ana@example.com",
  "attendeeName": "Ana Perez",
  "eventId": "c2d4e6f8-0a1b-2c3d-4e5f-67890abcdef0",
  "eventName": "Festival de Jazz 2026",
  "eventDate": "2026-06-15T19:30:00",
  "venue": "Teatro Nacional",
  "qrCode": "OGY2Y2YzYTctNmQ1My00YjRkLTlhZjgtYzBmNWYxOGYyODEy",
  "seatInfo": null
}
```

Notas:
- `qrCode` codifica el identificador del ticket segun lo acordado con Grupo A (base64 del UUID del ticket).

### 4.3 Evento `ticket.validated` (Grupo A -> Grupo B)
Evento emitido cuando un ticket pasa de `ACTIVE` a `USED` al ingreso.

Campos requeridos:
- `ticketId` (string UUID) - requerido
- `eventId` (string UUID) - requerido
- `validatedAt` (string ISO-8601) - requerido

Ejemplo:
```json
{
  "ticketId": "8f6cf3a7-6d53-4b4d-9af8-c0f5f18f2812",
  "eventId": "c2d4e6f8-0a1b-2c3d-4e5f-67890abcdef0",
  "validatedAt": "2026-06-15T18:45:10"
}
```

### 4.4 Reglas de consumo (Grupo B)
- Confirmar mensaje (ack) solo despues de procesar correctamente.
- Si falla procesamiento por datos temporales (servicio caido, timeout), reintentar.
- Si falla por payload invalido no recuperable, enviar a DLQ o registrar como descartado con evidencia.
- Prevenir reprocesamiento por `ticketId` + `routingKey` (idempotencia).

---

## 5) Contrato REST (Grupo B consume API de Grupo A)

Base URL (ejemplo): `http://event-management:8081`

Los segmentos `{eventId}` en path son **UUID** del evento (mismo valor que `id` en el catalogo).

### 5.1 Catalogo de eventos
`GET /api/events?page=0&size=20&category=MUSIC&date=2026-06`

Uso:
- Mostrar eventos disponibles en app de asistentes.
- Filtrar por categoria/mes y paginar resultados.

Respuesta esperada:
- Lista de eventos con metadata de paginacion.
- Campos minimos por evento: `id` (UUID), `title`, `eventDate`, `venue`, `category`, `active`, `status`.

### 5.2 Capacidad/aforo por evento
`GET /api/events/{eventId}/capacity`

Ejemplo: `GET /api/events/c2d4e6f8-0a1b-2c3d-4e5f-67890abcdef0/capacity`

Respuesta esperada:
```json
{
  "eventId": "c2d4e6f8-0a1b-2c3d-4e5f-67890abcdef0",
  "sold": 180,
  "available": 20,
  "total": 200
}
```

Uso:
- Mostrar disponibilidad en tiempo real al asistente.

### 5.3 Asistencia (dashboard)
`GET /api/events/{eventId}/attendance`

### 5.4 Verificacion de ticket para wallet
`GET /api/tickets/{ticketId}`

Respuesta esperada (campos minimos):
- `ticketId` (UUID)
- `eventId` (UUID)
- `eventName`
- `attendeeId` (UUID)
- `attendeeEmail`
- `status` (`ACTIVE | USED | CANCELLED`)
- `qrCode`
- `validatedAt` (si aplica)

Uso:
- Sincronizar estado real del ticket mostrado en wallet.

### 5.5 Validacion de QR (operacion de staff, no wallet)
`POST /api/tickets/validate`

Body soportado:
```json
{
  "ticketId": "8f6cf3a7-6d53-4b4d-9af8-c0f5f18f2812",
  "qrCode": null,
  "eventId": "c2d4e6f8-0a1b-2c3d-4e5f-67890abcdef0"
}
```

Reglas:
- Enviar `ticketId` o `qrCode`.
- Si valida por primera vez: status cambia a `USED` y se publica `ticket.validated`.
- Si ya estaba validado: responder informando estado actual (no duplicar validacion).

---

## 6) Codigos de error y manejo recomendado

Errores esperables de API Grupo A:
- `400 Bad Request`: payload invalido o datos faltantes.
- `404 Not Found`: ticket/evento no existe.
- `409 Conflict`: regla de negocio incumplida (ej. aforo completo, ticket cancelado).
- `500 Internal Server Error`: fallo inesperado.

Recomendacion Grupo B:
- Reintentar solo en `5xx` o fallos de red.
- No reintentar en `4xx` sin correccion de datos.

---

## 7) Migracion de base de datos (Grupo A)

Al pasar la clave primaria de `events` de entero a **UUID**, una base ya creada con el esquema anterior puede requerir:
- eliminar tablas `tickets` y `events` y dejar que Hibernate las recree (`spring.jpa.hibernate.ddl-auto=update` puede no alterar el tipo de PK de forma segura en todos los casos), **o**
- script SQL manual de migracion.

En entornos de desarrollo suele bastar con **vaciar** el volumen MySQL o borrar el esquema y volver a sincronizar desde WordPress.

---

## 8) Casos de prueba de integracion minimos

1. Compra confirmada en WooCommerce:
- Grupo A crea ticket y emite `ticket.sold`.
- Grupo B recibe y registra envio de entrada digital.

2. Validacion de ingreso:
- Staff valida ticket.
- Grupo A emite `ticket.validated`.
- Grupo B actualiza estadisticas/dashboard.

3. Verificacion wallet:
- Grupo B consulta `GET /api/tickets/{ticketId}` y refleja `ACTIVE/USED/CANCELLED`.

4. Evento duplicado en cola:
- Grupo B recibe `ticket.sold` duplicado.
- Debe evitar doble envio de correo/doble registro.

---

## 9) Acuerdos pendientes por cerrar en reunion A-B

- Confirmar politica de DLQ/reintentos en entorno Docker final.
- Confirmar inclusion futura de `schemaVersion` y `occurredAt` en cada evento.
- Definir SLA de disponibilidad de endpoints REST entre servicios.

---

## 10) Estado actual de implementacion en Grupo A (referencia)
- PK de evento: **UUID** (`events.id`).
- Publicacion `ticket.sold`: implementada (`eventId` como UUID string).
- Publicacion `ticket.validated`: implementada (`eventId` como UUID string).
- Endpoint `GET /api/events/{id}/attendance`: implementado (`id` UUID).
- Endpoint `POST /api/tickets/validate`: implementado (`eventId` opcional UUID en body).
- Docker Compose con MySQL, Redis, RabbitMQ, WordPress y app: disponible.
