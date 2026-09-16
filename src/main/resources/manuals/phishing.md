# Procedimiento: correo sospechoso y exposición de credenciales (PHISHING)

## Observables
- Correo reportado por un usuario como sospechoso, por ejemplo con el botón de reporte del correo corporativo.
- Remitente que suplanta a un área interna (nómina, recursos humanos, soporte) usando un dominio externo o parecido al corporativo.
- Asunto urgente u obligatorio: actualización de datos, pago pendiente, cuenta por vencer.
- Enlace a una página de inicio de sesión que no pertenece a la organización.
- Adjunto no solicitado.
- El usuario hizo clic en el enlace, escribió su usuario y contraseña en esa página, o teme haber entregado sus credenciales.
- El mismo correo llegó a varios destinatarios o fue reenviado dentro de la organización.

## Clasificación
Se clasifica como PHISHING todo correo o mensaje que intenta engañar al usuario para obtener credenciales, provocar un clic o una descarga. La interacción del usuario no cambia el tipo; determina la severidad.

## Severidad
- BAJA: correo reportado sin interacción del usuario.
- MEDIA: el usuario abrió el enlace, pero no ingresó credenciales ni descargó archivos.
- ALTA: el usuario ingresó credenciales en la página sospechosa o descargó el adjunto. Las credenciales se consideran expuestas aunque no se haya confirmado su uso por terceros.
- CRITICA: el correo se reenvió masivamente dentro de la organización desde una cuenta interna, o hay evidencia de uso de las credenciales expuestas (inicios de sesión no reconocidos, reglas de reenvío nuevas, envíos no autorizados desde la cuenta).

El número de destinatarios externos, por sí solo, no eleva la severidad a CRITICA.

## Acciones recomendadas (propuestas para revisión humana)
1. Validar el reporte: remitente real, cabeceras, dominio y destino del enlace.
2. Proponer la cuarentena del correo en todos los buzones que lo recibieron.
3. Proponer el bloqueo del dominio y de la URL en el proxy o filtro web.
4. Si hubo credenciales expuestas: proponer el cambio forzado de contraseña, el cierre de sesiones activas y la verificación del segundo factor (MFA) de la cuenta afectada.
5. Revisar la actividad asociada a la cuenta desde la hora del clic: inicios de sesión, ubicaciones, reglas del buzón y correos enviados.
6. Identificar a los demás destinatarios y confirmar si alguno interactuó con el correo.
7. Escalar a analista senior si la cuenta tiene privilegios, si hay indicios de uso de las credenciales o si el correo se propagó internamente.

## Evidencia a preservar
- Correo original con cabeceras completas.
- Remitente, dominio y URL como indicadores de compromiso (IoC).
- Hora del reporte y hora aproximada de la interacción del usuario.
- Acciones propuestas y decisión del analista.

## Qué no hacer
- No eliminar el correo antes de preservar la evidencia.
- No declarar la cuenta comprometida sin revisar su actividad.
- No presentar ninguna acción como ya ejecutada: toda contención requiere aprobación humana, según la política de aprobación y escalamiento.
