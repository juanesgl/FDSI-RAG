# Procedimiento: reportes incompletos o con datos insuficientes

## Observables
- Reporte manual recibido por mesa de ayuda, llamada o ticket sin categorizar.
- La persona que reporta no se identifica, o no se conoce el usuario afectado.
- No se indica el equipo, servidor o sistema afectado.
- Descripción vaga, como "algo raro está pasando", "el computador está extraño" o "creo que pasó algo", sin síntomas concretos.
- No hay registros técnicos, indicadores (IP, hash, dominio, remitente) ni hora aproximada del problema.

## Cuándo la información es insuficiente
Para clasificar con confianza se necesitan tres datos mínimos:
1. Sistema o equipo afectado.
2. Usuario afectado o persona que reporta.
3. Síntoma o actividad concreta observada.

Si falta alguno y el resto del reporte no permite deducirlo, el incidente se clasifica como INFORMACION_INSUFICIENTE. La sensación de que "algo anda mal" no es una actividad observada.

## Por qué no inventar una clasificación
- Una clasificación sin evidencia lleva a proponer contenciones sobre equipos o cuentas que quizá no están involucrados.
- Asignar un tipo específico a un reporte vago sesga la investigación y reduce la consistencia entre analistas.
- No se deben suponer el equipo, el usuario ni la amenaza a partir de reportes parecidos anteriores.

## Información que se debe solicitar
El asistente formula una pregunta de seguimiento concreta y breve, en una sola solicitud al reportante, que pida como mínimo:
- ¿Qué equipo o sistema está afectado (nombre, ubicación o dirección IP)?
- ¿Quién reporta y qué usuario utiliza ese equipo?
- ¿Qué síntoma concreto se observa (mensajes en pantalla, ventanas inesperadas, lentitud, archivos modificados, bloqueos) y desde cuándo?

De forma opcional se puede pedir:
- Si hubo alguna acción previa (abrir un correo, descargar un archivo, instalar un programa).
- Capturas de pantalla o mensajes de error.
- Si otras personas presentan el mismo problema.

## Tratamiento
- Tipo: INFORMACION_INSUFICIENTE, con severidad BAJA y prioridad P4 provisionales.
- No se proponen acciones de contención sobre sistemas o cuentas no identificados.
- Acción recomendada: contactar al reportante con la pregunta de seguimiento y mantener el ticket abierto.
- Cuando llegue la información, el incidente se analiza de nuevo y se clasifica con el procedimiento que corresponda.
- Si mientras tanto se reportan varios usuarios afectados o la interrupción de un servicio, escalar de inmediato sin esperar la clasificación.

## Revisión humana
La solicitud de información también es una recomendación. El analista la revisa y decide si la aprueba para enviarla o la rechaza para gestionar el caso manualmente.
