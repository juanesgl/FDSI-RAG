# Política: aprobación humana y escalamiento (Human-in-the-Loop)

## Principio
El asistente de respuesta a incidentes recomienda y las personas deciden. Ninguna acción de contención (bloqueo, aislamiento, cambio de credenciales, cuarentena) se ejecuta automáticamente sobre sistemas reales. Toda acción operativa ocurre fuera del asistente y solo después de la aprobación de un responsable.

## Flujo
1. Análisis: el asistente recibe el incidente y revisa la información disponible.
2. Consulta: recupera los procedimientos internos relevantes.
3. Propuesta: genera tipo, severidad, justificación y acciones recomendadas basadas en esos procedimientos.
4. Revisión: el incidente queda en espera de aprobación humana.
5. Decisión: el analista responsable aprueba o rechaza la propuesta.
6. Escalamiento: según la situación, el caso pasa a un analista senior o al coordinador.
7. Registro: se conservan la propuesta, la decisión, el responsable y la hora.

## Qué revisa el analista
Toda recomendación requiere revisión. El analista verifica que:
- El tipo y la severidad sean coherentes con la evidencia y con la política de clasificación.
- Cada acción esté respaldada por un procedimiento interno.
- Las acciones sean proporcionales al riesgo y apunten al activo o a la cuenta correctos.
- Ninguna acción se presente como ya realizada.
- No se proponga eliminar evidencia.

La propuesta se rechaza si la clasificación no corresponde, si incluye acciones sin respaldo o si omite contenciones necesarias. Tras un rechazo, el analista clasifica y define las acciones manualmente.

## Autoridad de aprobación
| Severidad | Quién aprueba |
|---|---|
| BAJA o MEDIA | Analista de turno |
| ALTA | Analista de turno, con notificación al analista senior |
| CRITICA | Analista senior o coordinador del equipo de respuesta |

Las acciones de alto impacto operativo, como aislar un equipo, bloquear una cuenta administrativa o bloquear rangos de IP, siempre requieren aprobación explícita.

## Cuándo escalar
- Severidad CRITICA.
- Cuentas administrativas, privilegiadas o de servicio involucradas.
- Indicios de acceso exitoso o de uso de credenciales expuestas.
- Posible afectación de más de un equipo o usuario.
- Necesidad de análisis forense.
- Clasificación dudosa, o propuesta rechazada en un caso ALTA o CRITICA.
- Reporte incompleto con señales de afectación amplia.

## Preservación de evidencia
Antes de aprobar cualquier contención se asegura la evidencia: alertas, registros, correos, hashes, indicadores y hora de los eventos. Las acciones que destruyen evidencia (borrar, formatear, reinstalar o apagar un equipo comprometido) no se aprueban sin autorización del analista senior.

## Registro de la decisión
Por cada incidente se registran la propuesta del asistente, la decisión (aprobada o rechazada), el responsable, la fecha y hora, y las observaciones. Este registro permite medir la aceptación de las recomendaciones y mejorar los procedimientos.
