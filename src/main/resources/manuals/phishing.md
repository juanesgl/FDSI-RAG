# Procedimiento: Correo de Phishing

## Clasificacion
Un reporte de correo sospechoso con enlaces o adjuntos no verificados, suplantacion de
remitente, o solicitud urgente de credenciales se clasifica como PHISHING.

## Severidad
- BAJA: el usuario reporto el correo sin haber interactuado con el (no hizo clic, no
  descargo adjuntos, no ingreso credenciales).
- MEDIA: el usuario hizo clic en un enlace pero no ingreso credenciales ni descargo archivos.
- ALTA: el usuario ingreso credenciales en un sitio falso o descargo un adjunto.
- CRITICA: el correo se envio o reenvio masivamente dentro de la organizacion (posible
  compromiso de cuenta usada para propagacion).

## Acciones de contencion recomendadas
1. Poner en cuarentena el correo en el gateway de correo para todos los destinatarios.
2. Bloquear el dominio y las URLs remitentes en el proxy/firewall.
3. Si el usuario ingreso credenciales: forzar cambio de contrasena y revisar sesiones activas.
4. Notificar al area de comunicaciones si el correo se distribuyo masivamente.
5. Documentar indicadores de compromiso (IoC) para el SIEM.
