"""
Carga de contratos desde un archivo CSV.

Resuelve el problema duro de esta entrega: idempotencia. Si un contrato
(identificado por número de contrato + entidad) ya fue cargado antes,
no se vuelve a agregar ni a contar dos veces.
"""

import csv

COLUMNAS_REQUERIDAS = [
    "numero_contrato",
    "entidad",
    "contratista",
    "funcionario",
    "monto",
    "fecha",
]


def cargar_contratos(ruta_csv):
    """
    Lee un archivo CSV de contratos y devuelve una lista de contratos
    únicos (sin duplicados), validando que las columnas requeridas existan.
    """
    contratos = []
    claves_vistas = set()

    with open(ruta_csv, newline="", encoding="utf-8") as archivo:
        lector = csv.DictReader(archivo)

        # Un archivo vacío no tiene encabezado: fieldnames queda en None
        if lector.fieldnames is None:
            raise ValueError(f"El CSV está vacío o no tiene encabezado: {ruta_csv}")

        faltantes = [c for c in COLUMNAS_REQUERIDAS if c not in lector.fieldnames]
        if faltantes:
            raise ValueError(f"Faltan columnas en el CSV: {faltantes}")

        for fila in lector:
            clave = (fila["numero_contrato"], fila["entidad"])

            # Idempotencia: si ya vimos este contrato, lo ignoramos
            if clave in claves_vistas:
                continue

            claves_vistas.add(clave)
            contratos.append(fila)

    return contratos
