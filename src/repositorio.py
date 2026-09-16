"""
Persistencia en PostgreSQL — HU-07.

Todas las operaciones de base de datos van aquí. El resto del código
(contratos.py, alertas.py) sigue trabajando en memoria; este módulo
es el puente entre la ejecución y la BD.

Conexión vía variables de entorno:
  DB_HOST     (default: localhost)
  DB_PORT     (default: 5433)
  DB_NAME     (default: dac)
  DB_USER     (default: dac_user)
  DB_PASSWORD (default: dac_pass)
"""

import os

import psycopg2
from psycopg2.extras import RealDictCursor


def conectar():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "5433"),
        dbname=os.getenv("DB_NAME", "dac"),
        user=os.getenv("DB_USER", "dac_user"),
        password=os.getenv("DB_PASSWORD", "dac_pass"),
    )


def _obtener_o_crear_entidad(cur, nombre):
    cur.execute("SELECT id_entidad FROM entidad WHERE nombre = %s", (nombre,))
    fila = cur.fetchone()
    if fila:
        return fila[0]
    cur.execute(
        "INSERT INTO entidad (nombre) VALUES (%s) RETURNING id_entidad", (nombre,)
    )
    return cur.fetchone()[0]


def _obtener_o_crear_contratista(cur, nombre):
    cur.execute("SELECT id_contratista FROM contratista WHERE nombre = %s", (nombre,))
    fila = cur.fetchone()
    if fila:
        return fila[0]
    cur.execute(
        "INSERT INTO contratista (nombre) VALUES (%s) RETURNING id_contratista",
        (nombre,),
    )
    return cur.fetchone()[0]


def _obtener_o_crear_funcionario(cur, nombre):
    cur.execute("SELECT id_funcionario FROM funcionario WHERE nombre = %s", (nombre,))
    fila = cur.fetchone()
    if fila:
        return fila[0]
    cur.execute(
        "INSERT INTO funcionario (nombre) VALUES (%s) RETURNING id_funcionario",
        (nombre,),
    )
    return cur.fetchone()[0]


def guardar_contratos(conn, contratos):
    """
    Persiste contratos en la BD. Ignora duplicados por clave natural
    (numero_contrato + entidad), garantizando idempotencia entre ejecuciones
    — HU-07, criterios 1 y 2.

    Retorna el número de contratos nuevos insertados.
    """
    nuevos = 0
    with conn.cursor() as cur:
        for c in contratos:
            id_entidad = _obtener_o_crear_entidad(cur, c["entidad"])
            id_contratista = _obtener_o_crear_contratista(cur, c["contratista"])
            id_funcionario = _obtener_o_crear_funcionario(cur, c["funcionario"])

            cur.execute(
                """
                INSERT INTO contrato
                    (numero_contrato, id_entidad, id_contratista, id_funcionario,
                     monto, fecha)
                VALUES (%s, %s, %s, %s, %s, %s)
                ON CONFLICT ON CONSTRAINT uq_contrato_clave_natural DO NOTHING
                """,
                (
                    c["numero_contrato"],
                    id_entidad,
                    id_contratista,
                    id_funcionario,
                    c["monto"],
                    c["fecha"],
                ),
            )
            if cur.rowcount:
                nuevos += 1
    conn.commit()
    return nuevos


def cargar_contratos_bd(conn):
    """
    Devuelve todos los contratos guardados en la BD como lista de dicts,
    con el mismo formato que usa cargar_contratos() de contratos.py.
    """
    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        cur.execute(
            """
            SELECT c.numero_contrato,
                   e.nombre  AS entidad,
                   ct.nombre AS contratista,
                   f.nombre  AS funcionario,
                   c.monto::text AS monto,
                   c.fecha::text AS fecha
            FROM contrato c
            JOIN entidad     e  ON e.id_entidad      = c.id_entidad
            JOIN contratista ct ON ct.id_contratista = c.id_contratista
            JOIN funcionario f  ON f.id_funcionario  = c.id_funcionario
            """
        )
        return [dict(r) for r in cur.fetchall()]


def guardar_alertas(conn, alertas):
    """
    Persiste alertas y su evidencia. Ignora alertas ya existentes para el
    mismo par (idempotencia).
    """
    with conn.cursor() as cur:
        for alerta in alertas:
            cur.execute(
                "SELECT id_contratista FROM contratista WHERE nombre = %s",
                (alerta["contratista"],),
            )
            id_contratista = cur.fetchone()[0]

            cur.execute(
                "SELECT id_funcionario FROM funcionario WHERE nombre = %s",
                (alerta["funcionario"],),
            )
            id_funcionario = cur.fetchone()[0]

            cur.execute(
                """
                INSERT INTO alerta (tipo, id_contratista, id_funcionario)
                VALUES ('REINCIDENCIA', %s, %s)
                ON CONFLICT ON CONSTRAINT uq_alerta_par DO NOTHING
                RETURNING id_alerta
                """,
                (id_contratista, id_funcionario),
            )
            fila = cur.fetchone()
            if not fila:
                continue  # alerta ya existía

            id_alerta = fila[0]

            for contrato in alerta["contratos"]:
                cur.execute(
                    """
                    SELECT c.id_contrato
                    FROM contrato c
                    JOIN entidad e ON e.id_entidad = c.id_entidad
                    WHERE c.numero_contrato = %s AND e.nombre = %s
                    """,
                    (contrato["numero_contrato"], contrato["entidad"]),
                )
                id_contrato = cur.fetchone()[0]
                cur.execute(
                    """
                    INSERT INTO alerta_contrato (id_alerta, id_contrato)
                    VALUES (%s, %s)
                    ON CONFLICT DO NOTHING
                    """,
                    (id_alerta, id_contrato),
                )
    conn.commit()
