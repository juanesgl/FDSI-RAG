# Contrato de alertas simuladas (subconjunto Wazuh)

**Proyecto:** Asistente de Respuesta a Incidentes — Clasificación y Contención Asistida por IA
**Versión del contrato:** 1.0
**Responsable del contrato y de los datos de prueba:** Julián Santiago Ramírez Urueña
**Consumidor principal:** adaptador REST de entrada (Juan Diego Valderrama Gaviria)

---

## 1. Propósito y alcance

Este documento define el formato JSON de las alertas **simuladas** que alimentan el backend y la regla exacta para transformarlas al modelo de dominio actual `Incidente` (`domain/model/Incidente.java`).

- Las alertas son un **subconjunto simplificado** del formato de alertas de Wazuh. No son alertas reales ni se obtienen de un Wazuh en producción.
- El contrato **no agrega campos al dominio**. `Incidente` solo recibe cinco campos de entrada: `descripcion`, `ip`, `usuario`, `sistema` e `impacto`. Todo lo demás de la alerta se transmite dentro de `descripcion` o se descarta.
- El sistema trabaja en un entorno controlado. Ningún dato de este contrato desencadena acciones reales sobre la infraestructura; la salida del sistema siempre es una **propuesta** sujeta a aprobación humana.

## 2. Decisiones de diseño

| # | Decisión | Motivo |
|---|---|---|
| D1 | `Incidente.ip` representa la **IP del activo afectado** (`agent.ip`), no necesariamente el origen de la amenaza. | El prompt del `AsistenteIaAdapter` etiqueta este campo como "IP afectada". Las IPs de origen o destino externo (`srcip`, `dstip`) viajan dentro de `descripcion`. |
| D2 | `Incidente.impacto` proviene **únicamente** de `data.impacto_reportado`. | Wazuh no tiene un campo de impacto. Derivarlo de `rule.level` filtraría la severidad esperada al modelo. |
| D3 | `rule.level`, `rule.groups` y `rule.mitre` **no se usan** para determinar la severidad ni se envían al modelo. | Son etiquetas o proxies de la clasificación; incluirlos contamina la evaluación. |
| D4 | Todas las reglas usan IDs del rango **100000+**, que son **simulados**. | Es el rango de reglas personalizadas de Wazuh; evita atribuir comportamiento a reglas nativas no verificadas. |
| D5 | El phishing se representa como una alerta con regla simulada del gateway de correo. | Mantiene un flujo de simulación homogéneo para los 4 escenarios. |
| D6 | `timestamp` usa ISO-8601 **con dos puntos** en el offset (`-05:00`). | Wazuh real emite `-0500`; el formato con dos puntos se interpreta directamente con `OffsetDateTime`. |
| D7 | `data` es un objeto **plano**. | Serialización simple y determinística a texto. |
| D8 | La prioridad **no** forma parte de la alerta ni del dominio: se deriva de la severidad (ver `clasificacion-severidad-prioridad.md`). | Decisión del equipo para el MVP. |

## 3. Estructura general

```text
AlertaSimulada
├── id              string        OBLIGATORIO
├── timestamp       string        OBLIGATORIO  (ISO-8601 con offset)
├── rule            objeto        OBLIGATORIO
│   ├── id          string        OBLIGATORIO  (100000+, simulado)
│   ├── level       entero 0–15   OBLIGATORIO  (NO se mapea)
│   ├── description string        OBLIGATORIO  (se mapea a descripcion)
│   ├── groups      string[]      opcional     (NO se mapea)
│   ├── mitre       objeto        opcional     (NO se mapea)
│   │   ├── id          string[]
│   │   └── technique   string[]
│   └── firedtimes  entero        opcional     (NO se mapea)
├── agent           objeto        opcional
│   ├── id          string        obligatorio si existe agent
│   ├── name        string        obligatorio si existe agent
│   └── ip          string        opcional
├── data            objeto plano  opcional
├── full_log        string        OBLIGATORIO
└── location        string        OBLIGATORIO
```

Codificación: UTF-8. Content-Type: `application/json`.

## 4. Campos

### 4.1 Campos raíz

| Campo | Obligatorio | Tipo | Descripción | ¿Llega al `Incidente`? |
|---|---|---|---|---|
| `id` | Sí | string no vacío | Identificador de la alerta al estilo Wazuh (`"<epoch>.<n>"`). **No debe revelar el escenario ni el tipo.** | No |
| `timestamp` | Sí | string ISO-8601 con offset | Momento en que ocurrió el evento. | Sí, como texto dentro de `descripcion` |
| `rule` | Sí | objeto | Regla simulada que produjo la alerta. | Parcialmente |
| `agent` | No | objeto | Activo afectado. Se omite cuando no se conoce. | Sí (`sistema`, `ip`) |
| `data` | No | objeto plano | Campos decodificados del evento. | Sí (`usuario`, `impacto`, `descripcion`) |
| `full_log` | Sí | string no vacío | Línea de log o texto original del evento. | Sí, dentro de `descripcion` |
| `location` | Sí | string no vacío | Fuente del evento: `mail-gateway`, `/var/log/secure`, `edr`, `mesa-de-ayuda`. | Sí, dentro de `descripcion` |

### 4.2 `rule`

| Campo | Obligatorio | Tipo | Uso |
|---|---|---|---|
| `id` | Sí | string | Solo trazabilidad. IDs 100000+ **simulados**. |
| `level` | Sí | entero 0–15 | Solo realismo. **Nunca** se mapea ni se usa para calcular la severidad. |
| `description` | Sí | string no vacío | Descripción **observacional** del evento. Se incluye en `descripcion`. |
| `groups` | No | string[] | **No se mapea.** |
| `mitre.id`, `mitre.technique` | No | string[] | **No se mapean.** |
| `firedtimes` | No | entero | **No se mapea.** |

Reglas simuladas usadas en los escenarios:

| `rule.id` | Fuente | Evento representado |
|---|---|---|
| `100110` | `mail-gateway` | Correo reportado por un usuario |
| `100210` | `/var/log/secure` | Fallos de autenticación SSH repetidos |
| `100310` | `edr` | Ejecutable detectado por firma y conexión saliente |
| `100410` | `mesa-de-ayuda` | Reporte manual sin categorizar |

### 4.3 `agent`

| Campo | Obligatorio (si hay `agent`) | Tipo | Uso |
|---|---|---|---|
| `id` | Sí | string | Si vale `"000"` (manager de Wazuh), el agente se considera **ausente** para el mapeo. |
| `name` | Sí | string | Se mapea a `Incidente.sistema`. |
| `ip` | No | string (IPv4) | Se mapea a `Incidente.ip`: **IP del activo afectado**. |

### 4.4 `data`

Reglas estructurales:

- Debe ser un objeto **plano**. Los valores permitidos son string, número entero, booleano o arreglo de strings.
- Los objetos anidados **no se permiten**. Una alerta con objetos anidados en `data` se rechaza.
- Los valores `null` o strings vacíos se consideran ausentes.
- Las claves estándar de Wazuh se mantienen en inglés (`srcip`, `dstip`, `srcuser`, `dstuser`, `dstport`). Las claves personalizadas van en español `snake_case`.
- Se admiten claves fuera del diccionario, pero deben respetar las reglas estructurales y las reglas anti-contaminación (§5).

Diccionario de claves:

| Clave | Tipo | Significado | Escenarios |
|---|---|---|---|
| `srcip` | string | IP de origen del evento (p. ej. el atacante) | 2 |
| `dstip` | string | IP de destino externo (p. ej. un servidor remoto contactado) | 3 |
| `dstport` | entero | Puerto de destino | 3 |
| `srcuser` | string | Usuario que origina la acción | — (alternativa para `usuario`) |
| `dstuser` | string | Cuenta o usuario afectado | 1, 2, 3 |
| `remitente` | string | Dirección del remitente del correo | 1 |
| `asunto` | string | Asunto del correo | 1 |
| `url` | string | Enlace contenido en el correo | 1 |
| `adjunto` | string | Nombre del adjunto, si existe | — |
| `destinatarios` | entero | Número de destinatarios del correo | 1 |
| `reenviado_internamente` | booleano | Si el correo se reenvió dentro de la organización | 1 |
| `accion_usuario` | string | Qué hizo el usuario con el correo | 1 |
| `tipo_cuenta` | string | Naturaleza de la cuenta (p. ej. `administrativa`, `estándar`) | 2 |
| `protocolo` | string | Protocolo de autenticación | 2 |
| `intentos_fallidos` | entero | Número de intentos fallidos observados | 2 |
| `ventana_minutos` | entero | Ventana de tiempo de los intentos | 2 |
| `inicio_sesion_exitoso` | booleano | Si hubo un inicio de sesión exitoso posterior | 2 |
| `archivo_ruta` | string | Ruta del archivo detectado | 3 |
| `sha256` | string | Hash del archivo (ficticio) | 3 |
| `nombre_deteccion` | string | Nombre de la firma detectada | 3 |
| `proceso_ejecutado` | booleano | Si el archivo llegó a ejecutarse | 3 |
| `contenido_por_edr` | booleano | Si el EDR detuvo el proceso | 3 |
| `dominio_destino` | string | Dominio contactado | 3 |
| `destino_en_lista_de_amenazas` | booleano | Si el destino figura en listas de amenazas | 3 |
| `cifrado_masivo_detectado` | booleano | Si se observó cifrado masivo de archivos | 3 |
| `equipos_adicionales_afectados` | entero | Otros equipos con la misma detección | 3 |
| `impacto_reportado` | string | Impacto observado o reportado, en lenguaje factual | 1, 3 |

## 5. Reglas anti-contaminación

El objetivo es que el modelo clasifique a partir de **hechos observables** y no de etiquetas que revelen la respuesta esperada.

1. **Tokens prohibidos** en `rule.description`, `full_log`, `location` y cualquier valor de `data`, sin importar mayúsculas o minúsculas:
   - Taxonomía de tipos: `PHISHING`, `ACCESO_NO_AUTORIZADO`, `MALWARE`, `FUGA_DE_INFORMACION`, `DENEGACION_DE_SERVICIO`, `INFORMACION_INSUFICIENTE`.
   - Severidades: `baja`, `media`, `alta`, `crítica` / `critica`, como palabras completas.
   - Prioridades: `P1`, `P2`, `P3`, `P4`.
2. **Campos que nunca se envían al modelo:** `id`, `rule.id`, `rule.level`, `rule.groups`, `rule.mitre`, `rule.firedtimes`.
3. **Vocabulario permitido:** términos que describen lo observado (correo, enlace, credenciales, autenticación fallida, hash, proceso, conexión saliente, cuenta administrativa, lista de amenazas). Es el vocabulario que el RAG necesita para recuperar el procedimiento correcto.
4. `impacto_reportado` describe **consecuencias observadas** ("el usuario nota lentitud"), nunca un nivel de severidad.
5. Los nombres de archivo de los escenarios (`esc-0X.json`) y los identificadores `ESC-0X` **nunca** viajan al backend.
6. Datos ficticios: las IPs externas usan rangos de documentación (RFC 5737: `203.0.113.0/24`, `198.51.100.0/24`), las internas usan `10.0.0.0/8`, los dominios usan `.example` y los hashes son ficticios.

Los 4 ejemplos de la §8 cumplen estas reglas sobre el resultado mapeado (verificado con búsqueda de palabras completas).

## 6. Mapeo alerta → `Incidente`

| Campo de `Incidente` | Regla | Si no hay dato |
|---|---|---|
| `descripcion` | Plantilla de la §7. | Nunca queda vacío (`rule.description`, `full_log` y `location` son obligatorios). |
| `ip` | `agent.ip` | `null` si falta `agent`, si `agent.id == "000"` o si falta `agent.ip`. **No** se usa `srcip` ni `dstip` como alternativa. |
| `usuario` | `data.dstuser`; si no existe, `data.srcuser` | `null` |
| `sistema` | `agent.name` | `null` si falta `agent` o si `agent.id == "000"` |
| `impacto` | `data.impacto_reportado` | `null`. **Nunca** se deriva de `rule.level` ni de otro campo. |
| `fechaRecepcion` | `LocalDateTime.now()` al recibir la petición | — (así mide el tiempo del sistema; el `timestamp` del evento va en `descripcion`) |
| `id`, `tipo`, `severidad`, `accionPropuesta`, `estado` | **No se mapean**; los asigna el sistema (persistencia, `IncidenteService`, `AsistenteIaAdapter`). | — |

Aclaraciones:

- **`Incidente.ip` es la IP del activo afectado**, no necesariamente el origen de la amenaza. En ESC-02 el atacante es `203.0.113.45` (`data.srcip`), pero `Incidente.ip` es `10.10.5.20`, el servidor atacado. La IP del atacante llega al modelo dentro de `descripcion`.
- Los campos `null` son válidos: `AsistenteIaAdapter` los presenta al modelo como "no especificado".
- `dstuser` y `srcuser` también aparecen en la línea "Datos observados" de `descripcion`. La duplicación es intencional.

## 7. Plantilla de `descripcion`

`AsistenteIaAdapter` hace la búsqueda por similitud **solo con `descripcion`**. Esta plantilla concentra en ese campo el contexto semántico necesario para recuperar el procedimiento correcto.

### 7.1 Plantilla exacta

```text
Alerta de monitoreo recibida desde {location}.
Evento detectado: {rule.description}.
Registro original: {full_log}
Datos observados: {data serializado}
Hora del evento: {timestamp}
```

### 7.2 Reglas de construcción

1. Las líneas se separan con `\n` (LF).
2. `location`, `rule.description` y `full_log` se recortan con `trim`.
3. Si `rule.description` termina en `.`, se elimina ese punto antes de agregar el de la plantilla (nunca queda `..`).
4. `timestamp` se copia **tal como llegó** (string original, sin reformatear).
5. La línea `Datos observados:` se **omite completa** si `data` no existe o si, tras aplicar la §7.3, no queda ningún par.
6. No se agregan otras líneas ni otros campos.

### 7.3 Serialización de `data`

1. Se recorren las claves **en el orden en que aparecen en el JSON**. El DTO debe preservar el orden, por ejemplo con `LinkedHashMap` o `JsonNode`.
2. Se **excluye** `impacto_reportado`, que ya viaja en `Incidente.impacto`.
3. Se excluyen los valores `null` y los strings vacíos o solo con espacios.
4. Cada par se escribe como `clave: valor`, donde en la clave cada `_` se reemplaza por un espacio (`intentos_fallidos` → `intentos fallidos`).
5. Conversión de valores:
   - booleano `true` → `sí`; `false` → `no`;
   - entero → su representación decimal (`35`);
   - arreglo de strings → elementos unidos con `, `;
   - string → tal cual.
6. Los pares se unen con `; ` (punto y coma y espacio), sin separador final.

## 8. Ejemplos de alertas

Estos JSON corresponden a los archivos `src/test/resources/escenarios/alertas/esc-0X.json`.

### 8.1 ESC-01 — Correo reportado con interacción del usuario — `esc-01.json`

Evento modelado con una regla simulada del gateway de correo (el phishing no es una alerta nativa de Wazuh).

```json
{
  "id": "1791468862.1001",
  "timestamp": "2026-10-08T09:14:22-05:00",
  "rule": {
    "id": "100110",
    "level": 10,
    "description": "Correo reportado por un usuario como sospechoso mediante el botón de reporte",
    "groups": [
      "email",
      "user_report"
    ]
  },
  "agent": {
    "id": "011",
    "name": "srv-correo-01",
    "ip": "10.10.1.25"
  },
  "data": {
    "dstuser": "mperez",
    "remitente": "rrhh-nomina@empresa-pagos.example",
    "asunto": "Actualización obligatoria de datos de nómina antes de hoy",
    "url": "http://portal-nomina-actualizacion.example/login",
    "destinatarios": 3,
    "reenviado_internamente": false,
    "accion_usuario": "hizo clic en el enlace e ingresó su usuario y contraseña en la página abierta",
    "impacto_reportado": "La usuaria teme haber entregado sus credenciales corporativas"
  },
  "full_log": "mail-gw: user_report from=rrhh-nomina@empresa-pagos.example to=mperez@empresa.example subject=\"Actualización obligatoria de datos de nómina antes de hoy\" links=1 attachments=0",
  "location": "mail-gateway"
}
```

### 8.2 ESC-02 — Fallos de autenticación repetidos — `esc-02.json`

Incluye `rule.mitre` y `rule.firedtimes` para mostrar campos opcionales que **no** se mapean. No trae `impacto_reportado` (demuestra que es opcional).

```json
{
  "id": "1791470513.2044",
  "timestamp": "2026-10-08T09:41:53-05:00",
  "rule": {
    "id": "100210",
    "level": 10,
    "description": "Múltiples fallos de autenticación SSH contra la misma cuenta en un periodo corto",
    "groups": [
      "authentication_failures",
      "sshd"
    ],
    "mitre": {
      "id": [
        "T1110"
      ],
      "technique": [
        "Brute Force"
      ]
    },
    "firedtimes": 35
  },
  "agent": {
    "id": "004",
    "name": "srv-bastion-01",
    "ip": "10.10.5.20"
  },
  "data": {
    "srcip": "203.0.113.45",
    "dstuser": "admin.infra",
    "tipo_cuenta": "administrativa",
    "protocolo": "SSH",
    "intentos_fallidos": 35,
    "ventana_minutos": 10,
    "inicio_sesion_exitoso": false
  },
  "full_log": "Oct  8 09:41:53 srv-bastion-01 sshd[2211]: Failed password for admin.infra from 203.0.113.45 port 51522 ssh2",
  "location": "/var/log/secure"
}
```

### 8.3 ESC-03 — Detección en endpoint con conexión saliente — `esc-03.json`

`dstip` y `dominio_destino` representan el destino externo; `Incidente.ip` sigue siendo la IP del endpoint afectado (`agent.ip`).

```json
{
  "id": "1791473290.3107",
  "timestamp": "2026-10-08T10:28:10-05:00",
  "rule": {
    "id": "100310",
    "level": 12,
    "description": "EDR: ejecutable con firma maliciosa ejecutado y conexión saliente posterior",
    "groups": [
      "edr",
      "virus"
    ]
  },
  "agent": {
    "id": "027",
    "name": "PC-CONT-014",
    "ip": "10.10.20.14"
  },
  "data": {
    "dstuser": "lgomez",
    "archivo_ruta": "C:\\Users\\lgomez\\Downloads\\factura_septiembre.exe",
    "sha256": "9ef5dd52f49172114172b91db3a44921fb4440757172d3d053f41b2ab4a48284",
    "nombre_deteccion": "Trojan.Win32.Agent.fdsi",
    "proceso_ejecutado": true,
    "contenido_por_edr": false,
    "dstip": "198.51.100.23",
    "dominio_destino": "update-cdn-sync.example",
    "dstport": 443,
    "destino_en_lista_de_amenazas": true,
    "cifrado_masivo_detectado": false,
    "equipos_adicionales_afectados": 0,
    "impacto_reportado": "El usuario nota lentitud en su equipo desde que abrió la factura"
  },
  "full_log": "EDR detection: file=factura_septiembre.exe sha256=9ef5dd52f49172114172b91db3a44921fb4440757172d3d053f41b2ab4a48284 action=allowed process_started=true outbound=198.51.100.23:443 (update-cdn-sync.example)",
  "location": "edr"
}
```

### 8.4 ESC-04 — Reporte con datos insuficientes — `esc-04.json`

No trae `agent` ni `data`. Es válido según el contrato: solo exige `id`, `timestamp`, `rule`, `full_log` y `location`.

```json
{
  "id": "1791474011.4002",
  "timestamp": "2026-10-08T10:40:11-05:00",
  "rule": {
    "id": "100410",
    "level": 3,
    "description": "Reporte manual recibido por mesa de ayuda sin categorizar"
  },
  "full_log": "Ticket HD-2291 (llamada sin identificar): 'algo raro está pasando con el computador desde esta mañana, no sé qué es'",
  "location": "mesa-de-ayuda"
}
```

## 9. Resultado esperado del mapeo

Esta es la referencia para las pruebas unitarias del mapper. También sirve para probar el flujo desde ya con el endpoint temporal `POST /internal/test/clasificar` de `IncidenteTestController`, enviando los cinco campos resultantes.

### 9.1 `esc-01.json` → `Incidente`

| Campo | Valor esperado |
|---|---|
| `ip` | `"10.10.1.25"` |
| `usuario` | `"mperez"` |
| `sistema` | `"srv-correo-01"` |
| `impacto` | `"La usuaria teme haber entregado sus credenciales corporativas"` |

`descripcion`:

```text
Alerta de monitoreo recibida desde mail-gateway.
Evento detectado: Correo reportado por un usuario como sospechoso mediante el botón de reporte.
Registro original: mail-gw: user_report from=rrhh-nomina@empresa-pagos.example to=mperez@empresa.example subject="Actualización obligatoria de datos de nómina antes de hoy" links=1 attachments=0
Datos observados: dstuser: mperez; remitente: rrhh-nomina@empresa-pagos.example; asunto: Actualización obligatoria de datos de nómina antes de hoy; url: http://portal-nomina-actualizacion.example/login; destinatarios: 3; reenviado internamente: no; accion usuario: hizo clic en el enlace e ingresó su usuario y contraseña en la página abierta
Hora del evento: 2026-10-08T09:14:22-05:00
```

### 9.2 `esc-02.json` → `Incidente`

| Campo | Valor esperado |
|---|---|
| `ip` | `"10.10.5.20"` |
| `usuario` | `"admin.infra"` |
| `sistema` | `"srv-bastion-01"` |
| `impacto` | `null` |

`descripcion`:

```text
Alerta de monitoreo recibida desde /var/log/secure.
Evento detectado: Múltiples fallos de autenticación SSH contra la misma cuenta en un periodo corto.
Registro original: Oct  8 09:41:53 srv-bastion-01 sshd[2211]: Failed password for admin.infra from 203.0.113.45 port 51522 ssh2
Datos observados: srcip: 203.0.113.45; dstuser: admin.infra; tipo cuenta: administrativa; protocolo: SSH; intentos fallidos: 35; ventana minutos: 10; inicio sesion exitoso: no
Hora del evento: 2026-10-08T09:41:53-05:00
```

### 9.3 `esc-03.json` → `Incidente`

| Campo | Valor esperado |
|---|---|
| `ip` | `"10.10.20.14"` |
| `usuario` | `"lgomez"` |
| `sistema` | `"PC-CONT-014"` |
| `impacto` | `"El usuario nota lentitud en su equipo desde que abrió la factura"` |

`descripcion`:

```text
Alerta de monitoreo recibida desde edr.
Evento detectado: EDR: ejecutable con firma maliciosa ejecutado y conexión saliente posterior.
Registro original: EDR detection: file=factura_septiembre.exe sha256=9ef5dd52f49172114172b91db3a44921fb4440757172d3d053f41b2ab4a48284 action=allowed process_started=true outbound=198.51.100.23:443 (update-cdn-sync.example)
Datos observados: dstuser: lgomez; archivo ruta: C:\Users\lgomez\Downloads\factura_septiembre.exe; sha256: 9ef5dd52f49172114172b91db3a44921fb4440757172d3d053f41b2ab4a48284; nombre deteccion: Trojan.Win32.Agent.fdsi; proceso ejecutado: sí; contenido por edr: no; dstip: 198.51.100.23; dominio destino: update-cdn-sync.example; dstport: 443; destino en lista de amenazas: sí; cifrado masivo detectado: no; equipos adicionales afectados: 0
Hora del evento: 2026-10-08T10:28:10-05:00
```

### 9.4 `esc-04.json` → `Incidente`

| Campo | Valor esperado |
|---|---|
| `ip` | `null` |
| `usuario` | `null` |
| `sistema` | `null` |
| `impacto` | `null` |

`descripcion`:

```text
Alerta de monitoreo recibida desde mesa-de-ayuda.
Evento detectado: Reporte manual recibido por mesa de ayuda sin categorizar.
Registro original: Ticket HD-2291 (llamada sin identificar): 'algo raro está pasando con el computador desde esta mañana, no sé qué es'
Hora del evento: 2026-10-08T10:40:11-05:00
```

## 10. Validación y errores (recomendado para el adaptador REST)

| Caso | Respuesta sugerida |
|---|---|
| Falta `id`, `timestamp`, `rule`, `rule.id`, `rule.level`, `rule.description`, `full_log` o `location`, o alguno es un string vacío | `400 Bad Request` |
| `timestamp` no es ISO-8601 con offset | `400 Bad Request` |
| `rule.level` fuera de 0–15 | `400 Bad Request` |
| `agent` presente sin `id` o sin `name` | `400 Bad Request` |
| `data` con un objeto anidado | `400 Bad Request` |
| Campos adicionales desconocidos en la raíz, en `rule` o en `agent` | Se ignoran (las alertas reales de Wazuh traen más campos) |
| Faltan `agent` y/o `data` | **Válido** (ESC-04). No debe rechazarse. |

## 11. Relación con otros artefactos

- Respuestas esperadas: `src/test/resources/escenarios/ground-truth/esc-0X.json`. Se mantienen **separadas** de las alertas y nunca se envían al backend.
- Base de conocimiento del RAG: `src/main/resources/manuals/`.
- Descripción narrativa de los escenarios: `docs/escenarios-de-prueba.md`.
- Métricas y procedimiento de evaluación: `docs/evaluacion-y-metricas.md`.

## 12. Control de cambios

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | 2026-09-15 | Versión inicial: contrato, mapeo, plantilla de `descripcion` y 4 ejemplos. |
