# Frontend SOC Dashboard — Plan de Referencia

> Documento de planificación para el futuro frontend del Asistente SOC.
> El backend está completo y validado E2E con key real; este documento define cómo consumirlo
> y qué falta antes de empezar el front.

---

## 1. Contexto

| | |
|---|---|
| **Proyecto** | Asistente SOC con IA (FDSI-RAG) |
| **Backend** | Spring Boot 4.1.1, Java 21, arquitectura hexagonal, RAG + LLM (OpenAI `gpt-4o-mini` / `text-embedding-3-small`), pgvector |
| **Estado actual** | Backend funcional y validado E2E: triaje síncrono, ingesta M2M asíncrona desde SIEM, decisión humana, Structured Outputs con comandos ejecutables |
| **Objetivo del front** | Dashboard de operaciones: visualizar incidentes, resultados del triaje IA y tomar decisiones humanas (aprobar/rechazar) |

**Lo que el backend ya produce** (no es texto libre): `tipo`, `severidad`, `accionesRecomendadas` estructuradas, `comandoEjecucion`, `sistemaAfectado`, `ipBloqueada`, `idAlertaOrigen`, `timestampOrigen` — todo persistido en columnas queryables.

---

## 2. Contrato de API (lo que consume el front)

### Endpoints existentes

| Método | Ruta | Códigos | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/alertas` | 201, 400 | Triaje **síncrono**: recibe alerta, clasifica con IA, devuelve el incidente completo |
| `POST` | `/api/v1/webhook/alertas` | 202, 400, 401 | Ingesta **M2M asíncrona**: acepta la alerta, triaje en background (~10-15s) |
| `PUT` | `/api/v1/alertas/{incidenteId}/decision?aprobado=true\|false` | 200, 404 | Registra la decisión humana |
| `GET` | `/v3/api-docs` | 200 | OpenAPI JSON → generación de cliente tipado |
| `GET` | `/swagger-ui.html` | 200 | Documentación interactiva |

### Estructura `IncidenteResponse`

```json
{
  "id": "4d9a24cb-897c-47a5-9b0f-68e3f6608b00",
  "fechaRecepcion": "2026-09-24T12:00:00",
  "descripcion": "Alerta de monitoreo recibida desde auth...",
  "sistema": "srv-web-01",
  "usuario": "root",
  "ip": "192.168.10.21",
  "impacto": null,
  "tipo": "ACCESO_NO_AUTORIZADO",
  "severidad": "ALTA",
  "accionPropuesta": "Justificacion...\n\nAcciones recomendadas:\n- [BLOQUEAR_IP] 203.0.113.9 -> iptables -A INPUT -s 203.0.113.9 -j DROP",
  "comandoEjecucion": null,
  "sistemaAfectado": "srv-web-01",
  "ipBloqueada": "203.0.113.9",
  "idAlertaOrigen": "esc-01-alert-003",
  "timestampOrigen": "2026-09-24T16:55:00",
  "estado": "ESPERANDO_APROBACION_HUMANA"
}
```

### Enums

| Campo | Valores |
|---|---|
| `tipo` | `PHISHING` \| `ACCESO_NO_AUTORIZADO` \| `MALWARE` \| `FUGA_DE_INFORMACION` \| `DENEGACION_DE_SERVICIO` \| `INFORMACION_INSUFICIENTE` \| `OTRO` |
| `severidad` | `BAJA` \| `MEDIA` \| `ALTA` \| `CRITICA` |
| `estado` | `NUEVO` \| `EN_TRIAJE_IA` \| `ESPERANDO_APROBACION_HUMANA` \| `APROBADO` \| `RECHAZADO` |

### Comportamientos que el front debe manejar

1. **El webhook es asíncrono**: responde `202` con `{id, estado:"EN_PROCESAMIENTO"}` y el triaje ocurre en background. El front debe hacer **polling** (cada 3-5s) hasta que `estado` salga de `EN_TRIAJE_IA`.
2. **El endpoint síncrono bloquea** hasta que el LLM responde (5-15s): el front debe mostrar un estado de carga y un timeout generoso (≥60s).
3. **Validación**: payload inválido → `400` con `ApiError`; secret incorrecto en webhook → `401`.
4. **`comandoEjecucion` / `ipBloqueada` pueden ser `null`** — el LLM no siempre los llena; la UI debe tolerar ausencia.

---

## 3. ⚠️ Gaps del backend que el front necesita (trabajo previo)

El front **no puede listar ni consultar incidentes** con la API actual. Antes de empezar el front (o en paralelo), agregar:

| Endpoint | Por qué |
|---|---|
| `GET /api/v1/alertas` | Listado con filtros (`estado`, `severidad`, `tipo`) + paginación — la pantalla principal del dashboard |
| `GET /api/v1/alertas/{incidenteId}` | Detalle individual (para el polling del webhook y deep-links) |
| *(opcional)* `GET /api/v1/alertas/{id}/historial` | Auditoría de cambios de estado (hoy solo se guarda el último estado) |

> Nota: hoy el polling del webhook solo puede resolverse consultando la BD directamente (`psql`), lo cual no es viable para un front. El `GET /{id}` es **requisito** para el flujo M2M.

---

## 4. Stack propuesto

```json
{
  "framework": "React 18 + Vite",
  "language": "TypeScript (strict)",
  "data fetching": "TanStack Query (React Query)",
  "ui": "shadcn/ui + Tailwind CSS",
  "forms": "React Hook Form + Zod",
  "api client": "Orval (generado desde /v3/api-docs)",
  "routing": "React Router v7"
}
```

### Rationale

| Decisión | Por qué |
|---|---|
| **TanStack Query** | Caching, polling (`refetchInterval`), invalidación automática tras aprobar/rechazar, estados de carga/error declarativos |
| **Cliente generado con Orval** | Contrato único desde OpenAPI: si el backend cambia, el tipo TS cambia y el compilador avisa. Cero DTOs a mano |
| **shadcn/ui + Tailwind** | Componentes accesibles, copiar/pegar, sin lock-in de librería, fácil de estilizar para el equipo |
| **Zod** | Validación del formulario contra el mismo schema que el backend (400s no deberían existir desde la UI) |
| **React Router** | Rutas: `/` (tabla), `/incidentes/:id` (detalle), `/inyectar` (formulario) |

---

## 5. Pantallas y componentes

| Pantalla | Componentes | Datos |
|---|---|---|
| **`/` — Tabla de incidentes** | Tabla con badges de severidad/estado/tipo, filtros (estado, severidad, tipo), paginación, columna `idAlertaOrigen` | `GET /api/v1/alertas` |
| **`/incidentes/:id` — Detalle** | Tarjeta de descripción, campos estructurados (`ipBloqueada`, `comandoEjecucion`, `sistemaAfectado`), `accionPropuesta` formateada, botones **Aprobar/Rechazar** | `GET /api/v1/alertas/{id}` + `PUT /decision` |
| **`/inyectar` — Formulario** | Formulario de alerta (o textarea JSON crudo), selector síncrono vs webhook, muestra 201 (resultado) o 202 (link a detalle con polling) | `POST /api/v1/alertas` / `POST /webhook/alertas` |
| **Polling del webhook** | Hook `useIncidente(id)` con `refetchInterval: 3000` hasta `estado != EN_TRIAJE_IA` | `GET /api/v1/alertas/{id}` |

### Mapa de colores sugerido (severidad)

| Severidad | Badge |
|---|---|
| `CRITICA` | rojo sólido |
| `ALTA` | rojo/ámbar |
| `MEDIA` | ámbar |
| `BAJA` | verde |
| `INFORMACION_INSUFICIENTE` | gris |

---

## 6. Setup rápido (cuando se decida arrancar)

```bash
# 1. Scaffold
npm create vite@latest soc-dashboard -- --template react-ts
cd soc-dashboard
npm i @tanstack/react-query @hookform/resolvers zod lucide-react react-router-dom
npx shadcn@latest init

# 2. Cliente tipado desde el backend corriendo
npx orval --input http://localhost:8080/v3/api-docs --output ./src/api --client react-query

# 3. Proxy de dev (evita CORS)
# vite.config.ts → server.proxy: { "/api": "http://localhost:8080" }
```

> Si el backend corre en Docker (`docker compose up -d --build`), el front se conecta igual a `localhost:8080`.

---

## 7. Roadmap

| Fase | Alcance | Esfuerzo |
|---|---|---|
| **0. Backend gaps** | `GET /alertas` (listado+filtros+paginación), `GET /alertas/{id}` | ~1-2h |
| **1. Base** | Scaffold, cliente Orval, tabla de incidentes con badges y filtros | ~2-3h |
| **2. Detalle + decisión** | Vista detalle, botones Aprobar/Rechazar con invalidación de cache | ~1-2h |
| **3. Inyección + polling** | Formulario (sync + webhook), polling del 202 hasta resultado | ~2h |
| **4. Pulido** | Estados vacíos/error, skeleton loaders, responsive, dark mode | ~1-2h |
| **5. (Opcional) Auth** | JWT simple o Keycloak — requiere trabajo en backend | variable |

---

## 8. Fuera de alcance (por ahora)

| Feature | Motivo |
|---|---|
| **Autenticación/autorización** | El backend no tiene auth; requiere decisión de arquitectura (JWT vs Keycloak) |
| **Ejecución real de comandos** | El backend solo *propone* (`comandoEjecucion`); no hay integración firewall/EDR/AD. La política HITL del manual lo prohíbe sin cambio de manual |
| **Notificaciones push / WebSocket** | Requiere SSE/WebSocket en backend; hoy el polling es suficiente |
| **Auditoría completa** | El backend solo guarda el último estado; haría falta tabla de historial |

---

## 9. Referencia rápida (curls de prueba)

```bash
# Síncrono (201, resultado completo)
curl -s -m 60 -X POST http://localhost:8080/api/v1/alertas \
  -H "Content-Type: application/json" -d '{...payload Wazuh...}'

# Webhook M2M (202, triaje en background)
curl -s -X POST http://localhost:8080/api/v1/webhook/alertas \
  -H "Content-Type: application/json" -d '{...payload Wazuh...}'

# Decisión humana
curl -s -X PUT "http://localhost:8080/api/v1/alertas/<ID>/decision?aprobado=true"

# Documentación
xdg-open http://localhost:8080/swagger-ui.html
```

---

## 10. Decisiones pendientes (para cuando se arranque)

- [ ] ¿El front vive en este repo (`soc-dashboard/`) o en repo aparte?
- [ ] ¿Quién implementa los gaps del backend (Fase 0)?
- [ ] ¿Auth desde el inicio o post-MVP?
- [ ] ¿Dark mode por defecto (operaciones SOC suele ser dark)?
- [ ] ¿Idioma de la UI: español (consistente con el dominio) o inglés?