#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generador dinamico de alertas simuladas (Nivel 2).

Fabrica alertas con la estructura del contrato del backend y las envia a
POST /api/v1/alertas. Los datos variables (hora, IP de origen, usuario,
numero de intentos, etc.) cambian en cada ejecucion, para demostrar que el
sistema procesa escenarios nuevos y no 4 JSON memorizados.

Esto es un SIMULADOR de fuente: fabrica la alerta, no la detecta. La
deteccion real corresponde al Nivel 3 (Wazuh).

Solo libreria estandar de Python 3. No requiere pip install.

Uso:
    python3 generar_alertas.py                 # 1 alerta de un escenario al azar
    python3 generar_alertas.py --escenario phishing
    python3 generar_alertas.py --escenario accesos --repetir 5
    python3 generar_alertas.py --listar        # ver escenarios disponibles
    python3 generar_alertas.py --dry-run       # imprime el JSON, no lo envia
    python3 generar_alertas.py --url http://localhost:8080

Escenarios: phishing, accesos, malware, incompleto, aleatorio (por defecto).
"""

import argparse
import json
import random
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone, timedelta

# Zona horaria de Colombia (UTC-5), para que el timestamp salga como -05:00
TZ_CO = timezone(timedelta(hours=-5))

# --- Datos de relleno para variar las alertas en cada corrida ---------------
USUARIOS = ["mperez", "lgomez", "jrodriguez", "asalazar", "dcastro", "ntorres"]
USUARIOS_ADMIN = ["admin.infra", "root", "svc.backup", "admin.red"]
EQUIPOS = ["PC-CONT-014", "PC-VENTAS-07", "PC-RRHH-03", "LAP-GER-01"]
SERVIDORES = ["srv-bastion-01", "srv-app-02", "srv-db-01", "srv-web-03"]
MALWARE = ["Trojan.Win32.Agent.fdsi", "Win32.Emotet.gen", "Ransom.Lockbit.sim",
           "Backdoor.Win32.Remcos"]
ARCHIVOS = ["factura_septiembre.exe", "cv_actualizado.scr", "cotizacion.exe",
            "reporte_nomina.exe"]
DOMINIOS_C2 = ["update-cdn-sync.example", "secure-login-portal.example",
               "cloud-backup-svc.example"]
ASUNTOS_PHISH = ["Actualizacion obligatoria de datos de nomina antes de hoy",
                 "Su cuenta sera suspendida: verifique ahora",
                 "Comprobante de pago pendiente de confirmacion"]


def ip_privada():
    """IP interna aleatoria del rango 10.10.x.x (el activo afectado)."""
    return f"10.10.{random.randint(1, 30)}.{random.randint(2, 254)}"


def ip_documentacion():
    """IP externa de rangos de documentacion RFC 5737 (el atacante)."""
    rango = random.choice(["203.0.113", "198.51.100"])
    return f"{rango}.{random.randint(1, 254)}"


def ahora_iso():
    """Timestamp ISO-8601 con offset -05:00, como exige el contrato."""
    return datetime.now(TZ_CO).replace(microsecond=0).isoformat()


def nuevo_id():
    """ID estilo Wazuh (epoch.secuencia), neutral: no revela el escenario."""
    epoch = int(datetime.now(TZ_CO).timestamp())
    return f"{epoch}.{random.randint(1000, 9999)}"


# --- Constructores de cada escenario ----------------------------------------
# Cada uno devuelve una alerta nueva con datos variables en cada llamada.

def esc_phishing():
    usuario = random.choice(USUARIOS)
    destinatarios = random.randint(1, 8)
    return {
        "id": nuevo_id(),
        "timestamp": ahora_iso(),
        "rule": {
            "id": "100110",
            "level": 10,
            "description": "Correo reportado por un usuario como sospechoso "
                           "mediante el boton de reporte",
        },
        "agent": {"id": "011", "name": "srv-correo-01", "ip": ip_privada()},
        "data": {
            "dstuser": usuario,
            "remitente": "rrhh-nomina@empresa-pagos.example",
            "asunto": random.choice(ASUNTOS_PHISH),
            "url": "http://portal-nomina-actualizacion.example/login",
            "destinatarios": destinatarios,
            "reenviado_internamente": False,
            "accion_usuario": "hizo clic en el enlace e ingreso su usuario y "
                              "contrasena en la pagina abierta",
            "impacto_reportado": "El usuario teme haber entregado sus "
                                 "credenciales corporativas",
        },
        "full_log": f"mail-gw: user_report from=rrhh-nomina@empresa-pagos.example "
                    f"to={usuario}@empresa.example links=1 attachments=0",
        "location": "mail-gateway",
    }


def esc_accesos():
    usuario = random.choice(USUARIOS_ADMIN)
    servidor = random.choice(SERVIDORES)
    origen = ip_documentacion()
    intentos = random.randint(21, 60)   # >20 => severidad ALTA segun el manual
    ventana = random.randint(5, 15)
    return {
        "id": nuevo_id(),
        "timestamp": ahora_iso(),
        "rule": {
            "id": "100210",
            "level": 10,
            "description": "Multiples fallos de autenticacion SSH contra la "
                           "misma cuenta en un periodo corto",
        },
        "agent": {"id": "004", "name": servidor, "ip": ip_privada()},
        "data": {
            "srcip": origen,
            "dstuser": usuario,
            "tipo_cuenta": "administrativa",
            "protocolo": "SSH",
            "intentos_fallidos": intentos,
            "ventana_minutos": ventana,
            "inicio_sesion_exitoso": False,
        },
        "full_log": f"sshd[{random.randint(1000, 9999)}]: Failed password for "
                    f"{usuario} from {origen} port {random.randint(40000, 60000)} ssh2",
        "location": "/var/log/secure",
    }


def esc_malware():
    usuario = random.choice(USUARIOS)
    equipo = random.choice(EQUIPOS)
    archivo = random.choice(ARCHIVOS)
    c2 = random.choice(DOMINIOS_C2)
    dst = ip_documentacion()
    return {
        "id": nuevo_id(),
        "timestamp": ahora_iso(),
        "rule": {
            "id": "100310",
            "level": 12,
            "description": "EDR: ejecutable con firma maliciosa ejecutado y "
                           "conexion saliente posterior",
        },
        "agent": {"id": "027", "name": equipo, "ip": ip_privada()},
        "data": {
            "dstuser": usuario,
            "archivo_ruta": f"C:\\Users\\{usuario}\\Downloads\\{archivo}",
            "nombre_deteccion": random.choice(MALWARE),
            "proceso_ejecutado": True,
            "contenido_por_edr": False,
            "dstip": dst,
            "dominio_destino": c2,
            "dstport": 443,
            "destino_en_lista_de_amenazas": True,
            "cifrado_masivo_detectado": False,
            "equipos_adicionales_afectados": 0,
            "impacto_reportado": "El usuario nota lentitud en su equipo desde "
                                 "que abrio el archivo",
        },
        "full_log": f"EDR detection: file={archivo} action=allowed "
                    f"process_started=true outbound={dst}:443 ({c2})",
        "location": "edr",
    }


def esc_incompleto():
    """Reporte vago: sin agent ni data. Valido segun el contrato."""
    ticket = random.randint(2000, 2999)
    sintomas = ["algo raro esta pasando con el computador desde esta manana, "
                "no se que es",
                "el equipo esta muy lento y salen ventanas solas",
                "creo que paso algo pero no estoy seguro"]
    return {
        "id": nuevo_id(),
        "timestamp": ahora_iso(),
        "rule": {
            "id": "100410",
            "level": 3,
            "description": "Reporte manual recibido por mesa de ayuda sin categorizar",
        },
        "full_log": f"Ticket HD-{ticket} (llamada sin identificar): "
                    f"'{random.choice(sintomas)}'",
        "location": "mesa-de-ayuda",
    }


ESCENARIOS = {
    "phishing": esc_phishing,
    "accesos": esc_accesos,
    "malware": esc_malware,
    "incompleto": esc_incompleto,
}


# --- Envio al backend -------------------------------------------------------
def enviar(alerta, url):
    """POST de la alerta al backend. Devuelve (ok, respuesta_o_error)."""
    endpoint = url.rstrip("/") + "/api/v1/alertas"
    cuerpo = json.dumps(alerta).encode("utf-8")
    req = urllib.request.Request(
        endpoint, data=cuerpo, method="POST",
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return True, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detalle = e.read().decode("utf-8", "replace")
        return False, f"HTTP {e.code}: {detalle}"
    except urllib.error.URLError as e:
        return False, (f"No se pudo conectar con {endpoint}. "
                       f"Esta el backend corriendo? Detalle: {e.reason}")


def resumen(resp):
    """Linea legible con lo que el backend clasifico."""
    if not isinstance(resp, dict):
        return str(resp)
    return (f"id={resp.get('id', '?')[:8]}... "
            f"tipo={resp.get('tipo', '?')} "
            f"severidad={resp.get('severidad', '?')} "
            f"estado={resp.get('estado', '?')}")


def detalle(resp):
    """Bloque legible con la propuesta completa que devolvio el backend."""
    if not isinstance(resp, dict):
        return ""
    lineas = ["",
              "    --- Propuesta del asistente (pendiente de aprobacion humana) ---",
              f"    Incidente: {resp.get('id', '?')}"]
    accion = resp.get("accionPropuesta")
    if accion:
        for linea in str(accion).splitlines():
            lineas.append(f"    {linea}")
    else:
        lineas.append("    (el backend no devolvio una accion propuesta)")
    return "\n".join(lineas)


def main():
    p = argparse.ArgumentParser(
        description="Generador dinamico de alertas simuladas (Nivel 2).")
    p.add_argument("--escenario", default="aleatorio",
                   choices=list(ESCENARIOS) + ["aleatorio"],
                   help="Escenario a generar (por defecto: aleatorio).")
    p.add_argument("--repetir", type=int, default=1,
                   help="Cuantas alertas enviar (por defecto: 1).")
    p.add_argument("--url", default="http://localhost:8080",
                   help="URL base del backend (por defecto: localhost:8080).")
    p.add_argument("--dry-run", action="store_true",
                   help="Imprime el JSON generado sin enviarlo.")
    p.add_argument("--verbose", "-v", action="store_true",
                   help="Muestra la propuesta completa del backend (justificacion y acciones).")
    p.add_argument("--listar", action="store_true",
                   help="Lista los escenarios disponibles y sale.")
    args = p.parse_args()

    if args.listar:
        print("Escenarios disponibles:")
        for nombre in ESCENARIOS:
            print(f"  - {nombre}")
        print("  - aleatorio (elige uno al azar en cada alerta)")
        return

    for i in range(args.repetir):
        nombre = (random.choice(list(ESCENARIOS))
                  if args.escenario == "aleatorio" else args.escenario)
        alerta = ESCENARIOS[nombre]()

        if args.dry_run:
            print(f"--- [{i + 1}/{args.repetir}] escenario={nombre} (dry-run) ---")
            print(json.dumps(alerta, indent=2, ensure_ascii=False))
            continue

        ok, resp = enviar(alerta, args.url)
        etiqueta = f"[{i + 1}/{args.repetir}] escenario={nombre}"
        if ok:
            print(f"OK  {etiqueta} -> {resumen(resp)}")
            if args.verbose:
                print(detalle(resp))
        else:
            print(f"ERR {etiqueta} -> {resp}", file=sys.stderr)
            # Si falla la conexion, no tiene sentido seguir reintentando.
            if "No se pudo conectar" in str(resp):
                sys.exit(1)


if __name__ == "__main__":
    main()