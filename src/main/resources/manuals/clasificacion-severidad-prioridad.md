# Política: clasificación, severidad y prioridad de incidentes

## Alcance
Aplica a todo incidente analizado por el equipo de respuesta. Define los tipos permitidos, los criterios generales de severidad y la prioridad de atención. Los procedimientos específicos de cada tipo fijan sus propios umbrales; esta política da consistencia entre analistas y resuelve dudas.

## Tipos de incidente
Cada incidente recibe exactamente una etiqueta:
- PHISHING: mensaje engañoso que busca obtener credenciales, clics o descargas.
- ACCESO_NO_AUTORIZADO: intento o acceso a cuentas o sistemas sin autorización.
- MALWARE: software malicioso detectado o ejecutado en un equipo.
- FUGA_DE_INFORMACION: exposición, copia o envío de información de la organización a destinatarios o ubicaciones no autorizados, sea intencional o accidental.
- DENEGACION_DE_SERVICIO: degradación o interrupción de la disponibilidad de un sistema o servicio causada por saturación intencional o tráfico anómalo.
- INFORMACION_INSUFICIENTE: el reporte no permite clasificar con confianza (ver el procedimiento de reportes incompletos).
- OTRO: evento de seguridad con información suficiente que no corresponde a ningún tipo anterior.

Diferencia clave: si no se conocen el sistema afectado, el usuario o una actividad concreta, el tipo es INFORMACION_INSUFICIENTE y no OTRO.

## Criterios de severidad
Se evalúan en conjunto:
- Impacto potencial: qué ocurriría si la amenaza tiene éxito (pérdida de datos, interrupción, suplantación).
- Evidencia disponible: indicio, intento, ejecución o compromiso confirmado.
- Criticidad del activo o de la cuenta: servidores y cuentas administrativas o privilegiadas pesan más que equipos o cuentas estándar.
- Alcance: un usuario o equipo frente a varios.
- Compromiso: confirmado o potencial.
- Necesidad de contención: si esperar aumenta el daño.

Niveles:
- CRITICA: compromiso confirmado con alto impacto o alcance amplio, como propagación a varios equipos, cifrado de información o acceso exitoso a una cuenta administrativa.
- ALTA: compromiso potencial serio que exige contención pronta, aunque afecte a un solo usuario o equipo. Por ejemplo: credenciales expuestas en un sitio no confiable, intentos repetidos y dirigidos contra una cuenta administrativa, o código malicioso ejecutado en un equipo sin contención automática.
- MEDIA: actividad sospechosa con impacto limitado o parcialmente contenida.
- BAJA: evento sin interacción ni ejecución, contenido automáticamente o con impacto mínimo.

Un nivel solo se asigna si la evidencia disponible cumple sus criterios. No se eleva la severidad por suposiciones sobre hechos que aún no se han observado.

## Prioridad derivada
La prioridad no es un dato de entrada del incidente ni se registra de forma independiente: se deriva siempre de la severidad asignada.

| Severidad | Prioridad | Tiempo objetivo de atención |
|---|---|---|
| CRITICA | P1 | Inmediata, máximo 15 minutos |
| ALTA | P2 | Máximo 1 hora |
| MEDIA | P3 | Máximo 4 horas |
| BAJA | P4 | Máximo 1 día hábil |

Si la revisión humana cambia la severidad, la prioridad cambia con ella.

## Clasificación provisional
Un incidente con INFORMACION_INSUFICIENTE recibe severidad BAJA y prioridad P4 de forma provisional, hasta completar los datos y volver a clasificarlo.

## Justificación
Toda clasificación explica qué evidencia sostiene el tipo y el nivel de severidad, y qué procedimiento interno se aplicó.
