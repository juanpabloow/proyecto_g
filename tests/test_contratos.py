"""
Pruebas de la carga de contratos.

El foco es el problema duro de esta entrega: idempotencia. Ver
docs/problema-duro.md, §7.
"""

import pytest

from src.alertas import detectar_reincidencias
from src.contratos import cargar_contratos

ENCABEZADO = "numero_contrato,entidad,contratista,funcionario,monto,fecha\n"


def escribir_csv(directorio, filas, encabezado=ENCABEZADO):
    """Crea un CSV temporal y devuelve su ruta."""
    ruta = directorio / "contratos.csv"
    ruta.write_text(encabezado + "".join(filas), encoding="utf-8")
    return str(ruta)


# --- Validación del archivo (HU-01) ---------------------------------


def test_carga_contratos_validos(tmp_path):
    ruta = escribir_csv(
        tmp_path,
        [
            "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n",
            "002,Alcaldía,Otra Ltda,Ana Ruiz,200,2026-01-02\n",
        ],
    )

    contratos = cargar_contratos(ruta)

    assert len(contratos) == 2
    assert contratos[0]["contratista"] == "ACME SAS"


def test_falla_si_faltan_columnas(tmp_path):
    ruta = escribir_csv(
        tmp_path,
        ["001,Alcaldía,ACME SAS\n"],
        encabezado="numero_contrato,entidad,contratista\n",
    )

    with pytest.raises(ValueError) as error:
        cargar_contratos(ruta)

    # El error debe nombrar qué falta, no solo decir "error"
    assert "funcionario" in str(error.value)


def test_falla_si_el_archivo_esta_vacio(tmp_path):
    ruta = tmp_path / "vacio.csv"
    ruta.write_text("", encoding="utf-8")

    with pytest.raises(ValueError):
        cargar_contratos(str(ruta))


# --- Problema duro: idempotencia -----------------------------------


def test_ignora_contratos_duplicados(tmp_path):
    """La misma clave natural repetida se carga una sola vez."""
    fila = "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
    ruta = escribir_csv(tmp_path, [fila, fila, fila])

    contratos = cargar_contratos(ruta)

    assert len(contratos) == 1


def test_duplicado_no_genera_alerta_falsa(tmp_path):
    """
    El caso que justifica el problema duro: un contrato duplicado NO debe
    verse como un contratista reincidiendo con un funcionario.
    """
    fila = "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
    ruta = escribir_csv(tmp_path, [fila, fila])

    alertas = detectar_reincidencias(cargar_contratos(ruta))

    assert alertas == []


def test_gana_el_primero_ante_datos_distintos(tmp_path):
    """
    Misma clave, montos distintos: se conserva la primera fila.
    Regla documentada en docs/problema-duro.md, §4.
    """
    ruta = escribir_csv(
        tmp_path,
        [
            "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n",
            "001,Alcaldía,ACME SAS,Juan Pérez,999,2026-01-01\n",
        ],
    )

    contratos = cargar_contratos(ruta)

    assert len(contratos) == 1
    assert contratos[0]["monto"] == "100"


def test_mismo_numero_en_entidades_distintas_no_se_deduplica(tmp_path):
    """
    La clave incluye la entidad: dos entidades pueden usar el consecutivo
    "001" y son contratos diferentes. Regla R-2.
    """
    ruta = escribir_csv(
        tmp_path,
        [
            "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n",
            "001,Gobernación,ACME SAS,Juan Pérez,200,2026-02-01\n",
        ],
    )

    contratos = cargar_contratos(ruta)

    assert len(contratos) == 2
    # Y sí son una reincidencia real: mismo par, dos contratos distintos
    assert len(detectar_reincidencias(contratos)) == 1
