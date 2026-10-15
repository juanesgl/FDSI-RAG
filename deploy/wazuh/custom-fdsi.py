#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Integracion custom de Wazuh -> backend FDSI-RAG.

El modulo wazuh-integratord invoca este script cada vez que una alerta
coincide con el filtro definido en ossec.conf (bloque <integration>).
Recibe el archivo de la alerta y la URL destino, transforma la alerta
real de Wazuh al contrato del backend (AlertaSeguridadRequest) y la
envia por POST a /api/v1/alertas.

Esta es la "capa anticorrupcion": el backend no sabe nada de Wazuh; este
script traduce el formato Wazuh al contrato ya acordado.

Argumentos que pasa Wazuh (orden fijo del integratord):
    sys.argv[1] = ruta del archivo JSON con la alerta
    sys.argv[2] = api_key (no se usa aqui; el backend no la exige)
    sys.argv[3] = hook_url (la del <hook_url> en ossec.conf)

Solo libreria estandar de Python 3.
"""

import json
import re
import sys
import urllib.request
import urllib.error

LOG_FILE = "/var/ossec/logs/integrations.log"

# IP del propio manager (Wazuh). Se usa para rellenar "IP afectada" cuando el
# ataque se auto-monitorea (agente 000, que no trae IP en la alerta). Es el
# host realmente atacado. CAMBIA ESTE VALOR si la IP de tu VM es otra.
IP_MANAGER = "192.168.2.21"


def log(msg):
    """Deja rastro en integrations.log para poder depurar la integracion."""
    try:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(f"custom-fdsi: {msg}\n")
    except Exception:
        pass  # el logging nunca debe tumbar la integracion


def normalizar_timestamp(ts):
    """
    Wazuh entrega el offset sin dos puntos (p. ej. '...+0000' o '...-0500').
    El backend espera ISO-8601 con offset 'con' dos puntos ('+00:00').
    Esta funcion inserta los dos puntos si hacen falta.
    """
    if not ts:
        return None
    return re.sub(r"([+-]\d{2})(\d{2})$", r"\1:\2", ts)


def contar_intentos(alerta, regla):
    """
    Cuenta los intentos reales que Wazuh agrupo en esta correlacion:
    las lineas de 'previous_output' (los eventos previos que dispararon la
    regla de frecuencia) + el que la disparo (full_log). Es dato real de
    Wazuh, no inventado. Sirve para cualquier volumen: si hubo mas intentos,
    'previous_output' trae mas lineas. Si no hay previous_output, usa el
    umbral de frecuencia de la regla como respaldo.
    """
    prev = alerta.get("previous_output") or ""
    lineas = [l for l in prev.split("\n") if l.strip()]
    total = len(lineas)
    if alerta.get("full_log"):
        total += 1  # el evento que disparo la correlacion
    if total <= 1:
        freq = regla.get("frequency")
        if isinstance(freq, int) and freq > 0:
            return freq
    return total if total > 0 else None


def texto_mitre(regla):
    """Texto corto con la tecnica MITRE si la alerta la trae (p. ej. 'T1110 - Brute Force')."""
    mitre = regla.get("mitre") or {}
    ids = mitre.get("id") or []
    tecnicas = mitre.get("technique") or []
    if ids:
        tec = tecnicas[0] if tecnicas else ""
        return f"{ids[0]} - {tec}".strip(" -")
    return None


def construir_payload(alerta):
    """Mapea la alerta de Wazuh al contrato AlertaSeguridadRequest del backend."""
    regla = alerta.get("rule", {}) or {}
    agente = alerta.get("agent", {}) or {}
    # copia de data: la vamos a enriquecer con lo que Wazuh ya sabe pero que el
    # contrato no transportaba (el conteo de intentos, el protocolo, MITRE...).
    datos = dict(alerta.get("data", {}) or {})

    # --- Enriquecimiento HONESTO -------------------------------------------
    # El modelo clasifica mal cuando no ve la MAGNITUD del ataque. Wazuh si la
    # tiene (previous_output / frequency); aqui la pasamos a "data" para que el
    # backend se la muestre al modelo. No se inventa nada: todo sale de la
    # alerta real.
    es_brute_force = ("authentication_failures" in (regla.get("groups") or [])
                      or "brute force" in (regla.get("description") or "").lower())
    if es_brute_force:
        intentos = contar_intentos(alerta, regla)
        if intentos:
            datos.setdefault("intentos_fallidos", intentos)
        datos.setdefault("protocolo", "SSH")
        datos.setdefault("inicio_sesion_exitoso", False)
    tecnica = texto_mitre(regla)
    if tecnica:
        datos.setdefault("tecnica_mitre", tecnica)

    payload = {
        "id": alerta.get("id"),
        "timestamp": normalizar_timestamp(alerta.get("timestamp")),
        "rule": {
            "id": regla.get("id"),
            "level": regla.get("level"),
            "description": regla.get("description"),
            "groups": regla.get("groups"),
        },
        "full_log": alerta.get("full_log"),
        "location": alerta.get("location"),
        # data debe ir siempre (aunque sea {}): el contrato lo marca presente
        "data": datos,
    }

    # agent = el sistema afectado. Antes se omitia el del manager (id "000")
    # para no mandar un agente vacio, pero eso dejaba la alerta sin "Sistema"
    # y la empujaba a INFORMACION_INSUFICIENTE. El manager tiene un nombre real
    # (p. ej. wazuh-server) y ES el blanco del ataque, asi que lo incluimos.
    # Solo se omite si de plano no viene identificado.
    nombre_agente = agente.get("name")
    ip_agente = agente.get("ip")
    if not nombre_agente and agente.get("id") == "000":
        nombre_agente = "wazuh-server"  # manager auto-monitoreado
    if agente.get("id") == "000" and not ip_agente:
        ip_agente = IP_MANAGER  # el manager no trae IP: usamos la del host atacado
    if nombre_agente:
        payload["agent"] = {
            "id": agente.get("id"),
            "name": nombre_agente,
            "ip": ip_agente,
        }

    return payload


def enviar(payload, hook_url):
    cuerpo = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        hook_url, data=cuerpo, method="POST",
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.status, r.read().decode("utf-8", "replace")


def main():
    if len(sys.argv) < 4:
        log(f"faltan argumentos (recibidos {len(sys.argv) - 1}). Se esperaba: archivo api_key hook_url")
        sys.exit(1)

    archivo_alerta = sys.argv[1]
    hook_url = sys.argv[3]

    try:
        with open(archivo_alerta, encoding="utf-8") as f:
            alerta = json.load(f)
    except Exception as e:
        log(f"no se pudo leer/parsear la alerta {archivo_alerta}: {e}")
        sys.exit(1)

    payload = construir_payload(alerta)

    try:
        status, respuesta = enviar(payload, hook_url)
        log(f"OK alerta {payload.get('id')} rule={payload['rule'].get('id')} "
            f"-> HTTP {status}")
    except urllib.error.HTTPError as e:
        detalle = e.read().decode("utf-8", "replace")
        log(f"ERROR HTTP {e.code} al enviar alerta {payload.get('id')}: {detalle}")
        sys.exit(1)
    except urllib.error.URLError as e:
        log(f"ERROR de conexion al enviar alerta {payload.get('id')} a {hook_url}: {e.reason}")
        sys.exit(1)


if __name__ == "__main__":
    main()