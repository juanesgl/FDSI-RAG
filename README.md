# Asistente SOC con IA (FDSI-RAG)

Asistente de clasificación y contención de incidentes de seguridad para un SOC, con triaje automático mediante IA (RAG + LLM) y aprobación humana (Human-in-the-Loop).

## Características

- **Triaje automático con IA**: clasifica alertas (tipo, severidad) usando RAG sobre los manuales internos del equipo.
- **Structured Outputs**: el LLM devuelve JSON estricto con acciones de contención específicas y ejecutables (`comandoEjecucion`, `sistemaAfectado`, `ipBloqueada`), no texto genérico.
- **Ingesta M2M desde SIEM**: webhook asíncrono (`POST /api/v1/webhook/alertas`) que recibe alertas de Wazuh/Splunk sin intervención humana.
- **Human-in-the-Loop**: ninguna acción se ejecuta automáticamente; el analista aprueba o rechaza la propuesta.
- **Trazabilidad SIEM → incidente**: se conservan el id y el timestamp originales de la alerta.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Maven |
| Arquitectura | Hexagonal (puertos y adaptadores) |
| Persistencia | PostgreSQL 16 + pgvector (Spring Data JPA) |
| IA | Spring AI 2.0.0 (OpenAI: `gpt-4o-mini`, `text-embedding-3-small`) |
| Config | spring-dotenv 5.1.0 (lee `.env`) |

## Requisitos

- Java 21
- Docker (para la BD y/o la app)
- Una API key de OpenAI

## Configuración

1. Crear `.env` en la raíz (no se sube a git):

```bash
OPENAI_API_KEY=sk-...
# Opcional: secret para el webhook SIEM. Si se define, el webhook exige el header X-Webhook-Secret.
WEBHOOK_SECRET=
```

2. Variables de BD (opcionales, con defaults de desarrollo):

| Variable | Default |
|---|---|
| `ASISTENTE_DB_URL` | `jdbc:postgresql://localhost:5432/asistente_soc_db` |
| `ASISTENTE_DB_USERNAME` | `postgres` |
| `ASISTENTE_DB_PASSWORD` | `root` |

## Puesta en marcha

### Opción A: Docker (BD + app)

```bash
docker compose up -d --build
# App en http://localhost:8080
```

### Opción B: Local (BD en Docker, app con Maven)

```bash
docker compose up -d db
./mvnw spring-boot:run
```

Al arrancar, la app ingesta los manuales de `src/main/resources/manuals/*.md` al vector store (solo si está vacío).

## API

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/alertas` | Triaje síncrono: recibe la alerta, clasifica con IA y devuelve el incidente completo |
| `POST` | `/api/v1/webhook/alertas` | Ingesta M2M asíncrona: responde `202` con el id y tria en background (~10-15s) |
| `PUT` | `/api/v1/alertas/{id}/decision?aprobado=true\|false` | Registra la decisión humana |
| `GET` | `/swagger-ui.html` | Documentación interactiva (OpenAPI) |

### Ejemplo: triaje síncrono

```bash
curl -s -X POST http://localhost:8080/api/v1/alertas \
  -H "Content-Type: application/json" -d '{
  "id": "esc-01-alert-001",
  "timestamp": "2026-09-24T12:00:00-05:00",
  "rule": {"id": "5710", "description": "Phishing email detected", "level": 10, "groups": ["email"], "firedtimes": 1},
  "agent": {"id": "001", "name": "srv-mail-01", "ip": "192.168.10.20"},
  "data": {"dstuser": "jperez", "srcip": "45.83.12.7", "link": "http://bit.ly/xyz"},
  "full_log": "Phishing email with suspicious link",
  "location": "mail"
}'
```

### Ejemplo: webhook M2M (asíncrono)

```bash
curl -s -X POST http://localhost:8080/api/v1/webhook/alertas \
  -H "Content-Type: application/json" -d '{...mismo payload...}'
# → {"id":"<uuid>","estado":"EN_PROCESAMIENTO"}  (el triaje corre en background)
```

## Tests

```bash
./mvnw test
```

Los tests usan H2 y mocks — **no requieren API key ni llamadas a OpenAI**.

## Estructura

```
src/main/java/co/edu/eci/asistente/asistente_soc/
├── domain/          # Modelo y puertos (Java puro, sin Spring)
│   ├── model/       # Incidente, EstadoIncidente
│   ├── ports/       # ClasificarIncidenteUseCase, AsistenteIaPort, IncidenteRepositoryPort
│   └── exception/   # IncidenteNoEncontradoException
├── application/     # IncidenteService (orquestación)
└── infrastructure/  # Adaptadores
    ├── in/          # REST: AlertaController, WebhookAlertaController, DTOs, mappers
    └── out/         # ia/ (RAG+LLM), persistence/ (JPA)
```

## Documentación

- `docs/frontend-soc-dashboard.md` — plan de referencia para el futuro frontend
- `src/main/resources/manuals/` — base de conocimiento del RAG (políticas y procedimientos del SOC)

## Notas

- **Human-in-the-Loop**: el asistente propone, las personas deciden. Ninguna acción de contención se ejecuta automáticamente sobre sistemas reales (ver `manuals/aprobacion-y-escalamiento.md`).
- **Rama de trabajo**: `feature/spring-dotenv` (implementación de las 4 misiones del jurado).