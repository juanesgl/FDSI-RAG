# Procedimiento: Intentos de Acceso No Autorizado

## Clasificacion
Multiples intentos fallidos de autenticacion contra un mismo usuario o sistema, intentos
de fuerza bruta, o inicios de sesion desde ubicaciones/geolocalizaciones inusuales se
clasifican como ACCESO_NO_AUTORIZADO.

## Severidad
- BAJA: menos de 5 intentos fallidos en un periodo corto, desde una IP conocida.
- MEDIA: entre 5 y 20 intentos fallidos, o un inicio de sesion exitoso posterior desde
  ubicacion inusual.
- ALTA: mas de 20 intentos fallidos, patron de fuerza bruta contra una cuenta privilegiada.
- CRITICA: acceso exitoso tras un patron de fuerza bruta contra cuenta administrativa.

## Acciones de contencion recomendadas
1. Bloquear temporalmente la cuenta afectada tras superar el umbral de intentos.
2. Bloquear la IP origen en el firewall perimetral.
3. Forzar autenticacion multifactor (MFA) en el proximo inicio de sesion legitimo.
4. Si el acceso fue exitoso: iniciar procedimiento de contencion de cuenta comprometida
   (revocar sesiones, rotar credenciales, revisar actividad reciente).
5. Escalar a analista senior si la cuenta afectada tiene privilegios administrativos.
