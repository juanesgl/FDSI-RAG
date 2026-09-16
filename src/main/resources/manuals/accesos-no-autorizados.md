# Procedimiento: intentos de acceso no autorizado (ACCESO_NO_AUTORIZADO)

## Observables
- Múltiples fallos de autenticación (contraseña incorrecta) contra la misma cuenta en un periodo corto.
- Intentos repetidos por SSH, RDP, VPN o portal web desde una misma IP de origen.
- Intentos dirigidos a cuentas administrativas, privilegiadas o de servicio (admin, root, cuentas de infraestructura).
- Intentos contra usuarios inexistentes o contra muchas cuentas desde el mismo origen.
- Inicio de sesión exitoso después de una serie de fallos, o desde una ubicación u horario inusual.
- IP de origen externa, desconocida o sin relación con la organización.

## Clasificación
Se clasifica como ACCESO_NO_AUTORIZADO todo intento de autenticarse o acceder a una cuenta o sistema sin autorización, haya tenido éxito o no.

## Intento fallido frente a acceso exitoso
- Solo fallos: hay riesgo, pero no compromiso confirmado. La cuenta se protege y no se trata como comprometida.
- Fallos seguidos de un inicio de sesión exitoso no reconocido: se asume compromiso de la cuenta hasta demostrar lo contrario.

Antes de fijar la severidad, verificar siempre en los registros si hubo algún inicio de sesión exitoso.

## Severidad
- BAJA: menos de 5 fallos en poco tiempo, desde una IP conocida o interna, contra una cuenta estándar.
- MEDIA: entre 5 y 20 fallos contra una cuenta estándar, o un inicio de sesión exitoso desde una ubicación inusual sin fallos previos.
- ALTA: más de 20 fallos contra una misma cuenta en una ventana corta, o intentos repetidos contra una cuenta administrativa o privilegiada, aunque no haya acceso exitoso.
- CRITICA: acceso exitoso a una cuenta administrativa tras una serie de fallos, o acceso exitoso confirmado con actividad no reconocida.

## Acciones recomendadas (propuestas para revisión humana)
1. Revisar el origen: IP, geolocalización, reputación y si pertenece a la organización.
2. Validar la legitimidad con el responsable de la cuenta (olvido de contraseña, servicio con credenciales desactualizadas). Si el origen resulta legítimo, documentarlo y reclasificar según la política de clasificación.
3. Proponer el bloqueo temporal de la IP de origen en el firewall perimetral.
4. Proponer la protección de la cuenta: bloqueo temporal, verificación de MFA y cambio de contraseña si hay dudas.
5. Revisar los registros de autenticación del sistema afectado y de otros sistemas, buscando la misma IP o la misma cuenta.
6. Si hubo acceso exitoso: proponer el cierre de sesiones, la rotación de credenciales y la revisión de la actividad realizada.
7. Escalar a analista senior cuando la cuenta sea administrativa o privilegiada, o cuando exista acceso exitoso.

## Evidencia a preservar
- Registros de autenticación con fecha, hora, cuenta, IP de origen y protocolo.
- Número de intentos y ventana de tiempo.
- Resultado de la verificación de acceso exitoso.

## Qué no hacer
- No declarar la cuenta comprometida si solo hay intentos fallidos.
- No apagar ni reiniciar el servidor afectado: se pierden registros y no se detienen los intentos.
- No ejecutar bloqueos directamente: toda acción queda sujeta a aprobación humana.
